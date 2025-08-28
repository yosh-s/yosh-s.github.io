// package com.tripmate;

import java.util.Scanner;
import java.util.logging.Logger;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        String googleApiKey = "AIzaSyATdVI59TbqVsT0vL7qid6SNd0wghu7bHI";
        String greeting = generateGreeting(googleApiKey);
        System.out.println(greeting);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\nYou: ");
            String userInput = scanner.nextLine().trim();

            if (userInput.equalsIgnoreCase("exit") || userInput.equalsIgnoreCase("quit")) {
                System.out.println("✈️ Safe travels! Thanks for using TripMate!");
                break;
            }
            if (userInput.isEmpty()) {
                System.out.println("Please enter a message.");
                continue;
            }
            handleResponse(userInput, googleApiKey);
        }
        scanner.close();
    }

    public static String generateGreeting(String apiKey) {
        String GREETING_PROMPT = "Generate a greeting for a new user of the TripMate app. The greeting should be friendly, welcoming, and encourage the user to explore the app's features. It should also include a call to action to start planning their next trip.";
        try {
            String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + (GREETING_PROMPT) + "\"}]}]}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key="
                                    + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                String greeting = extractText(responseBody);
                return greeting;
            } else {
                System.err.println("Error: " + response.statusCode() + " - " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.severe("Greeting generation error: " + e.getMessage());
        }
        return null; // Fallback to static greeting if API fails
    }

    public static String extractText(String jsonString) {
        Pattern pattern = Pattern.compile("\"text\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(jsonString);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static void handleResponse(String userInput, String apiKey) {
        final String SYSTEM_PROMPT = "You are TripMate, a travel assistant chatbot. Help with travel planning, destinations, itineraries, accommodations, transportation, and travel tips. Provide detailed, enthusiastic responses. For vague queries, suggest popular destinations or ask for clarification. For non-travel queries, politely redirect to travel topics.";
        try {
            HttpResponse<String> response = HttpHandler(SYSTEM_PROMPT, userInput, apiKey);
            int statusCode = response.statusCode();

            if (statusCode == 200) {
                String responseText = extractText(response.body());
                System.out.println(
                        "TripMate: " + (responseText != null ? responseText : "No response content received."));
                if (responseText == null) {
                    LOGGER.warning("Empty response for input: " + userInput);
                }
            } else {
                String errorMsg;
                switch (statusCode) {
                    case 401:
                    case 403:
                        errorMsg = "Authentication error with Gemini API. Check API key.";
                        break;
                    case 429:
                        errorMsg = "API quota exceeded. Try again later.";
                        break;
                    default:
                        errorMsg = "API error: HTTP " + statusCode;
                        break;
                }
                System.out.println("TripMate: " + errorMsg);
                LOGGER.severe(errorMsg + " for input: " + userInput);
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("TripMate: Error: " + e.getMessage() + ". Try again!");
            LOGGER.severe("Request error: " + e.getMessage());
        }
    }

    public static HttpResponse<String> HttpHandler(String PROMPT, String USER_INPUT, String API_KEY)
            throws IOException, InterruptedException {
        String prompt = PROMPT + "\nUser: " + USER_INPUT;
        String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + prompt + "\"}]}]}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key="
                                + API_KEY))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response;

    }

}