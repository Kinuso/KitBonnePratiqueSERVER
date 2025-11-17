package org.example;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Point d'entrée du client de chat.
 */
public class Main {

    /**
     * Démarre le client et tente de se connecter au serveur.
     *
     * @param args arguments de ligne de commande (non utilisés)
     */
    public static void main(String[] args) {
        final String SERVER_ADDRESS = "localhost";
        final int SERVER_PORT = 12345;

        Client client = new Client(SERVER_ADDRESS, SERVER_PORT);
        try {
            client.connect();
        } catch (IOException | InterruptedException | ExecutionException e) {
            System.out.println("Échec de connexion au serveur");
        }
    }
}
