package com.ieseljust.psp.client.communications;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.ieseljust.psp.client.CurrentConfig;
import com.ieseljust.psp.client.Message;

import org.json.JSONException;
import org.json.JSONObject;

public class communicationManager {

  /* Aquesta classe s'encarrega de la gestió de la comunicació amb el servidor. */

  public static JSONObject sendServer(String msg) throws communicationManagerException {
    // Envía al servidor l'string msg i retorna un JSON amb la resposta

    try {
      // Inicialitza el socket i es connecta al servidor
      Socket socket = new Socket(CurrentConfig.server(), 9999);
      // socket.connect(new InetSocketAddress(CurrentConfig.server(),
      // CurrentConfig.port()), 5000);

      // Obtenir l'OutputStream del socket
      OutputStream os = socket.getOutputStream();
      PrintWriter out = new PrintWriter(new OutputStreamWriter(os));

      // Enviar el missatge al servidor
      out.println(msg);
      out.flush();

      // Llegir la resposta del servidor
      InputStream is = socket.getInputStream();
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      String response = br.readLine();

      // Convertir la resposta a un objecte JSON i tancar el socket
      JSONObject jsonResponse = new JSONObject(response);
      socket.close();

      return jsonResponse;

    } catch (Exception e) {
      throw new communicationManagerException("Error en enviar al servidor: " + e.getMessage());
    }
  }

  public static void connect() throws JSONException, communicationManagerException {
    // Creem un missatge pel servidor amb l'ordre (command) register,
    // el nom d'usuari (user) i el port (listenPost) pel qual el client escoltarà
    // les notificacions (el tenim a CurrentConfig.listenPort())

    try {
      JSONObject registerMessage = new JSONObject();
      registerMessage.put("command", "register");
      registerMessage.put("user", CurrentConfig.username());
      registerMessage.put("listenPort", CurrentConfig.listenPort());

      // Envia el missatge al servidor
      JSONObject response = sendServer(registerMessage.toString());

      // Comprova la resposta del servidor
      if (response.has("status") && response.getString("status").equals("error")) {
        throw new communicationManagerException("Error en connectar al servidor: " + response.getString("error"));
      }

    } catch (Exception e) {
      throw new communicationManagerException("Error en connectar al servidor: " + e.getMessage());
    }
  }

  public static void sendMessage(Message m) {
    // Envia un missatge al servidor (es fa amb una línia!)
    try {
      // Usando el método toJSON() de la clase Message
      sendServer(m.toJSON().toString());

      // O usando el método toJSONCommand() de la clase Message si necesitas incluir
      // el campo "command"
      // sendServer(m.toJSONCommand().toString());

    } catch (communicationManagerException e) {
      // Manejar la excepción aquí según tus necesidades
      e.printStackTrace(); // O imprime un mensaje de error, lógica de recuperación, etc.
    }
  }
}
