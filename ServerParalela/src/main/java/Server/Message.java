package Server;

public class Message {
  public String sender;
  public String receiver;
  public String message;
  public String status;

  public Message(String sender, String receiver, String message, String status){
    this.message = message;
    this.sender = sender;
    this.receiver = receiver;
    this.status = status;
  }
}
