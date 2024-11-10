import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 12345;
    private static String token = null;
    private static DatagramSocket clientSocket = null;

    public static void main(String[] args) {

        try {
            clientSocket = new DatagramSocket();
            InetAddress serverAddress = InetAddress.getByName(SERVER_ADDRESS);
            byte[] receiveBuffer = new byte[1024];
            Scanner scanner = new Scanner(System.in);

            System.out.print("Enter initial command or 'REQUEST_TOKEN' to start: ");
            String initialCommand = scanner.nextLine().trim();
            byte[] sendBuffer = initialCommand.getBytes();
            DatagramPacket requestPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddress, SERVER_PORT);
            clientSocket.send(requestPacket);

            // Receive server's response (either a token or password prompt)
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            clientSocket.receive(receivePacket);
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();

            // Step 2: Handle admin password if prompted
            if (response.startsWith("Your admin token: ")) {
                System.out.print("Enter admin password: ");
                String password = scanner.nextLine().trim();

                // Send the password to the server
                sendBuffer = password.getBytes();
                DatagramPacket passwordPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddress, SERVER_PORT);
                clientSocket.send(passwordPacket);

                // Receive the response after sending the password
                receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                clientSocket.receive(receivePacket);
                String passwordResponse = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                System.out.println("Server response: " + passwordResponse);

                // Check if the response contains a token
                if (passwordResponse.startsWith("Your admin token: ")) {
                    token = passwordResponse.replace("Your token: ", "").trim();
                    System.out.println("Received admin token: " + token);
                } else {
                    System.out.println("Failed to obtain admin token. Incorrect password, Exiting.");
                    clientSocket.close();  // Mbyllni socket-in kur dështoni
                    return;
                }
            } else if (response.startsWith("Your token: ")) {
                // If the server directly gives a token without a password
                token = response.replace("Your token: ", "").trim();
                System.out.println("Received token: " + token);
            } else {
                System.out.println("Failed to obtain a token. Exiting.");
                clientSocket.close();  // Mbyllni socket-in kur dështoni
                return;
            }

            // Step 3: Send commands to the server using the token
            while (true) {
                System.out.print("Enter command (--help, --read, --write, --execute, --list_files) or 'exit' to quit: ");
                String command = scanner.nextLine().trim();

                if (command.equalsIgnoreCase("exit")) {
                    System.out.println("Exiting client...");
                    break;
                }

                String message = token + " " + command;
                sendBuffer = message.getBytes();
                DatagramPacket commandPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddress, SERVER_PORT);
                clientSocket.send(commandPacket);

                receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                clientSocket.receive(receivePacket);
                String commandResponse = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();

                if (commandResponse.equals("You have been disconnected due to inactivity.")) {
                    System.out.println(commandResponse);
                    break;
                }

                System.out.println("Server response: " + commandResponse);
            }

            clientSocket.close();
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
