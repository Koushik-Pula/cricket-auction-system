package com.auction.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class AuctionClient {
    public static void main(String[] args) {
        String serverAddress = "127.0.0.1"; // localhost
        int port = 12345;

        try (Socket socket = new Socket(serverAddress, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            // Separate thread to listen to server messages
            Thread listener = new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        System.out.println("[SERVER] " + response);
                    }
                } catch (Exception e) {
                    System.out.println("Disconnected from server.");
                }
            });
            listener.start();

            // Sending input to server
            String userInput;
            while ((userInput = console.readLine()) != null) {
                out.println(userInput); // send to server
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
