import org.json.JSONArray;
import org.json.JSONObject;

public class GeminiPayload {
    public static void main(String[] args) {
        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();

        part.put("text", "Hello Gemini!");
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        requestBody.put("contents", contents);

        System.out.println(requestBody.toString(2)); // pretty print
    }
}
