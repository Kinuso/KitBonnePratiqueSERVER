package org.example;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.apache.commons.lang3.StringUtils;

// Main du client
public class Main {
    public static void main(String[] args) {
        String addr = "localhost";
        int _Port = 12345;
        Client c = new Client(addr, _Port);
        try {
            c.Connect();
        } catch (IOException | InterruptedException | ExecutionException e) {
            System.out.println("Failed");
        }
        // System.out.println("Client terminé");
    }
}