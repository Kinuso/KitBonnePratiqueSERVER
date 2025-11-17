package org.example;

import java.io.IOException;

/**
 * Point d'entrée du serveur.
 */
public class Main {

    /**
     * Démarre le serveur de chat.
     *
     * @param args arguments de ligne de commande (non utilisés)
     */
    public static void main(String[] args) {
        final int SERVER_PORT = 12345; // Port du serveur

        Server server = new Server(SERVER_PORT);
        try {
            server.start();
        } catch (IOException e) {
            System.out.println("Erreur lors du démarrage du serveur");
        }
    }
}
