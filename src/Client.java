import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 12345;
    private static String token = null;
    public static void main(String[] args) {
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName(SERVER_ADDRESS);
            byte[] receiveBuffer = new byte[1024];

            // Step 1: Request a token from the server
            Scanner scanner2= new Scanner(System.in);
            String commanda = scanner2.nextLine().trim();
            String requestTokenMessage = commanda;//"REQUEST_TOKEN";
            byte[] sendBuffer = requestTokenMessage.getBytes();
            DatagramPacket requestPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddress, SERVER_PORT);
            clientSocket.send(requestPacket);

            // Receive token from server
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            clientSocket.receive(receivePacket);
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
            System.out.println("Server response: " + response);

            // Extract the token from the response
            if (response.startsWith("Your token: ")) {
                token = response.split("\n")[0].replace("Your token: ", "").trim();
                System.out.println("Received token: " + token);
            } else {
                System.out.println("Failed to obtain a token. Exiting.");
                return;
            }

            // Step 2: Send commands to the server
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Enter command (--help, --read, --write) or 'exit' to quit: ");
                String command = scanner.nextLine().trim();

                if (command.equalsIgnoreCase("exit")) {
                    System.out.println("Exiting client...");
                    break;
                }

                // Send the command with the token to the server
                String message = token + " " + command;
                sendBuffer = message.getBytes();
                DatagramPacket commandPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddress, SERVER_PORT);
                clientSocket.send(commandPacket);

                // Receive the server's response
                receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                clientSocket.receive(receivePacket);
                response = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                System.out.println("Server response: " + response);
            }

            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
