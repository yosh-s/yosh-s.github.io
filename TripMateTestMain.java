import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.IOException;
import java.util.logging.Level;

public class TripMateTestMain {
    private static final Logger LOGGER = Logger.getLogger(TripMateTestMain.class.getName());

    public static void main(String[] args) {
        // Set up logging
        try {
            FileHandler fileHandler = new FileHandler("tripmate_test.log");
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.SEVERE);
        } catch (IOException e) {
            System.out.println("Error setting up logging: " + e.getMessage());
            LOGGER.severe("Error setting up logging: " + e.getMessage());
            return;
        }

        // Check for API key (simulated, as in TripMate)
        String googleApiKey = "AIzaSyATdVI59TbqVsT0vL7qid6SNd0wghu7bHI";
        if (googleApiKey == null || googleApiKey.isEmpty()) {
            System.out.println("Error: GOOGLE_API_KEY not found in environment variables. Please set it and try again.");
            LOGGER.severe("GOOGLE_API_KEY not found in environment variables.");
            return;
        }

        System.out.println("🌍 TripMate Test Suite 🌍");

        // Test 1: printHelp method
        System.out.println("\n=== Testing printHelp ===");
        TripMate.printHelp();

        // Test 2: isTravelRelated method
        System.out.println("\n=== Testing isTravelRelated ===");
        testIsTravelRelated("What's the best time to visit Japan?", true);
        testIsTravelRelated("How's the weather in Paris?", true);
        testIsTravelRelated("What's the capital of France?", false);
        testIsTravelRelated("Can you book a flight to Rome?", true);
        testIsTravelRelated("How to code in Java?", false);

        // Test 3: handleResponse method
        System.out.println("\n=== Testing handleResponse ===");
        testHandleResponse("What's the best time to visit Sri lanka?");
        testHandleResponse("What are the top attractions in Sri lanka? what are things to do in there?");

        testHandleResponse("Suggest an itinerary for Paris");
        testHandleResponse("Non-travel question about math");
    }

    private static void testIsTravelRelated(String input, boolean expected) {
        try {
            boolean result = TripMate.isTravelRelated(input);
            System.out.printf("Input: %s\nExpected: %b, Got: %b\n", input, expected, result);
            if (result == expected) {
                System.out.println("Test PASSED");
            } else {
                System.out.println("Test FAILED");
                LOGGER.severe("isTravelRelated test failed for input: " + input);
            }
        } catch (Exception e) {
            System.out.println("Error testing isTravelRelated: " + e.getMessage());
            LOGGER.severe("Error testing isTravelRelated for input: " + input + ", Error: " + e.getMessage());
        }
    }

    private static void testHandleResponse(String input) {
        try {
            System.out.println("Input: " + input);
            System.out.print("Response: ");
            TripMate.handleResponse(input);
        } catch (Exception e) {
            System.out.println("Error testing handleResponse: " + e.getMessage());
            LOGGER.severe("Error testing handleResponse for input: " + input + ", Error: " + e.getMessage());
        }
    }
}