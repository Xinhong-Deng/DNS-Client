import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

public class DNSClient {
  enum Type {A, MX, NS};

  int timeOutMs = 5000;
  int maxRetries = 3;
  int port = 53;
  Type type = Type.A;
  byte[] server;
  String domainName;
  
  public void main(String[] args) {
    //parseUserInput(args);
  }

  private void parseUserInput(String[] userInput) throws Exception {

    for (int i = 2; i < userInput.length; i++) {
      if (userInput[i].equals("-t")) {
        i++;
        timeOutMs = Integer.parseInt(userInput[i])* 1000;
        continue;
      }

      if (userInput[i].equals("-r")) {

        continue;
      }

      if (userInput[i].equals("-p")) {
        continue;
      }

      if (userInput[i].equals("-mx")) {
        continue;
      }

      if (userInput[i].equals("-ns")) {
        continue;
      }

      throw new Exception("Unknown options");
      //should describe the error!! redesign it!!!

    }

  }
}
