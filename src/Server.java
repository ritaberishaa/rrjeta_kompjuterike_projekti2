import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

public class Server {

private static final int PORT = 12345;
private static final int MAX_CONNECTIONS = 5;
private static ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

public static void main(String[] args) {
    System.out.println("Server listening on port: " + PORT);
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
        while (true) {
            if (clients.size() < MAX_CONNECTIONS) {
                Socket clientSocket = serverSocket.accept();
                String clientIP = clientSocket.getInetAddress().getHostAddress();
                System.out.println("Client connected: " + clientIP);
                
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.put(clientIP, clientHandler);
                
                new Thread(clientHandler).start();
            } else {
                System.out.println("Max connection capacity reached. Waiting...");
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}

static class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean hasFullAccess;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.hasFullAccess = clients.size() == 0;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                logRequest(socket.getInetAddress().getHostAddress(), inputLine);
                System.out.println("Message received from client: " + inputLine);

                if (inputLine.equalsIgnoreCase("exit")) {
                    break;
                }

                if (hasFullAccess) {
                    out.println("Full access: Message received - " + inputLine);
                } else {
                    out.println("Read-only: Message received - " + inputLine);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            clients.remove(socket.getInetAddress().getHostAddress());
            System.out.println("Client disconnected: " + socket.getInetAddress().getHostAddress());
        }
    }
}

        
  
}
