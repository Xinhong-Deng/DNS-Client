import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DNSClient {
    enum Type {A, MX, NS}

    static int timeOutMs = 5000;
    static int maxRetries = 3;
    static int port = 53;
    static Type type = Type.A;
    static ByteBuffer server;
    static String domainName;

    public static void main(String[] args) {
        try {
            parseUserInput(args);
            testParser();
            //DatagramSocket socket = new DatagramSocket();
        } catch (Exception e) {
            //wrong handle!!!
            e.printStackTrace();
        }
    }

    private static void parseUserInput(String[] userInput) throws Exception {
        //todo: how to find out incorrect user input in a systematic way?

        for (int i = 0; i < userInput.length; i++) {
            System.out.println(i + "th input: " + userInput[i]);
            if (userInput[i].equals("-t")) {
                i++;
                timeOutMs = Integer.parseInt(userInput[i]) * 1000;
                continue;
            }

            if (userInput[i].equals("-r")) {
                i++;
                maxRetries = Integer.parseInt(userInput[i]);
                continue;
            }

            if (userInput[i].equals("-p")) {
                i++;
                port = Integer.parseInt(userInput[i]);
                continue;
            }

            if (userInput[i].equals("-mx")) {
                if (type != Type.A) {
                    throw new Exception("cannot set to -ns and -mx at the same time");
                }

                type = Type.MX;
                continue;
            }

            if (userInput[i].equals("-ns")) {
                if (type != Type.A) {
                    throw new Exception("cannot set to -ns and -mx at the same time");
                }

                type = Type.NS;
                continue;
            }

            if (userInput[i].charAt(0) == '@') {
                System.out.println("enter the server ip");
                String[] temp = userInput[i].split("@");
                String[] labels = (temp[1]).split("\\.");

                ArrayList<Integer> intLabels = new ArrayList<>();
                for (int y = 0; y < labels.length; y++) {
                    intLabels.add(Integer.parseInt(labels[y]));
                }
                server = ByteBuffer.allocate(4);
                intLabels.forEach(intLable -> {
                    server.put(intLable.byteValue());
                    //todo: double check
                });
                continue;
            }

            if (i == userInput.length - 1) {
                //todo: check that whether this satisfy the domain name format
                // before assigning it to domainName!!!
                domainName = userInput[i];
                continue;
            }

            throw new Exception("Unknown options");
            //todo: should describe the error!! redesign it!!!
            //todo: should be able to figure out some are missing (here or in main??)

        }

    }

    static private void testParser() {
        System.out.println("-t: " + timeOutMs);
        System.out.println("-r: " + maxRetries);
        System.out.println("-p: " + port);
        System.out.println("type: " + type);
        System.out.println("server: " + server.toString());
        System.out.println("hostname: " + domainName);
    }
}
