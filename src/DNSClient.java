import javafx.util.Pair;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DNSClient {
    public enum Type {A, MX, NS}

    private static int timeOutMs = 5000;
    private static int maxRetries = 3;
    private static int port = 53;
    private static Type type = Type.A;
    private static ByteBuffer server;
    private static String domainName;

    private static int retryCount = 0;

    public static void main(String[] args) {
        try {
            parseUserInput(args);
            InetAddress addr = InetAddress.getByAddress(server.array());
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(timeOutMs);
            DNSMessage dnsMessage = new DNSMessage(type, domainName);
            DatagramPacket queryPacket = dnsMessage.buildQueryPacket(addr, port);

            int lengthResponseBytes = 1024;
            byte[] responseBytes = new byte[lengthResponseBytes];
            DatagramPacket responsePacket = new DatagramPacket(responseBytes, lengthResponseBytes);
            socket.send(queryPacket);

            tryReceiveResponse(socket, responsePacket);
            String interpredResult = dnsMessage.interpretResponsePacket(responsePacket);
            System.out.println(interpredResult);

        } catch (UserInputException e) {
            System.out.println("ERROR\t" + "Incorrect input syntax: " + e.getMessage());
        } catch (UnknownHostException e) {
            System.out.println("ERROR\t" + "IP address provided is unknown: " + Arrays.toString(server.array()));
        } catch (SocketException e) {
            System.out.println("ERROR\t" + "Cannot open socket");
        } catch (SocketTimeoutException e) {
            System.out.println("ERROR\t" + e.getMessage());
        } catch (IOException e) {
            System.out.println("ERROR\t" + "I/O exception at send");
        } catch (ResponseException e) {
            System.out.println(e.getErrorMessage());
        }
    }

    private static void tryReceiveResponse(DatagramSocket socket, DatagramPacket responsePacket) throws SocketTimeoutException {
        //todo: double check this
        try {
            socket.receive(responsePacket);
        } catch (SocketTimeoutException e) {
            if (retryCount > maxRetries) {
                throw new SocketTimeoutException("Exceed max retry limit.");
            }
            retryCount++;
            tryReceiveResponse(socket, responsePacket);

        } catch (IOException e) {
            System.out.println("ERROR\t" + "I/O exception at send");
        }
        retryCount = 0;
    }

    private static void parseUserInput(String[] userInput) throws UserInputException {

        for (int i = 0; i < userInput.length; i++) {

            if (userInput[i].equals("-t")) {
                i++;
                try {
                    timeOutMs = Integer.parseInt(userInput[i]) * 1000;
                } catch (NumberFormatException numberE) {
                    throw new UserInputException("Not provide integer value for -t");
                }

                continue;
            }

            if (userInput[i].equals("-r")) {
                i++;
                try {
                    maxRetries = Integer.parseInt(userInput[i]);
                } catch (NumberFormatException e) {
                    throw new UserInputException("Not provide integer value for -r");
                }

                continue;
            }

            if (userInput[i].equals("-p")) {
                i++;
                try {
                    port = Integer.parseInt(userInput[i]);
                } catch (NumberFormatException e) {
                    throw new UserInputException("Not provide integer value for -p");
                }

                continue;
            }

            if (userInput[i].equals("-mx")) {
                if (type != Type.A) {
                    throw new UserInputException("Cannot set to -ns and -mx at the same time");
                }

                type = Type.MX;
                continue;
            }

            if (userInput[i].equals("-ns")) {
                if (type != Type.A) {
                    throw new UserInputException("Cannot set to -ns and -mx at the same time");
                }

                type = Type.NS;
                continue;
            }

            if (userInput[i].charAt(0) == '@') {
                String[] temp = userInput[i].split("@");
                String[] labels = (temp[1]).split("\\.");

                if (labels.length != 4) {
                    throw new UserInputException("Invalid ip address");
                }

                ArrayList<Integer> intLabels = new ArrayList<>();
                for (String label : labels) {
                    Integer intValue = Integer.parseInt(label);
                    if (intValue >= 256) {
                        throw new UserInputException("Invalid ip address");
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
                    throw new UserInputException("Invalid name");
                }

                domainName = userInput[i];
                continue;
            }

            throw new UserInputException("Unknown options");
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
            throw new UserInputException("Server IP address is not provided");
        }

        if (domainName == null) {
            throw new UserInputException("Name is not provided");
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
