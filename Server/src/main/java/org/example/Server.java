package org.example;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import com.google.common.collect.Lists;

public class Server {
    private int p;
    private List<ClientHandler> _clientsList = new ArrayList<>();
    private ServerSocket ss;
    private boolean isRunning = false;
    private List<String> hist = new ArrayList<>();
    private int count = 0;

    public Server(int port) {
        this.p = port;
    }

    public void start() throws IOException {
        ss = new ServerSocket();
        ss.bind(new InetSocketAddress("0.0.0.0", p));
        isRunning = true;
        System.out.println("Chat server started on port " + p);

        while (isRunning) {
            Socket cs = ss.accept();
            ClientHandler ch = new ClientHandler(cs, this);
            _clientsList.add(ch);
            Thread t = new Thread(ch);
            t.start();
        }
    }

    public void stop() throws IOException {
        isRunning = false;
        if (ss != null && !ss.isClosed()) {
            ss.close();
        }
    }

    // méthode pour envoyer message à tout le monde
    public void broadcastMessage(ClientHandler expediteur, String msg) {
        hist.add(msg);
        if (hist.size() > 100) {
            hist.remove(0);
        }

        for (int i = 0; i < _clientsList.size(); i++) {
            ClientHandler c = _clientsList.get(i);
            if (c != expediteur && c.nomUtilisateur != null) {
                try {
                    c.out.println(msg);
                } catch (Exception e) {
                    // client déconnecté ?
                }
            }
        }
    }

    // envoi historique
    public void sendHistoryToClient(ClientHandler c) {
        for (int i = 0; i < hist.size(); i++) {
            c.out.println(hist.get(i));
        }
    }

    // Classe interne pour gérer chaque client
    class ClientHandler implements Runnable {
        Socket s;
        PrintWriter out;
        String nomUtilisateur;
        private int clientId;

        public ClientHandler(Socket socket, Server srv) {
            this.s = socket;
            this.clientId = count++;
        }

        public void run() {
            try {
                InputStream in = s.getInputStream();
                BufferedReader r = new BufferedReader(new InputStreamReader(in));
                OutputStream outStream = s.getOutputStream();
                out = new PrintWriter(new OutputStreamWriter(outStream), true);

                out.println("Enter your name: ");
                nomUtilisateur = r.readLine();
                String m = nomUtilisateur + " has joined the chat.";
                System.out.println(m);

                sendHistoryToClient(this);
                broadcastMessage(this, m);

                String messageRecu;
                while ((messageRecu = r.readLine()) != null) {
                    m = nomUtilisateur + ": " + messageRecu;
                    System.out.println(m);
                    broadcastMessage(this, m);
                }

                String msgLeave = nomUtilisateur + " has left the chat.";
                System.out.println(msgLeave);
                broadcastMessage(this, msgLeave);

            } catch (IOException e) {
                System.out.println("Client error");
            }
        }
    }
}