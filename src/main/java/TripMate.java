package com.tripmate;

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
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.cdimascio.dotenv.Dotenv;

public class TripMate {
    private static final Logger LOGGER = Logger.getLogger(TripMate.class.getName());
    
    // Reusable HTTP client with optimized configuration
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    
    // Jackson ObjectMapper for JSON parsing (thread-safe and reusable)
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    
    // Compiled regex patterns for better performance
    private static final Pattern MATH_PATTERN = Pattern.compile(".*\\d+\\s*[+\\-*/]\\s*\\d+.*");
    private static final Pattern JSON_TEXT_PATTERN = Pattern.compile(
        "\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"", Pattern.DOTALL);
    
    // Cache for travel-related checks to avoid redundant API calls
    private static final Map<String, Boolean> TRAVEL_CACHE = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;
    
    // API configuration
    private static final String API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    
    // Prompts as constants
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

    // Default greeting as fallback
    private static final String DEFAULT_GREETING = 
        "🌍 Welcome to TripMate! Ready to explore amazing destinations and plan your next adventure? ✈️";

    /**
     * Generates a greeting using the API with fallback to default greeting
     */
    public static CompletableFuture<String> generateGreetingAsync(String apiKey) {
        return makeApiCallAsync(GREETING_PROMPT, apiKey)
            .thenApply(response -> response != null ? "TripMate: " + response : "TripMate: " + DEFAULT_GREETING)
            .exceptionally(throwable -> {
                LOGGER.warning("Failed to generate greeting: " + throwable.getMessage());
                return "TripMate: " + DEFAULT_GREETING;
            });
    }

    /**
     * Synchronous version for backwards compatibility
     */
    public static String generateGreeting(String apiKey) {
        try {
            return generateGreetingAsync(apiKey).get();
        } catch (Exception e) {
            LOGGER.warning("Greeting generation failed: " + e.getMessage());
            return "TripMate: " + DEFAULT_GREETING;
        }
    }

    /**
     * Checks if a message is travel-related with caching
     */
    public static boolean isTravelRelated(String message, String apiKey) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        String normalizedMessage = message.trim().toLowerCase();
        
        // Check cache first
        if (TRAVEL_CACHE.containsKey(normalizedMessage)) {
            return TRAVEL_CACHE.get(normalizedMessage);
        }
        
        // Manage cache size
        if (TRAVEL_CACHE.size() >= MAX_CACHE_SIZE) {
            TRAVEL_CACHE.clear(); // Simple cache eviction
        }
        
        try {
            String prompt = TRAVEL_CHECK_PROMPT + "\nInput: " + message;
            String response = makeApiCall(prompt, apiKey);
            boolean isTravel = response != null && response.trim().equalsIgnoreCase("true");
            
            // Cache the result
            TRAVEL_CACHE.put(normalizedMessage, isTravel);
            return isTravel;
            
        } catch (Exception e) {
            LOGGER.warning("Travel check failed for: " + message + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Optimized math detection using compiled pattern
     */
    public static boolean isMathRelated(String message) {
        return message != null && MATH_PATTERN.matcher(message).matches();
    }

    /**
     * Prints help information
     */
    public static void printHelp() {
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

    /**
     * Handles user input and generates response
     */
    public static void handleResponse(String userInput, String apiKey) {
        if (userInput == null || userInput.trim().isEmpty()) {
            System.out.println("TripMate: Please provide a valid question!");
            return;
        }
        
        try {
            String prompt = SYSTEM_PROMPT + "\nUser: " + userInput;
            String response = makeApiCall(prompt, apiKey);
            
            if (response != null && !response.trim().isEmpty()) {
                System.out.println("TripMate: " + response);
            } else {
                System.out.println("TripMate: I'm having trouble understanding that. Could you rephrase your question?");
                LOGGER.warning("Empty response for input: " + userInput);
            }
            
        } catch (Exception e) {
            System.out.println("TripMate: I'm experiencing technical difficulties. Please try again!");
            LOGGER.severe("Error handling response for input '" + userInput + "': " + e.getMessage());
        }
    }

    /**
     * Makes an API call synchronously
     */
    private static String makeApiCall(String prompt, String apiKey) throws IOException, InterruptedException {
        return makeApiCallAsync(prompt, apiKey).join();
    }

    /**
     * Makes an API call asynchronously
     */
    private static CompletableFuture<String> makeApiCallAsync(String prompt, String apiKey) {
        try {
            String requestBody = buildRequestBody(prompt);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::handleApiResponse);
                
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Handles API response and extracts content
     */
    private String handleApiResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        
        if (statusCode == 200) {
            String responseText = parseJsonResponse(response.body());
            if (responseText == null) {
                LOGGER.warning("Empty response content received");
            }
            return responseText;
        }
        
        String errorMsg = getErrorMessage(statusCode);
        LOGGER.severe(errorMsg + " - Status: " + statusCode);
        throw new RuntimeException(errorMsg);
    }

    /**
     * Builds the JSON request body
     */
    private static String buildRequestBody(String prompt) {
        try {
            // Using Jackson for proper JSON construction
            Map<String, Object> requestMap = Map.of(
                "contents", java.util.List.of(
                    Map.of("parts", java.util.List.of(
                        Map.of("text", prompt)
                    ))
                )
            );
            return JSON_MAPPER.writeValueAsString(requestMap);
        } catch (Exception e) {
            // Fallback to manual JSON construction
            return "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJsonString(prompt) + "\"}]}]}";
        }
    }

    /**
     * Gets appropriate error message based on status code
     */
    private static String getErrorMessage(int statusCode) {
        switch (statusCode) {
            case 401:
            case 403:
            return "Authentication error with Gemini API. Check API key.";
            case 429:
            return "API quota exceeded. Try again later.";
            case 400:
            return "Bad request. Check your input format.";
            case 500:
            return "Server error. Try again later.";
            default:
            return "API error: HTTP " + statusCode;
        }
    }

    /**
     * Improved JSON parsing using Jackson with regex fallback
     */
    public static String parseJsonResponse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Try Jackson first for robust JSON parsing
            JsonNode root = JSON_MAPPER.readTree(json);
            JsonNode candidates = root.path("candidates");
            
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content").path("parts");
                if (content.isArray() && content.size() > 0) {
                    String text = content.get(0).path("text").asText();
                    return cleanResponseText(text);
                }
            }
        } catch (Exception e) {
            LOGGER.info("Jackson parsing failed, falling back to regex: " + e.getMessage());
        }
        
        // Fallback to regex parsing
        return parseJsonWithRegex(json);
    }

    /**
     * Fallback regex-based JSON parsing
     */
    private static String parseJsonWithRegex(String json) {
        try {
            Matcher matcher = JSON_TEXT_PATTERN.matcher(json);
            if (matcher.find()) {
                String text = matcher.group(1)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t");
                
                return cleanResponseText(text);
            }
        } catch (Exception e) {
            LOGGER.severe("JSON parsing error: " + e.getMessage());
        }
        
        LOGGER.warning("No text content found in JSON response");
        return null;
    }

    /**
     * Cleans response text by removing unwanted metadata
     */
    private static String cleanResponseText(String text) {
        if (text == null) return null;
        
        // Remove common unwanted suffixes
        String[] unwantedMarkers = {"\"role\"", "\"finishReason\"", "\"index\":"};
        
        for (String marker : unwantedMarkers) {
            int idx = text.indexOf(marker);
            if (idx > 0) {
                text = text.substring(0, idx).trim();
            }
        }
        
        return text.trim();
    }

    /**
     * Improved JSON string escaping
     */
    private static String escapeJsonString(String input) {
        if (input == null) return "";
        
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f");
    }

    /**
     * Clears the travel cache (useful for testing or memory management)
     */
    public static void clearCache() {
        TRAVEL_CACHE.clear();
    }

    /**
     * Gets cache statistics for monitoring
     */
    public static String getCacheStats() {
        return String.format("Cache size: %d/%d", TRAVEL_CACHE.size(), MAX_CACHE_SIZE);
    }
}