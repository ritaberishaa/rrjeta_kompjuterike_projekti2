import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server {
    private static final int SERVER_PORT = 12345;
    private static final String ADMIN_PASSWORD = "admin123";

    private static final String ADMIN = "admin";
    private static String adminToken = null;
    private static final String LOG_FILE = "src//log.txt";
    private static final String LOG_FILE1 = "audit_log.txt";
    private static final int MAX_CLIENTS = 5;
    private static Map<String, Boolean> tokenMap = new HashMap<>();

    private static Queue<DatagramPacket> adminQueue = new ConcurrentLinkedQueue<>();
    private static Queue<DatagramPacket> regularQueue = new ConcurrentLinkedQueue<>();

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

                logMessage(clientAddress, message);

                String response;

                // Check maximum clients
                if (tokenMap.size() > MAX_CLIENTS) {
                    response = "Server is full. Maximum number of clients reached.";
                    sendResponse(serverSocket, response, clientAddress, clientPort);
                    continue;
                }

                // Admin token request
                else if (message.equals(ADMIN)) {
                    adminToken = UUID.randomUUID().toString();
                    tokenMap.put(adminToken, true);
                    response = "Your admin token: " + adminToken;
                    System.out.println("Admin client connected. Token: " + adminToken);
                    sendResponse(serverSocket, response, clientAddress, clientPort);
                    continue;
                }

                // Admin password check
                else if (message.equals(ADMIN_PASSWORD)) {
                    if (adminToken != null && tokenMap.containsKey(adminToken)) {
                        response = "Your admin token: " + adminToken;
                    } else {
                        response = "No active admin session. Please connect as admin first.";
                    }
                    sendResponse(serverSocket, response, clientAddress, clientPort);
                    continue;
                }

                // Regular client token request
                else if (message.equals("REQUEST_TOKEN")) {
                    String token = UUID.randomUUID().toString();
                    tokenMap.put(token, false);
                    response = "Your token: " + token;
                    System.out.println("Regular client connected. Token: " + token);
                    sendResponse(serverSocket, response, clientAddress, clientPort);
                    continue;
                }

                // Process commands based on token
                String[] parts = message.split(" ", 2);
                String token = parts[0];
                String command = parts.length > 1 ? parts[1] : "";

                if (tokenMap.containsKey(token)) {
                    boolean isAdmin = tokenMap.get(token);

                    // Queue requests based on admin status
                    if (isAdmin) {
                        adminQueue.add(receivePacket);
                    } else {
                        regularQueue.add(receivePacket);
                    }
                } else {
                    response = "Invalid or expired token.";
                    sendResponse(serverSocket, response, clientAddress, clientPort);
                }

                // Process requests from the queues
                DatagramPacket packetToProcess = !adminQueue.isEmpty() ? adminQueue.poll() : regularQueue.poll();

                if (packetToProcess != null) {
                    processRequest(serverSocket, packetToProcess);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processRequest(DatagramSocket serverSocket, DatagramPacket packet) throws IOException {
        String message = new String(packet.getData(), 0, packet.getLength()).trim();
        String[] parts = message.split(" ", 2);
        String token = parts[0];
        String command = parts.length > 1 ? parts[1] : "";

        String response;

        boolean isAdmin = tokenMap.getOrDefault(token, false);

        switch (command) {
            case "--help":
                response = "Available commands: --help, --read, --write, --execute, --list_files";
                if (isAdmin) {
                    response += " (Admin only)";
                }
                break;

            case "--read":
                response = readFile(LOG_FILE);
                break;

            case "--write":
                response = isAdmin ? writeFile("Written by --write command on "
                        + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n") : "Permission denied. Admins only.";
                break;

            case "--execute":
                response = isAdmin ? listActiveTokens() : "Permission denied. Admins only.";
                break;

            case "--list_files":
                response = isAdmin ? listFilesInDirectory("src") : "Permission denied. Admins only.";
                break;

            default:
                response = "Invalid command.";
        }

        sendResponse(serverSocket, response, packet.getAddress(), packet.getPort());
    }

    private static void sendResponse(DatagramSocket socket, String message, InetAddress clientAddress, int clientPort) throws IOException {
        byte[] sendBuffer = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, clientAddress, clientPort);
        socket.send(sendPacket);
    }

    private static void logMessage(InetAddress clientAddress, String message) {
        try (BufferedWriter logWriter = new BufferedWriter(new FileWriter(LOG_FILE1, true))) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            logWriter.write("[" + timestamp + "] [" + clientAddress.getHostAddress() + "] " + message + "\n");
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }

    private static String readFile(String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
        return content.toString();
    }

    private static String writeFile(String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("src//write.txt", true))) {
            writer.write(content);
        } catch (IOException e) {
            return "Error writing to file: " + e.getMessage();
        }
        return "Write command executed successfully.";
    }

    private static String listActiveTokens() {
        StringBuilder tokenStatus = new StringBuilder("Active tokens:\n");
        for (Map.Entry<String, Boolean> entry : tokenMap.entrySet()) {
            tokenStatus.append("Token: ").append(entry.getKey())
                    .append(" - Admin: ").append(entry.getValue()).append("\n");
        }
        return tokenStatus.toString();
    }

    private static String listFilesInDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        StringBuilder fileList = new StringBuilder();

        if (directory.exists() && directory.isDirectory()) {
            String[] files = directory.list();
            if (files != null && files.length > 0) {
                for (String file : files) {
                    fileList.append(file).append("\n");
                }
            } else {
                fileList.append("No files found.");
            }
        } else {
            fileList.append("Invalid directory.");
        }

        return fileList.toString();
    }
}
