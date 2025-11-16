package org.example;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.io.IOUtils;
import com.google.gson.Gson;

public class Client {
    private String adr;
    private int p;
    private Socket s;
    private ExecutorService exec;
    private BufferedReader lecteurConsole;
    private int messageCount = 0;

    public Client(String serverAddress, int serverPort) {
        this.adr = serverAddress;
        this.p = serverPort;
    }

    // méthode pour se connecter
    public void Connect() throws IOException, InterruptedException, ExecutionException {
        s = new Socket(adr, p);
        exec = Executors.newFixedThreadPool(2);

        Future<?> t1 = exec.submit(this::receiveMessages);
        Thread.sleep(100);
        Future<?> t2 = exec.submit(this::sendMessages);

        t1.get();
        t2.get();

        shutdown();
    }

    // reception des messages
    private void receiveMessages() {
        try {
            InputStream inputStream = s.getInputStream();
            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader r = new BufferedReader(isr);
            String msg;
            while ((msg = r.readLine()) != null) {
                System.out.println("\r" + msg);
                System.out.print("You: ");
            }
        } catch (IOException e) {
            System.out.println("Disconnected");
        }
        // TODO: fermer le reader
    }

    // envoi messages
    private void sendMessages() {
        try {
            OutputStream outputStream = s.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(outputStream);
            BufferedWriter w = new BufferedWriter(osw);
            lecteurConsole = new BufferedReader(new InputStreamReader(System.in));
            String input;
            while ((input = lecteurConsole.readLine()) != null) {
                w.write(input);
                w.newLine();
                w.flush();
                System.out.print("You: ");
                messageCount = messageCount + 1;
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // arrêt propre
    private void shutdown() throws IOException {
        if (exec != null) {
            exec.shutdown();
        }
        if (s != null && !s.isClosed()) {
            s.close();
        }
    }

    private String formatMessage(String msg) {
        return msg.trim();
    }
}