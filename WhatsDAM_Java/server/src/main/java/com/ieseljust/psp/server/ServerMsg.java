package com.ieseljust.psp.server;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class ServerMsg {
  /*
   * Classe servidor de missatges: s'encarregarà d'atendre les peticions dels
   * clients.
   */

  // Guardem una llista de les connexions actives
  private static ArrayList<Connexio> Connexions;

  public static void init() {
    // init fa de constructor de la classe: Crea l'arrayList de connexions
    Connexions = new ArrayList<Connexio>();
  }

  public static void listen(int srvPort) throws IOException {
    // Mètode per escoltar i atendre les peticions

    // 1. Crear un socket de tipus servidor que escolte pel port srvPort
    ServerSocket serverSocket = new ServerSocket(srvPort);
    System.out.println("Servidor escoltant al port " + srvPort);

    // 2. Iniciem un bucle infinit a l'espera de rebre connexions
    // Quan arribe una connexió, haurem de crear un thread per atendre la petició
    // La classe que s'encarregarà d'atendre les peticions és MsgHandler
    while (true) {
      try {
        // Espera fins que una connexió arriba
        Socket clientSocket = serverSocket.accept();

        // Crea un nou thread per atendre la petició del client
        MsgHandler handler = new MsgHandler(clientSocket, Connexions);
        Thread handlerThread = new Thread(handler);
        handlerThread.start();
      } catch (IOException e) {
        System.err.println("Error en acceptar la connexió: " + e.getMessage());
      }
    }
  }

  public static void main(String[] args) throws IOException {
    // Mètode principal de l'aplicació.
    // Crea un objecte de tipus ServerMsg
    // i invoca al mètode listen, per tal d'escoltar peticions.

    int srvPort = 9999;

    // Inicialitzem el servidor de missatges
    // (Seria com el constructor si no fora estàtica)
    ServerMsg.init();

    // Associem les connexions a les connexions
    // del Notificador.
    Notifier.setConnexions(Connexions);

    // Abans de llançar el servidor,
    // llancem un fil, que cada segon
    // envie un "ping" als clients, pe veure
    // si estan encara connectats. En cas
    // contrari, els elimina de la llista

    // Ho fem directament, sense una classe runnable,
    // a través d'una subclasse anònima:

    Thread thread = new Thread() {
      public void run() {
        while (true) {
          try {
            Thread.sleep(1000);
            // Enviem el ping
            Notifier.pingClients();
          } catch (InterruptedException ie) {
            System.err.println(ie.getMessage());
          }
        }
      }
    };
    thread.start();

    // Llancem el servidor per a que escolte
    // i atenga les peticions
    ServerMsg.listen(srvPort);

  }
}
