import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 12345;
    private static final int BUFFER_SIZE = 1024;
    private static ConcurrentHashMap<String, Boolean> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Server listening on port: " + PORT);

        try (DatagramSocket serverSocket = new DatagramSocket(PORT)) {
            byte[] receiveData = new byte[BUFFER_SIZE];

            while (true) {
                // Merr mesazhin nga klienti
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);

                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                String clientIP = receivePacket.getAddress().getHostAddress();

                System.out.println("Message received from client " + clientIP + ": " + message);

                if (message.equalsIgnoreCase("exit")) {
                    System.out.println("Client disconnected: " + clientIP);
                    clients.remove(clientIP);
                    continue;
                }

                // Log mesazhin
                logRequest(clientIP, message);

                // Përgjigju klientit
                String response = (clients.getOrDefault(clientIP, false)) ?
                        "Full access: Message received - " + message :
                        "Read-only: Message received - " + message;

                byte[] sendData = response.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivePacket.getAddress(), receivePacket.getPort());
                serverSocket.send(sendPacket);

                // Vendos qasje për klientin e parë
                clients.putIfAbsent(clientIP, clients.size() == 0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void logRequest(String clientIP, String message) {
        try (FileWriter fw = new FileWriter("log.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter logWriter = new PrintWriter(bw)) {
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            logWriter.println("IP: " + clientIP + " | Koha: " + timeStamp + " | Mesazhi: " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
