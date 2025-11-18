package org.example;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Classe représentant un client de chat simple.
 */
public class Client {

    private final String serverAddress;
    private final int serverPort;
    private Socket socket;
    private ExecutorService executorService;
    private BufferedReader consoleReader;
    private int messageCount = 0;

    private static final int SOCKET_TIMEOUT_MS = 10000; // 10 secondes
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

        Future<?> receiverTask = executorService.submit(this::receiveMessages);
        Thread.sleep(100);
        Future<?> senderTask = executorService.submit(this::sendMessages);

        receiverTask.get();
        senderTask.get();

        shutdown();
    }

    private void createSocket() throws IOException {
        socket = new Socket(serverAddress, serverPort);
        socket.setSoTimeout(SOCKET_TIMEOUT_MS);
    }

    /**
     * Lecture des messages depuis le serveur.
     */
    private void receiveMessages() {
        BufferedReader reader = null;
        try {
            reader = createReader();
            while (true) {
                String message = reader.readLine();
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
            System.out.println("Disconnected from server.");
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
            while ((input = readUserInput()) != null) {
                if (!input.trim().isEmpty()) {
                    if (input.length() > MAX_MESSAGE_LENGTH) {
                        input = input.substring(0, MAX_MESSAGE_LENGTH);
                        System.out.println("[Warning] Message truncated to " + MAX_MESSAGE_LENGTH + " characters.");
                    }
                    sendMessage(writer, input);
                    incrementMessageCount();
                }
            }
        } catch (IOException e) {
            System.out.println("Error sending message: " + e.getMessage());
        }
    }

    private BufferedWriter createWriter() throws IOException {
        return new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    private BufferedReader createConsoleReader() {
        return new BufferedReader(new InputStreamReader(System.in));
    }

    private String readUserInput() throws IOException {
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
    private void shutdown() throws IOException {
        if (executorService != null) {
            executorService.shutdown();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
