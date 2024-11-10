import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class Server {
    private static final int SERVER_PORT = 12345;
    private DatagramSocket serverSocket;
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String LOG_FILE = "audit_log.txt";
    private static Map<String, Boolean> tokenMap = new HashMap<>();


    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT)) {
            System.out.println("Server is running...");

            byte[] receiveBuffer = new byte[1024];
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);

                String message = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();

                String response;

                // Check if it's a new client requesting a token
                //if (message.equalsIgnoreCase("REQUEST_TOKEN")) {
                // Send a prompt for the admin password
                //    response = "Shenoni passwordin: ";
                //}
                // Handle password response for admin token request
                //else
                if (message.equals(ADMIN_PASSWORD)) {
                    String token = UUID.randomUUID().toString();
                    tokenMap.put(token, true); // true indicates admin privileges
                    response = "Your admin token: " + token;
                }
                // Handle non-admin token requests
                else if (message.split(" ").length == 1) {
                    // Generate a standard token for non-admin users
                    String token = UUID.randomUUID().toString();
                    tokenMap.put(token, false); // false indicates regular privileges
                    response = "Your token: " + token;
                }
                // Handle command requests with tokens
                else {
                    String[] parts;
                    String token = null;
                    String command=null;
                    System.out.println(message);
                    if(message.startsWith("Your admin token:")){
                        String[] mainParts = message.split("token: ", 2); // Separate prefix from token and command
                        if (mainParts.length > 1) {
                            String[] tokenAndCommand = mainParts[1].split(" ", 2); // Separate token and command
                            token = tokenAndCommand[0]; // Extract the token
                            command = tokenAndCommand.length > 1 ? tokenAndCommand[1] : "";
                            //parts=message.split("",3);
                            //token=parts[2];
                            //command=parts[3];
                        }
                    }else {
                        parts = message.split(" ", 2);
                        token = parts[0];
                        command = parts[1];
                    }
                    System.out.println("mesazhi: "+message);
                    System.out.println("tokeni: "+ token);
                    System.out.println("komanda: "+command);
                    // Check if the token is valid
                   
if (tokenMap.containsKey(token)) {
                        boolean isAdmin = tokenMap.get(token);
                        switch (command) {
                            case "--help":
                                response = "Available commands: --help, --read, --write, --execute";
                                if (isAdmin) {
                                    response += " (Admin only)";
                                }
                                break;

                            case "--read":
                                response = readFile(LOG_FILE);
                                break;

                            case "--write":
                                if (isAdmin) {
                                    response = writeFile("Shkruan nga komanda --write në " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
                                } else {
                                    response = "Permission denied. Admins only.";
                                }
                                break;

                            case "--execute":
                                if (isAdmin) {
                                    response = listActiveTokens();
                                } else {
                                    response = "Permission denied. Admins only.";
                                }
                                break;

                            default:
                                response = "Invalid command.";
                        }
                    } else {
                        response = "Invalid or expired token.";
                    }

                    System.out.println("Received command: '" + command + "' with token: '" + token + "' from " + clientAddress + ":" + clientPort);
                }
                // Send response to client
                byte[] sendBuffer = response.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, clientAddress, clientPort);
                serverSocket.send(sendPacket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
     private static String listActiveTokens() {
        StringBuilder tokenStatus = new StringBuilder("Active tokens:\n");
        for (Map.Entry<String, Boolean> entry : tokenMap.entrySet()) {
            tokenStatus.append("Token: ").append(entry.getKey())
                    .append(" - Admin: ").append(entry.getValue()).append("\n");
        }
        return tokenStatus.toString();
    }
}
