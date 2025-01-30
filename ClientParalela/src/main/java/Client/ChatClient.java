package Client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ChatClient extends Thread {
  private Socket chatSocket;
  private Socket fileSocket;

  private final String IP_SERVER;
  private final String FOLDER_USER_PATH;
  public final String USER;
  private final int PORT;

  OnMessageFunction onMessageFunction = (String sender, String message) -> {
  };
  OnReceiveFileData onReceiveFileData = (String sender, String filename, long filesize) -> {
  };
  OnReceiveFileBytes onReceiveFileBytes = (double percentage) -> {
  };
  OnSendFileBytes onSendFileBytes = (double percentage) -> {
  };
  OnSendFiledata onSendFiledata = (String receiver, String filename, long filesize) -> {
  };
  OnEndSendFile onEndSendFile = () -> {
  };
  OnEndReceiveFile onEndReceiveFile = () -> {
  };

  public ChatClient(String ip, String folderPath, int port, String user) {
    this.IP_SERVER = ip;
    this.FOLDER_USER_PATH = folderPath;
    this.PORT = port;
    this.USER = user;
  }

  public void initChatService() {
    try {
      chatSocket = new Socket(InetAddress.getByName(IP_SERVER), PORT);
      DataOutputStream outputStream = new DataOutputStream(chatSocket.getOutputStream());
      outputStream.writeUTF(USER);
      outputStream.writeUTF("chat-service");

      DataInputStream inputStream = new DataInputStream(chatSocket.getInputStream());
      while (true) {
        String sender = inputStream.readUTF();
        String message = inputStream.readUTF();
        onMessageFunction.execute(sender, message);
      }

    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void initFileTransferService() {
    try {
      fileSocket = new Socket(InetAddress.getByName(IP_SERVER), PORT);
      DataOutputStream outputStream = new DataOutputStream(fileSocket.getOutputStream());
      outputStream.writeUTF(USER);
      outputStream.writeUTF("file-transfer-service");

      while (true) {
        DataInputStream inputStream = new DataInputStream(fileSocket.getInputStream());
        String sender = inputStream.readUTF();
        String filename = inputStream.readUTF();
        long filesize = inputStream.readLong();
        onReceiveFileData.execute(sender, filename, filesize);

        String filePath = System.getProperty("user.home") + FOLDER_USER_PATH + "/" + sender + "_to_" + USER + "-"
            + filename;
        FileOutputStream outputFile = new FileOutputStream(filePath);

        byte[] buffer = new byte[1024];
        int read = 0;
        long totalReceived = 0;

        while (totalReceived < filesize && (read = inputStream.read(buffer)) != -1) {
          outputFile.write(buffer, 0, read);
          totalReceived += read;
          double percentage = ((double) totalReceived / filesize) * 100;
          onReceiveFileBytes.execute(percentage);
        }

        outputFile.close();
      }

    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void sendMessage(String receiver, String message) {
    try {
      DataOutputStream outputStream = new DataOutputStream(chatSocket.getOutputStream());
      outputStream.writeUTF("send");
      outputStream.writeUTF(receiver);
      outputStream.writeUTF(message);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void sendFile(String receiver, String filePath) {

    Thread thread = new Thread(() -> {
      try {
        DataOutputStream outputStream = new DataOutputStream(fileSocket.getOutputStream());
        FileInputStream inputFile = new FileInputStream(filePath);

        File file = new File(filePath);
        String filename = file.getName();
        long filesize = file.length();

        outputStream.writeUTF("send");
        outputStream.writeUTF(filename);
        outputStream.writeLong(filesize);
        outputStream.writeUTF(receiver);
        onSendFiledata.execute(receiver, filename, filesize);

        byte[] buffer = new byte[1024];
        int read = 0;
        long totalSent = 0;

        while ((read = inputFile.read(buffer)) != -1) {
          outputStream.write(buffer, 0, read);
          totalSent += read;
          double percentage = ((double) totalSent / filesize) * 100;
          onSendFileBytes.execute(percentage);
        }
        inputFile.close();
        onEndSendFile.execute();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    thread.start();
  }

  @Override
  public void run() {
    Thread chatService = new Thread(() -> initChatService());
    Thread fileTransferService = new Thread(() -> initFileTransferService());
    chatService.start();
    fileTransferService.start();
  }

  public void closeService() {
    try {
      DataOutputStream outputChatStream = new DataOutputStream(chatSocket.getOutputStream());
      DataOutputStream outputFileStream = new DataOutputStream(fileSocket.getOutputStream());
      outputChatStream.writeUTF("end-service");
      outputFileStream.writeUTF("end-service");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void setOnMessageFunction(OnMessageFunction callback) {
    this.onMessageFunction = callback;
  }

  public void setOnReceiverFileData(OnReceiveFileData callback) {
    this.onReceiveFileData = callback;
  }

  public void setOnReceiverFileBytes(OnReceiveFileBytes callback) {
    this.onReceiveFileBytes = callback;
  }

  public void setOnSendFileData(OnSendFiledata callback) {
    this.onSendFiledata = callback;
  }

  public void setOnSendFileBytes(OnSendFileBytes callback) {
    this.onSendFileBytes = callback;
  }

  public void setOnEndSendFile(OnEndSendFile callback) {
    this.onEndSendFile = callback;
  }

  public void setOnEndReceiveFile(OnEndReceiveFile callback) {
    this.onEndReceiveFile = callback;
  }

  @FunctionalInterface
  public interface OnMessageFunction {
    void execute(String sender, String message);
  }

  @FunctionalInterface
  public interface OnReceiveFileData {
    void execute(String sender, String filename, long filesize);
  }

  @FunctionalInterface
  public interface OnReceiveFileBytes {
    void execute(double percentage);
  }

  @FunctionalInterface
  public interface OnSendFiledata {
    void execute(String receiver, String filename, long filesize);
  }

  @FunctionalInterface
  public interface OnSendFileBytes {
    void execute(double percentage);
  }

  @FunctionalInterface
  public interface OnEndSendFile {
    void execute();
  }

  @FunctionalInterface
  public interface OnEndReceiveFile {
    void execute();
  }

}
