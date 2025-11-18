package org.example;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Classe représentant un client de chat simple, robuste.
 */
public class Client {

    private final String serverAddress;
    private final int serverPort;
    private Socket socket;
    private ExecutorService executorService;
    private BufferedReader consoleReader;
    private volatile boolean running = false;
    private int messageCount = 0;

    private static final int MAX_MESSAGE_LENGTH = 512; // message trop long

    public Client(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    /**
     * Connexion au serveur et démarrage des threads de lecture/écriture.
     */
    public void connect() throws IOException, InterruptedException, ExecutionException {
        createSocket();
        executorService = Executors.newFixedThreadPool(2);
        running = true;

        Future<?> receiverTask = executorService.submit(this::receiveMessages);
        // léger délai pour s'assurer que le reader est prêt (optionnel)
        Thread.sleep(100);
        Future<?> senderTask = executorService.submit(this::sendMessages);

        // attendre que les deux tâches se terminent (si elles se terminent)
        try {
            receiverTask.get();
            senderTask.get();
        } finally {
            shutdown();
        }
    }

    private void createSocket() throws IOException {
        socket = new Socket(serverAddress, serverPort);
        // Ne pas mettre un timeout trop court côté client, sinon on sera déconnecté automatiquement.
        // Si tu veux détecter inactivité côté client, définir un timeout long ou gérer ping/pong.
    }

    /**
     * Lecture des messages depuis le serveur.
     */
    private void receiveMessages() {
        BufferedReader reader = null;
        try {
            reader = createReader();
            while (running) {
                String message;
                try {
                    message = reader.readLine();
                } catch (SocketException se) {
                    System.out.println("Socket error while reading: " + se.getMessage());
                    break;
                }

                if (message == null) {
                    System.out.println("Server closed the connection.");
                    break;
                }
                if (message.trim().isEmpty()) {
                    continue;
                }
                displayReceivedMessage(message);
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server: " + e.getMessage());
        } finally {
            // Ne pas fermer le socket ici si l'autre thread écrit encore ; shutdown gère cela
            try {
                if (reader != null) reader.close();
            } catch (IOException ignored) {
            }
            running = false;
        }
    }

    private BufferedReader createReader() throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private void displayReceivedMessage(String message) {
        if (message != null) {
            System.out.println("\r" + message);
            System.out.print("You: ");
        }
    }

    /**
     * Envoi de messages au serveur.
     */
    private void sendMessages() {
        try (BufferedWriter writer = createWriter()) {
            consoleReader = createConsoleReader();
            String input;
            while (running && (input = readUserInput()) != null) {
                if (!input.trim().isEmpty()) {
                    // Tronquer localement si message trop long
                    if (input.length() > MAX_MESSAGE_LENGTH) {
                        input = input.substring(0, MAX_MESSAGE_LENGTH);
                        System.out.println("[Warning] Message truncated to " + MAX_MESSAGE_LENGTH + " characters.");
                    }
                    try {
                        sendMessage(writer, input);
                        incrementMessageCount();
                    } catch (IOException e) {
                        System.out.println("Failed to send message: " + e.getMessage());
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error in sendMessages: " + e.getMessage());
        } finally {
            running = false;
        }
    }

    private BufferedWriter createWriter() throws IOException {
        return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    private BufferedReader createConsoleReader() {
        return new BufferedReader(new InputStreamReader(System.in));
    }

    private String readUserInput() throws IOException {
        if (consoleReader == null) return null;
        return consoleReader.readLine();
    }

    private void sendMessage(BufferedWriter writer, String input) throws IOException {
        writer.write(input);
        writer.newLine();
        writer.flush();
        System.out.print("You: ");
    }

    private void incrementMessageCount() {
        messageCount++;
    }

    /**
     * Arrêt propre du client.
     */
    private void shutdown() {
        running = false;
        if (executorService != null) {
            executorService.shutdownNow();
        }
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {
        }
        try {
            if (consoleReader != null) consoleReader.close();
        } catch (IOException ignored) {
        }
        System.out.println("Client shutdown complete.");
    }
}
