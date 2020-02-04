import javafx.util.Pair;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DNSClient {
    enum Type {A, MX, NS}

    static int timeOutMs = 5000;
    static int maxRetries = 3;
    static int port = 53;
    static Type type = Type.A;
    static ByteBuffer server;
    static String domainName;

    static int retryCount = 0;
    static int headerSize;
	static int questionSize;
	static int id;

	private static DatagramPacket buildQueryPacket(InetAddress ip) {
		ArrayList<Byte> header = new ArrayList<Byte>();
        Random random = new Random();
		id = random.nextInt(4096);
		header.add((byte) id);//stores 8-15 bits of the id
		header.add((byte)(id>>8));//stores 0-7 bits of the id
		header.add((byte) 1);//RD is set to 1
		header.add((byte) 0);
		header.add((byte) 0);
		header.add((byte) 1);//QDCount will always be 1
		header.add((byte) 0);
		header.add((byte) 0);
		header.add((byte) 0);
		header.add((byte) 0);
		header.add((byte) 0);
		header.add((byte) 0);
		headerSize = header.size();
		
		int qType = getQType();
//		byte[] question = new byte[questionSize];
//		question[2]=0x0;
//		question[3]=(byte)qType;
//		question[4]=0x0;
//		question[5]=0x1;
		ArrayList<Byte> question = new ArrayList<>();
        addNameToQuestion(question);
		question.add((byte)qType);
		question.add((byte)0x0);
		question.add((byte)0x1);
        questionSize = question.size();

        ArrayList<Byte> packetArrayList = new ArrayList<>(header);
		byte[] packetArray = new byte[packetArrayList.size()];
		for(int i=0; i<packetArrayList.size();i++) {
			packetArray[i]=packetArrayList.get(i);
		}
		// TODO: ignore the other part or put 0s in them
        return new DatagramPacket(packetArray,packetArray.length,ip,port);
	}
	
    private static String interpretResponsePacket(DatagramPacket packet) throws ResponseException {
		byte[] response = packet.getData();

        //todo: check it???
		int ID = (int) (response[0]*Math.pow(2, 8) + response[1]);
		if (ID != id) {
		    throw new ResponseException("The response id does not match the query id.");
        }

        //todo: check correct calculation???
		int AA = response[2] & 8;       //AA is at bit 5??
		int RA = response[3] & 8;       //RA at bit 0??
		if(RA==0) {
		    //TODO: check if RA is 0 indicates that not support recursion
            throw new ResponseException("Error: Recursive queries are not suppported.");
		}
		int RCODE = response[3] & 15;   //RCODE at bit 4-7
		if (RCODE == 1) {
			throw new ResponseException("Format error: the name server was unable to interpret the query.");
		}else if (RCODE == 2) {
            throw new ResponseException("Server Failure: the name server was unable to process this qeury due to a problem with the name server.");
		}else if (RCODE == 3) {
            throw new ResponseException("Name error: meaningful only for responses from an authoritative name server, this code signifies that the domain name referenced in the query does not exist");
		}else if (RCODE == 4) {
            throw new ResponseException("Not implemented: the name server does not support the requested kind of query");
		}else if (RCODE == 5) {
            throw new ResponseException("Refused: the name server refuses to perform the requested operation for policy reasons");
		}

		int QDCOUNT = (int) (response[4]*Math.pow(2, 8) + response[5]);
		int ANCOUNT = (int) (response[6]*Math.pow(2, 8) + response[7]);
		int NSCOUNT = (int) (response[8]*Math.pow(2, 8) + response[9]);
		int ARCOUNT = (int) (response[10]*Math.pow(2, 8) + response[11]);
		//TODO: parse until the data set.

        Pair<ArrayList<String>, Integer> temp = getName(response, headerSize + questionSize);
        ArrayList<String> names = temp.getKey();
        int currentIndex = temp.getValue();
		
				
		return null;
	}
    public static void main(String[] args) {
        try {
            parseUserInput(args);
            testParser();
            InetAddress addr = InetAddress.getByAddress(server.array());
            DatagramSocket socket = new DatagramSocket(port, addr);
            socket.setSoTimeout(timeOutMs);
            DatagramPacket queryPacket = buildQueryPacket(addr);

            int lengthResponseBytes = 100;
            byte[] responseBytes = new byte[lengthResponseBytes];
            DatagramPacket responsePacket = new DatagramPacket(responseBytes, lengthResponseBytes, addr, port);
            socket.send(queryPacket);

            tryReceiveResponse(socket, responsePacket);
            String interpredResult = interpretResponsePacket(responsePacket);
            System.out.println(interpredResult);

        } catch (UserInputException e) {
            System.out.println("ERROR\t" + "Incorrect input syntax: " + e.getMessage());
        } catch (UnknownHostException e) {
            System.out.println("ERROR\t" + "IP address provided is unknown: " + Arrays.toString(server.array()));
        } catch (SocketException e) {
            System.out.println("ERROR\t" + "Cannot open socket");
        } catch (SocketTimeoutException e) {
            System.out.println("ERROR\t");
        } catch (IOException e) {
            System.out.println("ERROR\t" + "I/O exception at send");
        } catch (ResponseException e) {
            System.out.println(e.getErrorMessage());
        }
    }

	static Pair<ArrayList<String>, Integer> getName(byte[]response, int index){
		
		ArrayList<String> names = new ArrayList<String>();
		Integer i = index;//i is the current index
		Pair <ArrayList<String>, Integer> result;
		while(i<(response.length-1)) {
			if((byte)(response[i]&0xC0)==0xC0) {
				int pointer = (int) ((response[i]&0x3F)*Math.pow(2, 8))+response[i+1];
				i=i+2;
				names.addAll(getName(response, pointer).getKey());
			}
			String name = "";
			while(response[i]!=0) {
				char curChar = (char) response[i];
				if (Character.isDigit(curChar)) {
					i++;
					curChar = (char) response[i];
					i++;
					for(int j =0; j<(int)curChar;j++) {
						curChar = (char) response[i];
						i++;
						if(Character.isLetter(curChar)) {
							name = name + curChar;
						}else {
							result = new Pair<ArrayList<String>, Integer>(names, i); 
							return result;
						}
					}
					
				}else {
					result= new Pair<ArrayList<String>, Integer>(names,i);
					return result;
				}
				name = name + '.';
				
			}
			if(name.compareTo("")!=0)
				names.add(name);
			
				
		}
		result = new Pair<ArrayList<String>, Integer>(names,i);
		return result;
		
	}

	private static int getQType() {
        if(type==Type.A) {
            return 0x1;
        }
        if(type==Type.NS) {
            return 0x2;
        }
        if(type==Type.MX) {
            return 0xf;
        }

        // not belong to any type. should be an exception...
        return 0;
    }

    private static void addNameToQuestion(ArrayList<Byte> question) {
        String[] fragments = domainName.split("\\.");
        for (String fragment : fragments) {
            int length = fragment.length();
            question.add((byte) length);
            byte[] qName = fragment.getBytes();
            for (byte b : qName) {
                question.add(b);
            }
        }
        question.add((byte)0x0);
    }

    private static void tryReceiveResponse(DatagramSocket socket, DatagramPacket responsePacket) throws SocketTimeoutException  {
        //todo: double check this
        try {
            socket.receive(responsePacket);
        } catch (SocketTimeoutException e) {
            if (retryCount > maxRetries) {
                throw new SocketTimeoutException("Exceed max retry limit.");
            }
            retryCount ++;
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
                    throw  new UserInputException("Invalid ip address");
                }

                ArrayList<Integer> intLabels = new ArrayList<>();
                for (String label : labels) {
                    Integer intValue = Integer.parseInt(label);
                    if (intValue >= 128) {
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
            throw  new UserInputException("Name is not provided");
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
