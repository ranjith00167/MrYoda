package utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mryoda.diagnostics.api.utils.LoggerUtil;
import io.restassured.response.Response;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Captures and logs all API responses for later validation analysis
 * Purpose: Collect real API response structures to write proper validations
 */
public class APIResponseCapture {
    private static final String RESPONSE_LOG_DIR = "api-responses/";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final List<String> capturedResponses = new ArrayList<>();

    static {
        // Create responses directory if it doesn't exist
        try {
            java.nio.file.Files.createDirectories(
                java.nio.file.Paths.get(RESPONSE_LOG_DIR)
            );
        } catch (IOException e) {
            System.err.println("Error creating api-responses directory: " + e.getMessage());
        }
    }

    /**
     * Capture API response with endpoint details
     */
    public static void captureResponse(String endpoint, Response response) {
        try {
            Map<String, Object> capture = new HashMap<>();
            capture.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            capture.put("endpoint", endpoint);
            capture.put("httpStatus", response.getStatusCode());
            capture.put("contentType", response.getContentType());
            
            try {
                String body = response.getBody().asString();
                capture.put("responseBody", body);
                
                // Try to parse as JSON to show structure
                try {
                    Object json = mapper.readValue(body, Object.class);
                    capture.put("parsedStructure", json);
                } catch (Exception e) {
                    capture.put("parsedStructure", "Not JSON - " + body.substring(0, Math.min(100, body.length())));
                }
            } catch (Exception e) {
                capture.put("responseBody", "Error reading body: " + e.getMessage());
            }
            
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(capture);
            capturedResponses.add(json);
            
            // Also write individual response files
            writeResponseFile(endpoint, json);
            
        } catch (Exception e) {
            LoggerUtil.error("Error capturing response: " + e.getMessage());
        }
    }

    /**
     * Write response to individual file for inspection
     */
    private static void writeResponseFile(String endpoint, String responseJson) {
        try {
            String filename = RESPONSE_LOG_DIR + endpoint.replaceAll("[^a-zA-Z0-9]", "_") + "_" + 
                             System.currentTimeMillis() + ".json";
            FileWriter writer = new FileWriter(filename);
            writer.write(responseJson);
            writer.close();
            LoggerUtil.info("📝 Response captured: " + filename);
        } catch (IOException e) {
            LoggerUtil.error("Error writing response file: " + e.getMessage());
        }
    }

    /**
     * Generate summary report of all captured responses
     */
    public static void generateCaptureReport() {
        try {
            String reportPath = RESPONSE_LOG_DIR + "CAPTURE_REPORT_" + 
                               System.currentTimeMillis() + ".txt";
            FileWriter writer = new FileWriter(reportPath);
            
            writer.write("=".repeat(80) + "\n");
            writer.write("API RESPONSE CAPTURE REPORT\n");
            writer.write("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
            writer.write("Total Responses Captured: " + capturedResponses.size() + "\n");
            writer.write("=".repeat(80) + "\n\n");
            
            for (String response : capturedResponses) {
                writer.write(response);
                writer.write("\n" + "-".repeat(80) + "\n\n");
            }
            
            writer.close();
            LoggerUtil.info("📊 Response capture report: " + reportPath);
        } catch (IOException e) {
            LoggerUtil.error("Error generating report: " + e.getMessage());
        }
    }

    /**
     * Get captured responses for programmatic access
     */
    public static List<String> getCapturedResponses() {
        return new ArrayList<>(capturedResponses);
    }

    /**
     * Clear captured responses (useful for cleanup between test phases)
     */
    public static void clearCaptures() {
        capturedResponses.clear();
        LoggerUtil.info("✓ Captured responses cleared");
    }
}
