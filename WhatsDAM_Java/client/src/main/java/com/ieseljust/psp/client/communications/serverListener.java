package com.ieseljust.psp.client.communications;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import com.ieseljust.psp.client.CurrentConfig;
import com.ieseljust.psp.client.Message;
import com.ieseljust.psp.client.ViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class serverListener implements Runnable {

  ViewModel vm;

  public serverListener(ViewModel vm) {
    this.vm = vm;
  }

  @Override
  public void run() {
    ServerSocket listener = null;

    try {
      listener = new ServerSocket(0);
      CurrentConfig.setlistenPort(listener.getLocalPort());

    } catch (IOException e) {
      System.out.println("El port està ocupat o és inaccessible.");
      return;
    }

    while (true) {
      try {
        Socket socket = listener.accept();
        handleIncomingMessage(socket);
        socket.close();
      } catch (IOException e) {
        System.err.println("Error en acceptar la connexió: " + e.getMessage());
      }
    }
  }

  private void handleIncomingMessage(Socket socket) {
    try {
      InputStream is = socket.getInputStream();
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      String line = br.readLine();

      if (line != null) {
        processReceivedMessage(line);
      }
    } catch (IOException e) {
      System.err.println("Error al llegir les dades del servidor: " + e.getMessage());
    }
  }

  private void processReceivedMessage(String message) {
    try {
      JSONObject jsonMessage = new JSONObject(message);
      String messageType = jsonMessage.getString("type");

      switch (messageType) {
        case "userlist":
          processUserListMessage(jsonMessage);
          break;
        case "message":
          processMessage(jsonMessage);
          break;
        default:
          System.out.println("Tipus de missatge desconegut: " + messageType);
      }
    } catch (JSONException e) {
      System.err.println("Error en processar el missatge JSON: " + e.getMessage());
    }
  }

  private void processUserListMessage(JSONObject jsonMessage) {
    try {
      JSONArray jsonArray = jsonMessage.getJSONArray("content");
      ArrayList<String> userList = new ArrayList<>();

      for (int i = 0; i < jsonArray.length(); i++) {
        userList.add(jsonArray.getString(i));
      }

      vm.updateUserList(userList);
    } catch (JSONException e) {
      System.err.println("Error en processar el missatge de llista d'usuaris: " + e.getMessage());
    }
  }

  private void processMessage(JSONObject jsonMessage) {
    try {
      String user = jsonMessage.getString("user");
      String content = jsonMessage.getString("content");

      vm.addMessage(new Message(user, content));
    } catch (JSONException e) {
      System.err.println("Error en processar el missatge de l'usuari: " + e.getMessage());
    }
  }
}
