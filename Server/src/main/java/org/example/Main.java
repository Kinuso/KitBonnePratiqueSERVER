package org.example;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;

public class Main {
    public static void main(String[] args) {
        // Port du serveur
        int p = 12345;
        Server s = new Server(p);
        try {
            s.start();
        } catch (IOException e) {
            System.out.println("erreur");
        }
        // System.out.println("Code inutile");
        // ArrayList<String> temp = new ArrayList<>();
    }
}