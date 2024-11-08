import java.io.*;
import java.net.*;

public class Client {
    private static final String SERVER_ADDRESS = "172.16.110.103";
    private static final int PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Lidhur me serverin në portin " + PORT);

            String message;
            while (true) {
                System.out.print("Shkruaj mesazhin (ose 'exit' për të dalë): ");
                message = userInput.readLine();
                if (message.equalsIgnoreCase("exit")) {
                    out.println(message);
                    break;
                }

                out.println(message);
                String response = in.readLine();
                System.out.println("Serveri tha: " + response);
            }

        } catch (ConnectException e) {
            System.out.println("Nuk mund të lidhet me serverin. Kontrolloni nëse serveri është aktiv dhe provoni përsëri.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
