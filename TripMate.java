import java.util.Arrays;
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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TripMate {
    private static final Logger LOGGER = Logger.getLogger(TripMate.class.getName());
    private static final String[] TRAVEL_KEYWORDS = {
        "trip", "travel", "vacation", "itinerary", "destination", "tourism", "journey",
        "flight", "hotel", "resort", "booking", "visit", "tour", "cruise", "backpack",
        "sightseeing", "adventure", "explore", "city break", "holiday", "getaway",
        "airport", "visa", "passport", "luggage", "accommodation", "restaurant",
        "tourist", "guide", "map", "transportation", "train", "bus", "rental car",
        "beach", "mountain", "museum", "landmark", "culture", "local", "weather",
        "budget travel", "luxury travel", "backpacking", "road trip", "honeymoon",
        "family vacation", "solo travel", "group travel", "business travel"
    };

    // Simulated system prompt
    private static final String SYSTEM_PROMPT = 
        "You are TripMate, a specialized travel assistant chatbot. Your expertise is in helping users with travel planning, " +
        "destinations, itineraries, accommodations, transportation, travel tips, cultural information, and all travel-related queries. " +
        "Provide detailed, helpful, and engaging responses about travel topics. Be enthusiastic about travel and offer practical advice, " +
        "recommendations, and insights to enhance the user's travel experience. If the query is vague, suggest popular destinations or ask " +
        "for clarification. If the query is not travel-related, politely redirect the user to ask about travel topics.";

    public static void main(String[] args) {
        // Set up logging
        try {
            FileHandler fileHandler = new FileHandler("tripmate.log");
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.SEVERE);
        } catch (IOException e) {
            System.out.println("Error setting up logging: " + e.getMessage());
            return;
        }

        // Check for API key
        String googleApiKey = System.getenv("GOOGLE_API_KEY");
        if (googleApiKey == null || googleApiKey.isEmpty()) {
            System.out.println("Error: GOOGLE_API_KEY not found in environment variables. Please set it and try again.");
            LOGGER.severe("GOOGLE_API_KEY not found in environment variables.");
            return;
        }

        System.out.println("🌍 Welcome to TripMate - Your Travel Assistant! ✈️");
        printHelp();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\nYou: ");
            String userInput = scanner.nextLine().trim();

            // Handle special commands
            if (userInput.equalsIgnoreCase("exit") || userInput.equalsIgnoreCase("quit") || userInput.equalsIgnoreCase("bye")) {
                System.out.println("✈️ Safe travels and goodbye! Thanks for using TripMate!");
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

            // Check if input is travel-related
            if (!isTravelRelated(userInput)) {
                System.out.println("TripMate: I'm specialized in travel-related questions only! Please ask about trips, destinations, " +
                                   "travel planning, accommodations, or other travel topics. 🌎✈️ Try typing 'help' for ideas!");
                continue;
            }

            // Handle response
            handleResponse(userInput);
        }
        scanner.close();
    }

    public static boolean isTravelRelated(String message) {
        String lowerMessage = message.toLowerCase();
        return Arrays.stream(TRAVEL_KEYWORDS).anyMatch(lowerMessage::contains);
    }

    public static void printHelp() {
        System.out.println(
            "🌍 TripMate Help 🌍\n" +
            "I'm here to assist with all your travel needs! Here are some example questions:\n" +
            "- \"What's the best time to visit Japan?\"\n" +
            "- \"Can you suggest a 5-day itinerary for Paris?\"\n" +
            "- \"What are budget-friendly hotels in New York?\"\n" +
            "- \"How do I get around in Rome using public transport?\"\n" +
            "- \"What are must-see landmarks in London?\"\n" +
            "Type 'exit' to quit or 'help' to see this message again.\n"
        );
    }

    public static void handleResponse(String userInput) {
        try {
            String googleApiKey = "AIzaSyATdVI59TbqVsT0vL7qid6SNd0wghu7bHI";
            // load appi key from env file
            
            String prompt = SYSTEM_PROMPT + "\nUser: " + userInput;

            // Create JSON payload manually
            String requestBody = createJsonPayload(prompt);

            // Set up HttpClient
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + googleApiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            // Send request and get response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            if (statusCode == 200) {
                // Parse JSON response manually
                String responseText = parseJsonResponse(response.body());
                if (responseText != null && !responseText.isEmpty()) {
                    System.out.print("TripMate: " + responseText);
                    System.out.println();
                } else {
                    System.out.println("TripMate: No response content received from API.");
                    LOGGER.severe("No content in API response for input: " + userInput);
                }
            } else if (statusCode == 401 || statusCode == 403) {
                System.out.println("TripMate: Authentication error with Gemini API. Please check your API key.");
                LOGGER.severe("Authentication error with Gemini API. Status code: " + statusCode);
            } else if (statusCode == 429) {
                System.out.println("TripMate: API quota exceeded. Please try again later or check your plan.");
                LOGGER.severe("API quota exceeded. Status code: " + statusCode);
            } else {
                System.out.println("TripMate: Error from API: HTTP " + statusCode);
                LOGGER.severe("API error with status code: " + statusCode + " for input: " + userInput);
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("TripMate: Sorry, I encountered an error: " + e.getMessage() + ". Please try again!");
            LOGGER.severe("Error processing response: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("TripMate: Unexpected error: " + e.getMessage() + ". Please try again!");
            LOGGER.severe("Unexpected error processing response: " + e.getMessage());
        }
    }

    /**
     * Creates JSON payload manually without using external JSON library
     */
    private static String createJsonPayload(String prompt) {
        // Escape special characters in the prompt
        String escapedPrompt = escapeJsonString(prompt);
        
        // Build JSON structure manually
        return "{" +
               "\"contents\": [" +
               "{" +
               "\"parts\": [" +
               "{" +
               "\"text\": \"" + escapedPrompt + "\"" +
               "}" +
               "]" +
               "}" +
               "]" +
               "}";
    }

    /**
     * Parses JSON response manually to extract the text content
     */
    private static String parseJsonResponse(String jsonResponse) {
        try {
            // Use regex to find the text content in the JSON response
            // Pattern to match: "text":"content"
            Pattern pattern = Pattern.compile("\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
            Matcher matcher = pattern.matcher(jsonResponse);
            
            if (matcher.find()) {
                String extractedText = matcher.group(1);
                // Unescape JSON string
                return unescapeJsonString(extractedText);
            }
            
            return null;
        } catch (Exception e) {
            LOGGER.severe("Error parsing JSON response: " + e.getMessage());
            return null;
        }
    }

    /**
     * Escapes special characters for JSON string
     */
    private static String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    // Handle other control characters
                    if (c < 32 || c > 126) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * Unescapes JSON string
     */
    private static String unescapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            if (escape) {
                switch (c) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        // Handle unicode escape sequence
                        if (i + 4 < input.length()) {
                            String hex = input.substring(i + 1, i + 5);
                            try {
                                int codePoint = Integer.parseInt(hex, 16);
                                sb.append((char) codePoint);
                                i += 4; // Skip the next 4 characters
                            } catch (NumberFormatException e) {
                                sb.append(c); // If parsing fails, just append the character
                            }
                        } else {
                            sb.append(c);
                        }
                        break;
                    default:
                        sb.append(c);
                        break;
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else {
                sb.append(c);
            }
        }
        
        return sb.toString();
    }
}