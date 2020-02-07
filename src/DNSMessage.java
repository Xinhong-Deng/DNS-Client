import javafx.util.Pair;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DNSMessage {
    ArrayList<Byte> header;
    ArrayList<Byte> question;
    List<byte[]> answer;
    int id;
    DNSClient.Type type;
    String domainName;

    DNSMessage(DNSClient.Type type, String domainName) {
        this.type = type;
        this.domainName = domainName;
        header = new ArrayList<>();
        question = new ArrayList<>();
    }

    DatagramPacket buildQueryPacket(InetAddress ip, int port) {
        // todo: length of query is 12!!! wrong!!
        Random random = new Random();
        id = random.nextInt(65535);
        header.add((byte) id);//stores 8-15 bits of the id
        header.add((byte) (id >> 8));//stores 0-7 bits of the id
        header.add((byte) 0x01);//QR - RD
        header.add((byte) 0);
        header.add((byte) 0);//QDCount will always be 1
        header.add((byte) 1);
        header.add((byte) 0);
        header.add((byte) 0);
        header.add((byte) 0);
        header.add((byte) 0);
        header.add((byte) 0);
        header.add((byte) 0);

        int qType = getQType();
        addNameToQuestion();
        question.add((byte) 0x0);
        question.add((byte) qType);
        question.add((byte) 0x0);
        question.add((byte) 0x1);

        ArrayList<Byte> packetArrayList = new ArrayList<>(header);
        packetArrayList.addAll(question);
        byte[] packetArray = new byte[packetArrayList.size()];
        for (int i = 0; i < packetArrayList.size(); i++) {
            packetArray[i] = packetArrayList.get(i);
        }
        // TODO: ignore the other part or put 0s in them
        return new DatagramPacket(packetArray, packetArray.length, ip, port);
    }

    String interpretResponsePacket(DatagramPacket packet) throws ResponseException {
        ByteBuffer responseB = ByteBuffer.allocate(packet.getData().length).put(packet.getData());

        //todo: check it???
        responseB.rewind();
        byte b0 = responseB.get();
        byte b1 = responseB.get();
        int ID = (b0 | (b1 << 8));
        if (ID != id) {
            throw new ResponseException("The response id does not match the query id.");
        }

        short headerL2 = responseB.getShort();
        int AA = (headerL2 & 0b0000010000000000) >>> 10;

        int RA = (headerL2 & 0b0000000010000000) >>> 7;
        if (RA == 0) {
            //TODO: check if RA is 0 indicates that not support recursion
            throw new ResponseException("Error: Recursive queries are not suppported.");
        }

        int RCODE = (headerL2 & 0b0000000000001111);   //RCODE at bit 4-7
        if (RCODE == 1) {
            throw new ResponseException("Format error: the name server was unable to interpret the query.");
        } else if (RCODE == 2) {
            throw new ResponseException("Server Failure: the name server was unable to process this qeury due to a problem with the name server.");
        } else if (RCODE == 3) {
            throw new ResponseException("Name error: meaningful only for responses from an authoritative name server, this code signifies that the domain name referenced in the query does not exist");
        } else if (RCODE == 4) {
            throw new ResponseException("Not implemented: the name server does not support the requested kind of query");
        } else if (RCODE == 5) {
            throw new ResponseException("Refused: the name server refuses to perform the requested operation for policy reasons");
        }

        short QDCOUNT = responseB.getShort();
        short ANCOUNT = responseB.getShort();
        short NSCOUNT = responseB.getShort();
        short ARCOUNT = responseB.getShort();
        //TODO: parse until the data set.

//        Pair<ArrayList<String>, Integer> temp = getName(response, header.size() + question.size());
//        ArrayList<String> names = temp.getKey();
//        int currentIndex = temp.getValue();
//
//        short answerType =  response[currentIndex];



        return null;
    }

    private int getQType() {
        if (this.type == DNSClient.Type.A) {
            return 0x01;
        }
        if (this.type == DNSClient.Type.NS) {
            return 0x02;
        }
        if (this.type == DNSClient.Type.MX) {
            return 0x0f;
        }

        return 0;
    }

    private void addNameToQuestion() {
        String[] fragments = domainName.split("\\.");
        for (String fragment : fragments) {
            int length = fragment.length();
            question.add((byte) length);
            byte[] qName = fragment.getBytes();
            for (byte b : qName) {
                question.add(b);
            }
        }
        question.add((byte) 0x0);
    }

    private Pair<ArrayList<String>, Integer> getName(byte[] response, int index) {
        // todo: possible for a pointer points to a pointer!!!
        // todo: need to change the byte[] to bytebuffer for easier oepration...
        ArrayList<String> names = new ArrayList<String>();
        Integer i = index;//i is the current index
        Pair<ArrayList<String>, Integer> result;
        while (i < (response.length - 1)) {
            if ((byte) (response[i] & 0xC0) == 0xC0) {
                int pointer = (int) ((response[i] & 0x3F) * Math.pow(2, 8)) + response[i + 1];
                i = i + 2;
                names.addAll(getName(response, pointer).getKey());
            }
            StringBuilder name = new StringBuilder();
            while (response[i] != 0) {
                char curChar = (char) response[i];
                if (Character.isDigit(curChar)) {
                    i++;
                    curChar = (char) response[i];
                    i++;
                    for (int j = 0; j < (int) curChar; j++) {
                        curChar = (char) response[i];
                        i++;
                        if (Character.isLetter(curChar)) {
                            name.append(curChar);
                        } else {
                            result = new Pair<ArrayList<String>, Integer>(names, i);
                            return result;
                        }
                    }

                } else {
                    result = new Pair<ArrayList<String>, Integer>(names, i);
                    return result;
                }
                name.append('.');

            }
            if (name.toString().compareTo("") != 0)
                names.add(name.toString());


        }
        result = new Pair<>(names, i);
        return result;

    }
}
