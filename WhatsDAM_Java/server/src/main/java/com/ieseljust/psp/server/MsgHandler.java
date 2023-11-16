package com.ieseljust.psp.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.ArrayList;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.json.JSONObject;

class MsgHandler implements Runnable {
  // Classe per atendre les peticions a través de threads

  private Socket MySocket; // Socket que atendrà la petició
  private ArrayList<Connexio> Connexions; // Vector de connexions del servidor

  MsgHandler(Socket socket, ArrayList<Connexio> connexions) {
    // Constructor de la classe.
    // S'inicia amb un socket i la llista de connexions.
    // Aquesta llista de connexions només podrà ser modificada
    // per un fil en la seua secció crítica

    MySocket = socket;
    Connexions = connexions;
  }

  JSONObject sendMessage(JSONObject MissatgeRebut) {
    // Rep un missatge en format JSON i l'envia a través
    // de la classe Notifier a tots els usuaris connectats
    // fent ús del mètode broadCastMessage.
    // Retornarà un JSONObject amb la resposta que ens
    // retorne aquest mètode.

    JSONObject resposta = new JSONObject();

    try {
      Notifier.broadCastMessage(MissatgeRebut);
      resposta.put("status", "ok");

    } catch (Exception e) {
      resposta.put("error", e.getMessage());
    }
    return resposta;

  }

  JSONObject registerUser(JSONObject MissatgeRebut) {
    // Mètode per registrar l'usuari

    JSONObject resposta = new JSONObject();

    // Recorre totes les connexions existents, i comprova si existeix
    // un usuari amb el mateix nom.

    for (Connexio connexio : Connexions) {
      System.out.println(connexio.toString());
      if (MissatgeRebut.getString("user").equals(connexio.getUser())) {
        // Si hi ha un usuari amb el mateix nom, retorna un missatge d'error
        resposta.put("status", "error");
        resposta.put("message", "User " + connexio.getUser() + " already registered");
        return resposta;
      }
    }

    // En cas que no existisca, crea una nova connexió amb l'usuari
    synchronized (Connexions) {
      Connexio con = new Connexio(MissatgeRebut.getString("user"), MySocket, MissatgeRebut.getInt("listenPort"));
      Connexions.add(con);
    }

    resposta.put("status", "ok");

    // I com que s'ha fet una actualizació de l'estat al
    // servidor, enviem un broadcast a tots els clients
    // amb la llista d'usuaris (a través del mètode broadCastUsers)
    Notifier.broadCastUsers();

    return resposta;
  }

  @Override
  public void run() {
    try {
      // Aquest mètode s'encarregarà d'atendre cada petició i generar la resposta
      // adequada.

      // 1. Llegirem les línies a través de l'InputStream del socket amb què s'ha
      // obert la connexió
      // (només se'ns passarà una línia per petició)
      InputStream is = MySocket.getInputStream();
      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader br = new BufferedReader(isr);

      String linia = br.readLine();

      // 2. Una vegada tinguem la línia llegida, caldrà convertir-la a objecte JSON
      // amb:
      // JSONObject MissatgeRebut = new JSONObject(linia);
      JSONObject MissatgeRebut = new JSONObject(linia);

      // 3. Obtenim l'ordre (camp "command") del JSON per tal d'obtenir què ens demana
      // el client.
      String command = MissatgeRebut.getString("command");

      // 4. Implementem un mètode per atendre cada tipus de missatge.
      // Consell: Implementar un mètode per atendre cada tipus de missatge,
      // en lloc de fer-ho tot al switch.

      switch (command) {
        case "register":
          // Registra l'usuari al servidor, afegint-lo a la llista de connexions.
          // Tingueu en compte que ja disposeu d'un mètode RegisterUser en aquesta
          // classe que implementa aquesta funcionalitat.
          JSONObject registerResponse = registerUser(MissatgeRebut);
          sendResponse(registerResponse);
          break;

        case "newMessage":
          // Es rep un missatge per enviar a la resta de clients.
          // Recordeu que també disposeu d'un mètode sendMessage que envia un missatge
          // a tots els clients.
          JSONObject sendMessageResponse = sendMessage(MissatgeRebut);
          sendResponse(sendMessageResponse);
          break;

        default:
          // En cas que l'ordre no siga vàlida, envia una resposta d'error.
          JSONObject errorResponse = new JSONObject();
          errorResponse.put("status", "error");
          errorResponse.put("error", "Invalid command");
          sendResponse(errorResponse);
          break;
      }

    } catch (Exception e) {
      System.out.println("Error: " + e.getMessage());
    } finally {
      try {
        // Tanquem el socket quan hem acabat de gestionar la petició.
        MySocket.close();
      } catch (IOException e) {
        System.err.println("Error en tancar el socket: " + e.getMessage());
      }
    }
  }

  private void sendResponse(JSONObject response) {
    // Envia la resposta JSON al client a través del socket.
    try {
      OutputStream os = MySocket.getOutputStream();
      PrintWriter out = new PrintWriter(os, true);
      out.println(response.toString());
    } catch (IOException e) {
      System.err.println("Error en enviar la resposta: " + e.getMessage());
    }
  }

}
