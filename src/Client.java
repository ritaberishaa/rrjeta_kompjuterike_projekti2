import java.io.*;
import java.net.*;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int PORT = 12345;
    private static final int BUFFER_SIZE = 1024;
    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket();
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))){


            System.out.println("Jemi lidhur me serverin ne portin "+PORT);

            String message;
            while (true) {
                System.out.print("Shkruaj mesazhin (ose 'exit' për të dalë): ");
                message = userInput.readLine();

                if (message.equalsIgnoreCase("exit")) {
                    break;
                }

                byte[] sendData = message.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(SERVER_ADDRESS), PORT);
                socket.send(sendPacket);

                byte[] receiveData = new byte[BUFFER_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);

                String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Serveri tha: " + response);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
