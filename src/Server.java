import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {
    private static final int SERVER_PORT = 12345;
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String LOG_FILE = "src//log.txt";
    private static final String LOG_FILE1 = "audit_log.txt";
    private static Map<String, Boolean> tokenMap = new HashMap<>();
    private DatagramSocket serverSocket;

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
                if (message.equals(ADMIN_PASSWORD)) {
                    String token = UUID.randomUUID().toString();
                    tokenMap.put(token, true); // true indicates admin privileges
                    response = "Your admin token: " + token;
                } else if (message.split(" ").length == 1) {
                    String token = UUID.randomUUID().toString();
                    tokenMap.put(token, false); // false indicates regular privileges
                    response = "Your token: " + token;
                } else {
                    String[] parts;
                    String token = null;
                    String command = null;
                    System.out.println("Received message: " + message); // Log the received message
                    if (message.startsWith("Your admin token:")) {
                        String[] mainParts = message.split("token: ", 2);
                        if (mainParts.length > 1) {
                            String[] tokenAndCommand = mainParts[1].split(" ", 2);
                            token = tokenAndCommand[0];
                            command = tokenAndCommand.length > 1 ? tokenAndCommand[1] : "";
                        }
                    } else {
                        parts = message.split(" ", 2);
                        token = parts[0];
                        command = parts[1];
                    }
                    System.out.println("Token: " + token + ", Command: " + command);

                    if (tokenMap.containsKey(token)) {
                        boolean isAdmin = tokenMap.get(token);
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

                            case "--list_files":
                                if (isAdmin) {
                                    response = listFilesInDirectory("src");
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

                if (!message.startsWith("--")) {
                    System.out.println("Client message: " + message); // Show message from client
                }

                byte[] sendBuffer = response.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, clientAddress, clientPort);
                serverSocket.send(sendPacket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
