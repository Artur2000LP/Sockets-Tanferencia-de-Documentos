
package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Server extends Thread {

  private final String IP_ADDRESS;
  private final int PORT;
  private final int MAX_CLIENTS;

  private final Map<String, Socket> fileSockets;
  private final Map<String, Socket> chatSockets;
  private final List<Message> messages;

  public Server(String ip, int port, int maxClients) {
    this.IP_ADDRESS = ip;
    this.PORT = port;
    this.MAX_CLIENTS = maxClients;
    this.fileSockets = new LinkedHashMap<String, Socket>();
    this.chatSockets = new LinkedHashMap<String, Socket>();
    this.messages = new ArrayList<>();
  }

  @Override
  public void run() {
    try {
      InetAddress ipAddress = InetAddress.getByName(IP_ADDRESS);
      InetSocketAddress socketAddress = new InetSocketAddress(ipAddress, PORT);
      ServerSocket server = new ServerSocket();

      System.out.println("Servidor activo en: " + IP_ADDRESS + ":" + PORT);

      server.bind(socketAddress);

      int nClients = 0;
      while (nClients < MAX_CLIENTS) {
        Socket newConnection = server.accept();
        Thread client = new Thread(new HandlerClient(newConnection));
        client.start();
        ++nClients;
      }

      server.close();

    } catch (Exception e) {
    }

  }

  private class HandlerClient implements Runnable {
    private final Socket connectionSocket;

    public HandlerClient(Socket client) {
      connectionSocket = client;
    }

    @Override
    public void run() {
      try {
        // lectura de datos de protocolo
        DataInputStream inputStream = new DataInputStream(connectionSocket.getInputStream());

        String user = inputStream.readUTF(); // pepito, pepa, pepon, pepino
        String service = inputStream.readUTF();// chat-service , file-transfer-service

        switch (service) {
          case "chat-service":
            activeChatService(user);
            break;
          case "file-transfer-service":
            activeFileTransferService(user);
            break;
          default:
            break;
        }

      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    private void activeChatService(String user) {
      chatSockets.put(user, connectionSocket);
      try {
        DataInputStream inputStream = new DataInputStream(connectionSocket.getInputStream());

        while (true) {
          String action = inputStream.readUTF();
          if (action == "end-service")
            break;

          // leemos informacion importante del mensaje del usuario
          String receiver = inputStream.readUTF();
          String message = inputStream.readUTF();

          String status = "0 %";

          if (chatSockets.containsKey(receiver)) {
            DataOutputStream outputStreamReceiver = new DataOutputStream(chatSockets.get(receiver).getOutputStream());
            outputStreamReceiver.writeUTF(user);
            outputStreamReceiver.writeUTF(message);
            status = "100 %";
          }

          messages.add(new Message(user, receiver, message, status));
        }

        chatSockets.remove(user);
        connectionSocket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }

    }

    private void activeFileTransferService(String user) {
      try {
        fileSockets.put(user, connectionSocket);
        DataInputStream inputStream = new DataInputStream(connectionSocket.getInputStream());

        while (true) {
          String action = inputStream.readUTF();
          if (action == "end-service")
            break;
          // leemos informacion importante del usuario
          String filename = inputStream.readUTF();
          long filesize = inputStream.readLong();
          String receiver = inputStream.readUTF();

          String status = "0%";

          if (fileSockets.containsKey(receiver)) {
            DataOutputStream outputStreamReceiver = new DataOutputStream(fileSockets.get(receiver).getOutputStream());

            byte[] buffer = new byte[1024];
            int read = 0;
            long  totalReceived = 0;

            outputStreamReceiver.writeUTF(user);
            outputStreamReceiver.writeUTF(filename);
            outputStreamReceiver.writeLong(filesize);
            while(totalReceived < filesize && (read = inputStream.read(buffer)) != -1) {
              outputStreamReceiver.write(buffer, 0, read);
              totalReceived += read;
              double percentage = ((double) totalReceived / filesize) * 100;
              status = "" + percentage + " %";
            }

          }

          messages.add(new Message(user, receiver, "fichero: " + filename, status));

        }

        fileSockets.remove(user);
        connectionSocket.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }

}
