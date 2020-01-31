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
            System.out.println(e.getMessage());
        }
    }

    private static void parseUserInput(String[] userInput) throws Exception {
        //todo: probably want a new exception tyep?

        for (int i = 0; i < userInput.length; i++) {

            if (userInput[i].equals("-t")) {
                i++;
                timeOutMs = Integer.parseInt(userInput[i]) * 1000;
                //todo: cannot construct the error message from the NumberFormatException in the main()!!
                continue;
            }

            if (userInput[i].equals("-r")) {
                i++;
                maxRetries = Integer.parseInt(userInput[i]);
                //todo: cannot construct the error message from the NumberFormatException in the main()!!
                continue;
            }

            if (userInput[i].equals("-p")) {
                i++;
                port = Integer.parseInt(userInput[i]);
                //todo: cannot construct the error message from the NumberFormatException in the main()!!
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

                if (labels.length != 4) {
                    throw  new Exception("invalid ip address");
                }

                ArrayList<Integer> intLabels = new ArrayList<>();
                for (int y = 0; y < labels.length; y++) {
                    Integer intValue = Integer.parseInt(labels[y]);
                    if (intValue >= 128) {
                        throw new Exception("invalid ip address");
                    }

                    intLabels.add(intValue);
                }
                server = ByteBuffer.allocate(4);
                intLabels.forEach(intLable -> {
                    server.put(intLable.byteValue());
                });
                continue;
            }

            if (i == userInput.length - 1) {

                List<String> labels = Arrays.asList(userInput[i].split("\\."));
                boolean isInvalidName = labels.stream().anyMatch(n -> n.length() == 0)
                        || labels.size() == 0;
                if (isInvalidName) {
                    throw new Exception("incorrect userinput");
                }

                domainName = userInput[i];
                continue;
            }

            throw new Exception("Unknown options");
            //todo: should describe the error!! redesign it!!!
            //todo: should be able to figure out some are missing (here or in main??)

            //todo: potnetial error
            // 1. ip address does not have @ (don't know how to deal with it)
            // 2. name not provided
            // 3. ip not provided
            // 4. unknown option
            // NOT HANDLE name missing and (ip without @) case!!!
        }

        if (server == null) {
            throw new Exception("server is not provided");
        }

        if (domainName == null) {
            throw  new Exception("name is not provided");
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
