import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.github.cdimascio.dotenv.Dotenv;

public class TripMate {
    private static final Logger LOGGER = Logger.getLogger(TripMate.class.getName());
    private static final String SYSTEM_PROMPT =
        "You are TripMate, a travel assistant chatbot. Help with travel planning, destinations, itineraries, " +
        "accommodations, transportation, and travel tips. Provide detailed, enthusiastic responses. " +
        "For vague queries, suggest popular destinations or ask for clarification. For non-travel queries, " +
        "politely redirect to travel topics.";
    private static final String TRAVEL_CHECK_PROMPT =
        "Determine if the following user input is related to travel, such as travel planning, destinations, " +
        "itineraries, accommodations, transportation, travel tips, geographic information (e.g., cities, countries, capitals), " +
        "or budget-related questions that could apply to travel planning (e.g., trip budgets, affordable destinations). " +
        "Respond with only 'true' or 'false'.";
    private static final String GREETING_PROMPT =
        "Generate a short, enthusiastic, travel-themed greeting for TripMate, a travel assistant chatbot. " +
        "The greeting should be unique, welcoming, and inspire users to explore travel ideas. " +
        "Keep it concise (1-2 sentences) and include at least one emoji. Do not include any non-travel content.";

    public static void main(String[] args) {
        // Load .env variables
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        // Initialize logging
        try {
            FileHandler fileHandler = new FileHandler("tripmate.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.INFO);
        } catch (IOException e) {
            System.out.println("Error setting up logging: " + e.getMessage());
            LOGGER.severe("Logging setup failed: " + e.getMessage());
            return;
        }

        // Check API key
        String googleApiKey = dotenv.get("GOOGLE_API_KEY");
        if (googleApiKey == null || googleApiKey.isEmpty()) {
            System.out.println("Error: GOOGLE_API_KEY not set or invalid.");
            LOGGER.severe("GOOGLE_API_KEY not set or invalid.");
            return;
        }


        // Generate dynamic greeting
        String greeting = generateGreeting(googleApiKey);
        System.out.println(greeting != null ? greeting : "🌍 Welcome to TripMate - Your Travel Assistant! ✈️");
        printHelp();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\nYou: ");
            String userInput = scanner.nextLine().trim();

            if (userInput.equalsIgnoreCase("exit") || userInput.equalsIgnoreCase("quit")) {
                System.out.println("✈️ Safe travels! Thanks for using TripMate!");
                break;
            }
            if (userInput.equalsIgnoreCase("help")) {
                printHelp();
                continue;
            }
            if (userInput.isEmpty()) {
                System.out.println("Please enter a message.");
                continue;
            }

            if (!isTravelRelated(userInput, googleApiKey)) {
                if (isMathRelated(userInput)) {
                    System.out.println("TripMate: You asked about " + userInput + " and it is related to math, and I'm specialized for trip planning so I can't help you with that.");
                } else {
                    System.out.println("TripMate: It looks like your question might not be travel-related. Could you clarify how it relates to travel? For example, try asking about a budget-friendly trip to Paris or the best time to visit Japan! Type 'help' for more ideas! 🌎");
                }
                LOGGER.info("Non-travel query: " + userInput);
                continue;
            }

            handleResponse(userInput, googleApiKey);
        }
        scanner.close();
    }

    private static String generateGreeting(String apiKey) {
        try {
            String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJsonString(GREETING_PROMPT) + "\"}]}]}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String responseText = parseJsonResponse(response.body());
                if (responseText != null && !responseText.trim().isEmpty()) {
                    return "TripMate: " + responseText;
                }
                LOGGER.warning("Empty or invalid greeting response from API");
            } else {
                LOGGER.warning("Greeting API error: HTTP " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.severe("Greeting generation error: " + e.getMessage());
        }
        return null; // Fallback to static greeting if API fails
    }

    private static boolean isTravelRelated(String message, String apiKey) {
        try {
            String prompt = TRAVEL_CHECK_PROMPT + "\nInput: " + message;
            String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJsonString(prompt) + "\"}]}]}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String responseText = parseJsonResponse(response.body());
                return responseText != null && responseText.trim().equalsIgnoreCase("true");
            } else {
                LOGGER.warning("Travel check API error: HTTP " + response.statusCode() + " for input: " + message);
                return false; // Fallback to false on API error
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.severe("Travel check error: " + e.getMessage());
            return false; // Fallback to false on exception
        }
    }

    private static boolean isMathRelated(String message) {
        return message.matches(".*\\d+\\s*[+\\-*/]\\s*\\d+.*");
    }

    private static void printHelp() {
        System.out.println(
            "🌍 TripMate Help 🌍\n" +
            "Ask about travel planning, destinations, or tips! Examples:\n" +
            "- Best time to visit Japan?\n" +
            "- 5-day itinerary for Paris?\n" +
            "- Budget hotels in New York?\n" +
            "- Public transport in Rome?\n" +
            "Type 'exit' to quit or 'help' for this message."
        );
    }

    private static void handleResponse(String userInput, String apiKey) {
        try {
            String prompt = SYSTEM_PROMPT + "\nUser: " + userInput;
            String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJsonString(prompt) + "\"}]}]}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode == 200) {
                String responseText = parseJsonResponse(response.body());
                System.out.println("TripMate: " + (responseText != null ? responseText : "No response content received."));
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

    private static String escapeJsonString(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    private static String parseJsonResponse(String json) {
        try {
            Pattern pattern = Pattern.compile("\"text\"\\s*:\\s*\"(.*?)\"(?=\\s*,\\s*\"|})", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                String text = matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
                int unwantedIdx = text.indexOf("\"role\"");
                if (unwantedIdx > 0) {
                    text = text.substring(0, unwantedIdx).trim();
                }
                unwantedIdx = text.indexOf("\"finishReason\"");
                if (unwantedIdx > 0) {
                    text = text.substring(0, unwantedIdx).trim();
                }
                return text;
            }
            LOGGER.warning("No text content found in JSON response: " + json);
            return null;
        } catch (Exception e) {
            LOGGER.severe("JSON parsing error: " + e.getMessage());
            return null;
        }
    }
}