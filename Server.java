import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 5555;
    private static Set<ClientHandler> clientHandlers = ConcurrentHashMap.newKeySet();
    
    public static void main(String[] args) {
        System.out.println("=== LAN Chat Server Started ===");
        System.out.println("Listening on port: " + PORT);
        System.out.println("Waiting for clients to connect...\n");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                
                ClientHandler handler = new ClientHandler(clientSocket);
                clientHandlers.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                // Get username
                out.println("ENTER_USERNAME");
                username = in.readLine();
                
                if (username == null || username.trim().isEmpty()) {
                    username = "Anonymous";
                }
                
                System.out.println(username + " joined the chat");
                broadcastMessage("SERVER: " + username + " joined the chat", this);
                
                // Listen for messages
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("/quit")) {
                        break;
                    }
                    System.out.println(username + ": " + message);
                    broadcastMessage(username + ": " + message, null);
                }
                
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                disconnect();
            }
        }
        
        private void broadcastMessage(String message, ClientHandler excludeClient) {
            for (ClientHandler client : clientHandlers) {
                if (client != excludeClient && client.out != null) {
                    client.out.println(message);
                }
            }
        }
        
        private void disconnect() {
            try {
                clientHandlers.remove(this);
                if (username != null) {
                    System.out.println(username + " left the chat");
                    broadcastMessage("SERVER: " + username + " left the chat", this);
                }
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }
    }
}