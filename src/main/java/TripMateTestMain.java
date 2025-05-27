package com.tripmate;

import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.IOException;
import java.util.logging.Level;


public class TripMateTestMain {
    private static final Logger LOGGER = Logger.getLogger(TripMateTestMain.class.getName());

    public static void main(String[] args) {
        

        // Check API key
        String googleApiKey = System.getenv("GOOGLE_API_KEY");
        if (googleApiKey == null || googleApiKey.isEmpty()) {
            System.out.println("Error: GOOGLE_API_KEY not set or invalid.");
            LOGGER.severe("GOOGLE_API_KEY not set or invalid.");
            return;
        }

        // Generate dynamic greeting
        String greeting = TripMate.generateGreeting(googleApiKey);
        System.out.println(greeting != null ? greeting : "🌍 Welcome to TripMate - Your Travel Assistant! ✈️");
        TripMate.printHelp();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\nYou: ");
            String userInput = scanner.nextLine().trim();

            if (userInput.equalsIgnoreCase("exit") || userInput.equalsIgnoreCase("quit")) {
                System.out.println("✈️ Safe travels! Thanks for using TripMate!");
                break;
            }
            if (userInput.equalsIgnoreCase("help")) {
                TripMate.printHelp();
                continue;
            }
            if (userInput.isEmpty()) {
                System.out.println("Please enter a message.");
                continue;
            }

            if (!TripMate.isTravelRelated(userInput, googleApiKey)) {
                if (TripMate.isMathRelated(userInput)) {
                    System.out.println("TripMate: You asked about " + userInput + " and it is related to math, and I'm specialized for trip planning so I can't help you with that.");
                } else {
                    System.out.println("TripMate: It looks like your question might not be travel-related. Could you clarify how it relates to travel? For example, try asking about a budget-friendly trip to Paris or the best time to visit Japan! Type 'help' for more ideas! 🌎");
                }
                LOGGER.info("Non-travel query: " + userInput);
                continue;
            }

            TripMate.handleResponse(userInput, googleApiKey);
        }
        scanner.close();
    }
}