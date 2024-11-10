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
    private Map<String, ClientInfo> clientMap = new HashMap<>();
    private String privilegedToken = null;
private static final long INACTIVITY_TIMEOUT_MS = 60000; // 1 minute timeout for inactivity
private ScheduledExecutorService inactivityChecker;



    public Server() throws Exception {
        serverSocket = new DatagramSocket(SERVER_PORT);
        startInactivityChecker();
        System.out.println("Server is running in port: "+SERVER_PORT);
    }

   private class ClientInfo {
    InetAddress address;
    int port;
    boolean isPrivileged;
    long lastActivityTime; // Track last activity time

    public ClientInfo(InetAddress address, int port, boolean isPrivileged) {
        this.address = address;
        this.port = port;
        this.isPrivileged = isPrivileged;
        this.lastActivityTime = System.currentTimeMillis(); // Initialize last activity time
    }

    public void updateLastActivity() {
        this.lastActivityTime = System.currentTimeMillis(); // Update activity time
    }
}



   public void start() throws Exception {
    byte[] receiveBuffer = new byte[1024];

    while (true) {
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        serverSocket.receive(receivePacket);

        InetAddress clientAddress = receivePacket.getAddress();
        int clientPort = receivePacket.getPort();
        String message = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();

        if (message.equals("REQUEST_TOKEN")) {
            // Generate a unique token for the client
            String token = UUID.randomUUID().toString();
            boolean isPrivileged = privilegedToken == null; // First client gets privileged access
            if (isPrivileged) {
                privilegedToken = token;
            }

            // Save client info
            clientMap.put(token, new ClientInfo(clientAddress, clientPort, isPrivileged));

            // Send the token to the client
            String response = "Your token: " + token + (isPrivileged ? " (privileged)" : " (read-only)");
            byte[] sendBuffer = response.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, clientAddress, clientPort);
            serverSocket.send(sendPacket);

            System.out.println("Assigned token: " + token + " to client at " + clientAddress + ":" + clientPort);
        } else {
            // Extract token and command from the message
            message = message.replace("(privileged) ", "").trim();
            String[] parts = message.split(" ", 3);
            if (parts.length < 2) {
                continue; // Invalid message format
            }
            String token = parts[0];
            String command = parts[2];

            // Validate the token and get client info
            ClientInfo clientInfo = clientMap.get(token);
            String response;

            if (clientInfo != null && clientInfo.address.equals(clientAddress) && clientInfo.port == clientPort) {
                clientInfo.updateLastActivity(); // Update last activity time on valid command
                boolean hasPrivileges = clientInfo.isPrivileged;

                // Process command based on privilege level
                switch (command.toLowerCase()) {
                    case "--help":
                        response = hasPrivileges ? "Privileged help content." : "Regular help content.";
                        break;
                    case "--read":
                        response = "Reading data...";
                        break;
                    case "--write":
                        response = hasPrivileges ? "Writing data..." : "Permission denied.";
                        break;
                    default:
                        response = "Unknown command. Type --help for a list of available commands.";
                }
            } else {
                response = "Invalid or expired token.";
            }

            // Send response to client
            byte[] sendBuffer = response.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, clientAddress, clientPort);
            serverSocket.send(sendPacket);

            System.out.println("Received command: '" + command + "' with token: '" + token + "' from " + clientAddress + ":" + clientPort);
        }
    }
}


        private void startInactivityChecker() {
    inactivityChecker = Executors.newSingleThreadScheduledExecutor();
    inactivityChecker.scheduleAtFixedRate(() -> {
        long currentTime = System.currentTimeMillis();
        clientMap.entrySet().removeIf(entry -> {
            ClientInfo clientInfo = entry.getValue();
            if (currentTime - clientInfo.lastActivityTime > INACTIVITY_TIMEOUT_MS) {
                System.out.println("Removing inactive client: " + entry.getKey());
                if (clientInfo.isPrivileged) {
                    privilegedToken = null; // Release privileged token if the inactive client was privileged
                }
                return true; // Remove client from map
            }
            return false;
        });
    }, INACTIVITY_TIMEOUT_MS, INACTIVITY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
}
    }

    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
