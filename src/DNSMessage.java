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

    byte[] temp;

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
        temp = packet.getData();
        //todo: check it???
        responseB.rewind();
        byte b0 = responseB.get();
        byte b1 = responseB.get();
        short ID = (short) (b0 | (b1 << 8));
        //todo: still not working...
//        if (ID != id) {
//            throw new ResponseException("The response id does not match the query id.");
//        }

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

        responseB.position(header.size() + question.size());
        for (int i = 0; i < ANCOUNT; i++) {
            String name = getName(responseB);
            short anType = responseB.getShort();
            short anClass = responseB.getShort();
            int ttl = responseB.getInt();
            short rdLength = responseB.getShort();
            String rdata = "";
            if (anType == 1) {
                rdata = processARdata(responseB);
            } else if (anType == 2) {
                rdata = processNSRdata(responseB);
            } else if (anType == 5) {
                rdata = processCNAMERdata(responseB);
            } else if (anType == 15) {
                Pair<Short, String> p = processMXRdata(responseB);
            }
        }
        //todo: put the response together or maybe create a data type?
        //todo: skip the authority part
        //todo: process the additional section (same structure as the answer section)


        return null;
    }

    private Pair<Short, String> processMXRdata(ByteBuffer response) {
        short preference = response.getShort();
        String exchange = getName(response);
        return new Pair<>(preference, exchange);
    }

    private String processCNAMERdata(ByteBuffer response) {
        return getName(response);
    }

    private String processNSRdata(ByteBuffer response) {
        return getName(response);
    }

    private String processARdata(ByteBuffer responseB) {
        responseB.get();
        StringBuilder sBuilder = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sBuilder.append(responseB.get()).append('.');
        }
        sBuilder.deleteCharAt(sBuilder.length() - 1);
        return sBuilder.toString();
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

    /**
     * may use this function in RDATA and NAME of the answer section
     * @param responseB
     * @return
     */
    private String getName(ByteBuffer responseB) {
        // todo: keep a map to be referred to when accessing through pointer?
        // move the position to answer section

        StringBuilder name = new StringBuilder();
        int p = getNameByByte(responseB,
                responseB.position(),
                name);

        responseB.position(p);
        return name.toString();
    }

    /**
     * this function will build the string until it sees a pointer or 0
     * will not change the current position of the response
     * will return where it stops back
     * @param response
     * @param position
     * @param name
     * @return
     */
    private int getNameByByte(ByteBuffer response, int position, StringBuilder name) {
        response = response.duplicate();
        response.position(position);
        byte temp = response.get();
        while (((temp & 0b11000000) != 0b11000000) && temp != 0) {
            byte partLength = temp;
            temp = response.get();     // move it to the start of the name part
            for (byte i = 0; i < partLength; i++, temp = response.get()) {
                name.append((char) temp);
            }
            name.append('.');
        }

        if (temp == 0) {
            return response.position();
        }

        byte p = response.get();
        getNameByByte(response, p, name);

        return response.position();
    }

}
