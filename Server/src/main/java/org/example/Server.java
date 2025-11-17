package org.example;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

/**
 * Serveur de chat simple.
 */
public class Server {

    private final int port;
    private List<ClientHandler> clients = new ArrayList<>();
    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private List<String> history = new ArrayList<>();
    private int clientCount = 0;

    private static final int SOCKET_TIMEOUT_MS = 10000; // 10 secondes
    private static final int MAX_HISTORY = 100;
    private static final int MAX_MESSAGE_LENGTH = 512;

    public Server(int port) {
        this.port = port;
    }

    /**
     * Démarrage du serveur.
     */
    public void start() throws IOException {
        initSocket();
        isRunning = true;
        System.out.println("Chat server started on port " + port);

        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(SOCKET_TIMEOUT_MS);
                addClient(clientSocket);
            } catch (SocketTimeoutException e) {
                // Pas de connexion pendant le timeout, continuer
            }
        }
    }

    private void addClient(Socket clientSocket) {
        ClientHandler clientHandler = new ClientHandler(clientSocket, this);
        clients.add(clientHandler);
        new Thread(clientHandler).start();
    }

    private void initSocket() throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress("0.0.0.0", port));
        serverSocket.setSoTimeout(SOCKET_TIMEOUT_MS);
    }

    public void stop() throws IOException {
        isRunning = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }

    /**
     * Diffuse un message à tous les clients sauf l'expéditeur.
     */
    public void broadcastMessage(ClientHandler sender, String message) {
        if (message == null) return;

        if (message.length() > MAX_MESSAGE_LENGTH) {
            message = message.substring(0, MAX_MESSAGE_LENGTH);
        }

        history.add(message);
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
        }

        for (ClientHandler client : clients) {
            if (client != sender && client.username != null) {
                try {
                    client.out.println(message);
                } catch (Exception e) {
                    // client déconnecté ?
                }
            }
        }
    }

    /**
     * Envoie l'historique des messages à un nouveau client.
     */
    public void sendHistoryToClient(ClientHandler client) {
        for (String msg : history) {
            client.out.println(msg);
        }
    }

    /**
     * Gestionnaire de client.
     */
    class ClientHandler implements Runnable {

        private final Socket socket;
        private PrintWriter out;
        private String username;
        private final int clientId;
        private BufferedReader reader;
        private String message;

        public ClientHandler(Socket socket, Server server) {
            this.socket = socket;
            this.clientId = clientCount++;
        }

        public void run() {
            try {
                initStreams();
                handleUserJoin();

                sendHistoryToClient(this);
                broadcastMessage(this, message);

                listenForMessages();
                handleUserLeave();
            } catch (IOException e) {
                System.out.println("Client disconnected or error: " + e.getMessage());
            } finally {
                closeConnection();
            }
        }

        private void initStreams() throws IOException {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        }

        private void handleUserJoin() throws IOException {
            out.println("Enter your name: ");
            username = reader.readLine();
            if (username == null || username.trim().isEmpty()) {
                username = "Anonymous" + clientId;
            }
            message = username + " has joined the chat.";
            System.out.println(message);
        }

        private void handleUserLeave() {
            String leaveMsg = username + " has left the chat.";
            System.out.println(leaveMsg);
            broadcastMessage(this, leaveMsg);
            closeConnection();
        }

        private void listenForMessages() throws IOException {
            String received;
            while ((received = reader.readLine()) != null) {
                if (!received.trim().isEmpty()) {
                    if (received.length() > MAX_MESSAGE_LENGTH) {
                        received = received.substring(0, MAX_MESSAGE_LENGTH);
                    }
                    message = username + ": " + received;
                    System.out.println(message);
                    broadcastMessage(this, message);
                }
            }
        }

        private void closeConnection() {
            try {
                if (reader != null) reader.close();
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) socket.close();
                clients.remove(this);
            } catch (IOException ignored) {
            }
        }
    }
}
