import io.github.cdimascio.dotenv.Dotenv;
import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.logging.*;

public class TripMate {

    private static final Logger LOGGER = Logger.getLogger(TripMate.class.getName());
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Map<String, String> greetingCache = new HashMap<>();

    public static void main(String[] args) {
        // Load .env file
        Dotenv dotenv = Dotenv.load();
        String googleApiKey = dotenv.get("GOOGLE_API_KEY");
        if (googleApiKey == null) {
            System.err.println("ERROR: GOOGLE_API_KEY not found in .env file");
            System.exit(1);
        }

        setupLogger();
        printWelcome(googleApiKey);
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\nYou: ");
            String userInput = scanner.nextLine().trim();

            if (userInput.isEmpty()) continue;
            if (userInput.equalsIgnoreCase("exit") || userInput.equalsIgnoreCase("quit")) {
                System.out.println("TripMate: Goodbye! Safe travels! ✈️🌍");
                break;
            }

            System.out.print("TripMate: ");
            showSpinner();

            CompletableFuture.supplyAsync(() -> processUserInput(userInput, googleApiKey))
                .thenAccept(response -> {
                    clearSpinner();
                    System.out.println(response);
                })
                .exceptionally(e -> {
                    clearSpinner();
                    System.out.println("TripMate: Oops! There was a problem processing your request.");
                    LOGGER.severe("Error: " + e.getMessage());
                    return null;
                });
        }
        scanner.close();
    }

    private static void setupLogger() {
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.FINE);
        LOGGER.addHandler(handler);
        LOGGER.setLevel(Level.FINE);
    }

    private static void printWelcome(String apiKey) {
        System.out.println("🌍 Welcome to TripMate! ✈️");
        System.out.println("Ask me anything about travel. Type 'exit' or 'quit' to end.\n");
        String greeting = generateGreeting(apiKey);
        if (greeting != null) {
            System.out.println("TripMate: " + greeting);
        } else {
            System.out.println("TripMate: Ready to help you plan your next adventure!");
        }
    }

    private static String generateGreeting(String apiKey) {
        if (greetingCache.containsKey("greeting")) {
            return greetingCache.get("greeting");
        }
        // Simulate API call or use real API here
        String greetingText = "Ready to help you plan your next adventure!";
        greetingCache.put("greeting", greetingText);
        return greetingText;
    }

    private static String processUserInput(String userInput, String apiKey) {
        try {
            // Simulate API call, or use real API here
            if (userInput.toLowerCase().contains("hello") || userInput.toLowerCase().contains("hi")) {
                return "Hello! How can I assist you with your travel plans today?";
            } else if (userInput.toLowerCase().contains("recommend") || userInput.toLowerCase().contains("suggest")) {
                return "I recommend visiting Paris for its art and cuisine, or Bali for beaches and culture!";
            } else if (userInput.toLowerCase().contains("weather")) {
                return "For weather updates, I suggest checking a reliable weather app. Would you like recommendations?";
            } else {
                return "I'm here to help with travel! Try asking about destinations, flights, or hotels.";
            }
            // For real API usage, uncomment and adapt:
            /*
            String apiUrl = "https://api.example.com/travel?query=" + userInput;
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + apiKey)
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
            */
        } catch (Exception e) {
            LOGGER.severe("Error processing input: " + e.getMessage());
            return "Sorry, I couldn't process your request. Please try again.";
        }
    }

    private static void showSpinner() {
        Thread spinnerThread = new Thread(() -> {
            int i = 0;
            while (!Thread.currentThread().isInterrupted()) {
                System.out.print("\rTripMate: " + SPINNER[i % SPINNER.length] + " Thinking...");
                i++;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        spinnerThread.setDaemon(true);
        spinnerThread.start();
        try {
            Thread.sleep(1000); // Let spinner show for a moment
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        spinnerThread.interrupt();
    }

    private static void clearSpinner() {
        System.out.print("\rTripMate: " + " ".repeat(20) + "\r"); // Clear spinner line
    }

    private static final String[] SPINNER = new String[] { "⣾", "⣽", "⣻", "⢿", "⡿", "⣟", "⣯", "⣷" };
}
