package org.example;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serveur de chat simple, sécurisé et robuste.
 */
public class Server {

    private final int port;
    private List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private List<String> history = Collections.synchronizedList(new ArrayList<>());
    private ServerSocket serverSocket;
    private volatile boolean isRunning = false;
    private final AtomicInteger clientCount = new AtomicInteger(0);

    private static final int ACCEPT_TIMEOUT_MS = 10_000; // timeout pour accept()
    private static final int SOCKET_READ_TIMEOUT_MS = 2 * 60 * 1000; // timeout lecture client (2 minutes)
    private static final int MAX_HISTORY = 100;
    private static final int MAX_MESSAGE_LENGTH = 512; // protège mémoire
    private static final int MAX_USERNAME_LENGTH = 32;
    private static final int MAX_CONNECTIONS = 200; // limite basique anti-DoS

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

                // DoS protection : limiter le nombre de connexions simultanées
                if (clients.size() >= MAX_CONNECTIONS) {
                    try (PrintWriter tmpOut = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true)) {
                        tmpOut.println("Server overloaded. Try later.");
                    } catch (IOException ignore) {
                    } finally {
                        try {
                            clientSocket.close();
                        } catch (IOException ignored) {
                        }
                    }
                    continue;
                }

                // Timeout lecture pour détecter clients inactifs
                try {
                    clientSocket.setSoTimeout(SOCKET_READ_TIMEOUT_MS);
                } catch (Exception e) {
                    // Si on ne peut pas définir le timeout, on continue quand même
                }

                addClient(clientSocket);
            } catch (SocketTimeoutException e) {
                // Pas de connexion pendant le timeout, continuer la boucle
            } catch (IOException e) {
                if (isRunning) {
                    System.out.println("Error accepting client: " + e.getMessage());
                }
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
        serverSocket.setSoTimeout(ACCEPT_TIMEOUT_MS);
    }

    /**
     * Arrêt propre du serveur.
     */
    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing server socket: " + e.getMessage());
        }

        // Fermer tous les clients
        for (ClientHandler client : clients) {
            try {
                client.closeConnection();
            } catch (Exception ignored) {
            }
        }
        clients.clear();
        System.out.println("Server stopped.");
    }

    /**
     * Diffuse un message à tous les clients sauf l'expéditeur.
     */
    public void broadcastMessage(ClientHandler sender, String message) {
        if (message == null) return;

        // Tronquer si trop long (défense supplémentaire)
        if (message.length() > MAX_MESSAGE_LENGTH) {
            message = message.substring(0, MAX_MESSAGE_LENGTH);
        }

        // Ajout dans l'historique (taille limitée)
        history.add(message);
        if (history.size() > MAX_HISTORY) {
            // remove oldest
            if (!history.isEmpty()) {
                history.remove(0);
            }
        }

        // Itération thread-safe grâce à CopyOnWriteArrayList
        for (ClientHandler client : clients) {
            if (client == null) continue;
            if (client == sender) continue;
            if (client.username == null || client.out == null) continue;
            try {
                client.out.println(message);
            } catch (Exception e) {
                // Ignorer erreurs d'envoi ; cleanup se fera ailleurs
            }
        }
    }

    /**
     * Envoie l'historique des messages à un nouveau client.
     */
    public void sendHistoryToClient(ClientHandler client) {
        if (client == null || client.out == null) return;
        for (String msg : history) {
            client.out.println(msg);
        }
    }

    /**
     * Lit une ligne depuis le reader sans allouer une très grande chaîne si le client
     * envoie une ligne énorme. Tronque/consomme l'excès sans OOM.
     *
     * @param reader BufferedReader
     * @param maxLen taille maximale autorisée
     * @return la ligne lue, ou null si fin de flux
     * @throws IOException en cas d'erreur IO
     */
    private static String readLimitedLine(BufferedReader reader, int maxLen) throws IOException {
        StringBuilder sb = new StringBuilder(Math.min(128, maxLen));
        int ch;
        boolean sawChar = false;
        int count = 0;

        reader.mark(1);
        while ((ch = reader.read()) != -1) {
            sawChar = true;
            // Nouvelline -> fin de ligne
            if (ch == '\n') break;
            if (ch == '\r') {
                // gère \r\n ou solo \r
                reader.mark(1);
                int next = reader.read();
                if (next != '\n') {
                    reader.reset();
                }
                break;
            }
            // Exclure caractères de contrôle non souhaités (sauf tab)
            if (Character.isISOControl(ch) && ch != '\t') {
                // ignorer
                continue;
            }
            if (count < maxLen) {
                sb.append((char) ch);
            }
            count++;
            // Si on dépasse la limite, consommer le reste de la ligne sans le stocker
            if (count > maxLen) {
                while ((ch = reader.read()) != -1 && ch != '\n') {
                    // consommer
                }
                break;
            }
            reader.mark(1);
        }

        if (!sawChar && ch == -1) {
            return null; // fin de flux
        }
        return sb.toString();
    }

    /**
     * Nettoie un message pour éviter contrôle chars et séquences potentiellement dangereuses.
     */
    private static String sanitizeMessage(String msg) {
        if (msg == null) return "";
        // supprimer null bytes
        msg = msg.replace("\u0000", "");
        // supprimer autres caractères de contrôle (sauf tab/newline)
        msg = msg.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        // échappement simple HTML pour éviter problèmes si affiché dans UI web futur
        msg = msg.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return msg.trim();
    }

    /**
     * Valide un nom d'utilisateur (caractères autorisés et longueur).
     */
    private static boolean isValidUsername(String name) {
        if (name == null) return false;
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_USERNAME_LENGTH) return false;
        return trimmed.matches("[A-Za-z0-9_\\-\\.]+");
    }

    /**
     * Handler qui gère la connexion d'un client.
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
            this.clientId = clientCount.getAndIncrement();
        }

        @Override
        public void run() {
            try {
                initStreams();
                handleUserJoin();

                sendHistoryToClient(this);
                broadcastMessage(this, message);

                listenForMessages();
                handleUserLeave();
            } catch (SocketTimeoutException e) {
                System.out.println("Client " + clientId + " timed out: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("Client " + clientId + " error: " + e.getMessage());
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
            String name = readLimitedLine(reader, MAX_USERNAME_LENGTH);
            if (!isValidUsername(name)) {
                username = "Anonymous" + clientId;
            } else {
                username = name.trim();
            }
            message = username + " has joined the chat.";
            System.out.println(message);
        }

        private void handleUserLeave() {
            if (username == null) username = "Anonymous" + clientId;
            String leaveMsg = username + " has left the chat.";
            System.out.println(leaveMsg);
            broadcastMessage(this, leaveMsg);
        }

        private void listenForMessages() throws IOException {
            String received;
            while ((received = readLimitedLine(reader, MAX_MESSAGE_LENGTH)) != null) {
                received = sanitizeMessage(received);
                if (received.isEmpty()) continue;
                message = username + ": " + received;
                System.out.println(message);
                broadcastMessage(this, message);
            }
            // si readLimitedLine retourne null => fin de flux => client déconnecté
        }

        /**
         * Ferme proprement la connexion et libère les ressources.
         */
        private void closeConnection() {
            try {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignored) {
                    }
                }
                if (out != null) out.close();
                if (socket != null && !socket.isClosed()) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }
            } finally {
                clients.remove(this);
            }
        }
    }
}
