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


            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            clientSocket.receive(receivePacket);
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();


            if (response.startsWith("Your admin token: ")) {
                System.out.print("Enter admin password: ");
                String password = scanner.nextLine().trim();


                sendBuffer = password.getBytes();
                DatagramPacket passwordPacket = new DatagramPacket(sendBuffer, sendBuffer.length, serverAddress, SERVER_PORT);
                clientSocket.send(passwordPacket);


                receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                clientSocket.receive(receivePacket);
                String passwordResponse = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                System.out.println("Server response: " + passwordResponse);


                if (passwordResponse.startsWith("Your admin token: ")) {
                    token = passwordResponse.replace("Your admin token: ", "").trim();
                    System.out.println("Received admin token: " + token);
                } else {
                    System.out.println("Failed to obtain admin token. Incorrect password, Exiting.");
                    clientSocket.close();
                    return;
                }
            } else if (response.startsWith("Your token: ")) {

                token = response.replace("Your token: ", "").trim();
                System.out.println("Received token: " + token);
            } else {
                System.out.println("Failed to obtain a token. Exiting.");
                clientSocket.close();
                return;
            }


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
                System.out.println("Server response: " + commandResponse);
            }

            clientSocket.close();
            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
