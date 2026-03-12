package utilities;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * COMPREHENSIVE FAILURE LOGGING SYSTEM
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Purpose: Capture ALL test failures with complete context for validation
 * 
 * Features:
 * ✓ Centralized failure logging from any test step
 * ✓ Enriched failure context (timestamp, scenario, step, error details)
 * ✓ In-memory failure registry for reporting
 * ✓ File-based logging for persistent records
 * ✓ HTML validation report generation
 * ✓ Summary statistics (pass/fail counts, categories)
 */
public class FailureLogger {
    
    private static final List<FailureRecord> failures = new ArrayList<>();
    private static final String LOGS_DIR = "logs/failures";
    private static final String REPORT_FILE = "logs/VALIDATION_FAILURES_REPORT.html";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    static {
        new File(LOGS_DIR).mkdirs();
    }
    
    /**
     * Log a failure with detailed context
     */
    public static void logFailure(String category, String step, String message, String expectedValue, String actualValue, Throwable exception) {
        FailureRecord record = new FailureRecord(
            LocalDateTime.now(),
            category,
            step,
            message,
            expectedValue,
            actualValue,
            exception != null ? exception.toString() : "N/A",
            Thread.currentThread().getStackTrace()
        );
        
        failures.add(record);
        logToFile(record);
        
        System.out.println("\n" + "═".repeat(80));
        System.out.println("❌ FAILURE LOGGED: " + category);
        System.out.println("├─ Step: " + step);
        System.out.println("├─ Message: " + message);
        System.out.println("├─ Expected: " + expectedValue);
        System.out.println("├─ Actual: " + actualValue);
        System.out.println("├─ Timestamp: " + record.timestamp);
        System.out.println("└─ Thread: " + Thread.currentThread().getName());
        System.out.println("═".repeat(80) + "\n");
    }
    
    /**
     * Log a simple failure (convenience method)
     */
    public static void logFailure(String category, String step, String message) {
        logFailure(category, step, message, "", "", null);
    }
    
    /**
     * Log assertion failure
     */
    public static void logAssertionFailure(String step, String assertion, String expected, String actual) {
        logFailure("ASSERTION_FAILURE", step, "Assertion failed: " + assertion, expected, actual, null);
    }
    
    /**
     * Log validation failure
     */
    public static void logValidationFailure(String validationType, String step, String reason, String expected, String actual) {
        logFailure("VALIDATION_FAILURE_" + validationType.toUpperCase(), step, reason, expected, actual, null);
    }
    
    /**
     * Log calculation mismatch
     */
    public static void logCalculationMismatch(String step, String calculationType, double expected, double actual) {
        logFailure("CALCULATION_MISMATCH_" + calculationType.toUpperCase(), step,
            "Calculation mismatch in " + calculationType,
            "₹" + expected,
            "₹" + actual,
            null
        );
    }
    
    /**
     * Log API error
     */
    public static void logApiError(String endpoint, int statusCode, String response) {
        logFailure("API_ERROR", endpoint, "API returned error", "2xx Status", statusCode + "", null);
    }
    
    /**
     * Log element not found
     */
    public static void logElementNotFound(String step, String elementDescription) {
        logFailure("ELEMENT_NOT_FOUND", step, "Required element not found: " + elementDescription, "VISIBLE", "NOT_VISIBLE", null);
    }
    
    /**
     * Log timeout
     */
    public static void logTimeout(String step, int timeoutSeconds) {
        logFailure("TIMEOUT", step, "Operation timed out after " + timeoutSeconds + " seconds", "COMPLETED", "TIMEOUT", null);
    }
    
    /**
     * Get all failures recorded in session
     */
    public static List<FailureRecord> getAllFailures() {
        return new ArrayList<>(failures);
    }
    
    /**
     * Get failures by category
     */
    public static List<FailureRecord> getFailuresByCategory(String category) {
        List<FailureRecord> result = new ArrayList<>();
        for (FailureRecord r : failures) {
            if (r.category.contains(category)) {
                result.add(r);
            }
        }
        return result;
    }
    
    /**
     * Get failure count
     */
    public static int getFailureCount() {
        return failures.size();
    }
    
    /**
     * Clear all failures
     */
    public static void clearFailures() {
        failures.clear();
    }
    
    /**
     * Generate HTML validation report
     */
    public static void generateValidationReport() {
        try {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html>\n");
            html.append("<head>\n");
            html.append("<title>MrYoda - Validation Failures Report</title>\n");
            html.append("<style>\n");
            html.append("body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n");
            html.append("h1 { color: #d32f2f; border-bottom: 3px solid #d32f2f; padding-bottom: 10px; }\n");
            html.append(".summary { background-color: #fff3cd; padding: 15px; border-left: 4px solid #ffc107; margin: 20px 0; }\n");
            html.append(".failure { background-color: #ffebee; padding: 15px; margin: 10px 0; border-left: 4px solid #d32f2f; border-radius: 4px; }\n");
            html.append(".category { color: #d32f2f; font-weight: bold; font-size: 14px; }\n");
            html.append(".step { color: #1976d2; font-weight: bold; margin: 5px 0; }\n");
            html.append(".message { color: #333; margin: 5px 0; }\n");
            html.append(".details { background-color: #f5f5f5; padding: 10px; margin: 10px 0; border-radius: 3px; font-family: monospace; font-size: 12px; }\n");
            html.append(".timestamp { color: #666; font-size: 12px; }\n");
            html.append(".pass { color: #4caf50; }\n");
            html.append(".fail { color: #d32f2f; }\n");
            html.append("table { width: 100%; border-collapse: collapse; margin: 20px 0; }\n");
            html.append("th, td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }\n");
            html.append("th { background-color: #d32f2f; color: white; }\n");
            html.append("</style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("<h1>🔍 MrYoda Validation Failures Report</h1>\n");
            
            // Summary section
            html.append("<div class=\"summary\">\n");
            html.append("<h2>📊 Summary</h2>\n");
            html.append("<p><strong>Total Failures:</strong> <span class=\"fail\">").append(failures.size()).append("</span></p>\n");
            html.append("<p><strong>Report Generated:</strong> ").append(LocalDateTime.now().format(TIMESTAMP_FORMAT)).append("</p>\n");
            html.append("</div>\n");
            
            // Category breakdown
            Map<String, Integer> categoryCount = new HashMap<>();
            for (FailureRecord f : failures) {
                categoryCount.put(f.category, categoryCount.getOrDefault(f.category, 0) + 1);
            }
            
            if (!categoryCount.isEmpty()) {
                html.append("<h2>📈 Failure Categories</h2>\n");
                html.append("<table>\n");
                html.append("<tr><th>Category</th><th>Count</th></tr>\n");
                for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
                    html.append("<tr><td>").append(entry.getKey()).append("</td><td><span class=\"fail\">").append(entry.getValue()).append("</span></td></tr>\n");
                }
                html.append("</table>\n");
            }
            
            // Detailed failures
            html.append("<h2>❌ Detailed Failures</h2>\n");
            for (FailureRecord record : failures) {
                html.append("<div class=\"failure\">\n");
                html.append("<div class=\"category\">[").append(record.category).append("]</div>\n");
                html.append("<div class=\"step\">Step: ").append(record.step).append("</div>\n");
                html.append("<div class=\"message\">Message: ").append(record.message).append("</div>\n");
                html.append("<div class=\"details\">\n");
                html.append("Expected: <strong>").append(record.expectedValue).append("</strong><br/>\n");
                html.append("Actual: <strong>").append(record.actualValue).append("</strong><br/>\n");
                if (!record.exceptionDetails.equals("N/A")) {
                    html.append("Exception: ").append(record.exceptionDetails).append("<br/>\n");
                }
                html.append("Timestamp: ").append(record.timestamp.format(TIMESTAMP_FORMAT)).append("<br/>\n");
                html.append("</div>\n");
                html.append("</div>\n");
            }
            
            html.append("</body>\n");
            html.append("</html>\n");
            
            // Write to file
            try (FileWriter fw = new FileWriter(REPORT_FILE)) {
                fw.write(html.toString());
                System.out.println("\n✅ Validation Report Generated: " + REPORT_FILE);
            }
            
        } catch (IOException e) {
            System.err.println("❌ Failed to generate validation report: " + e.getMessage());
        }
    }
    
    /**
     * Log failure to file
     */
    private static void logToFile(FailureRecord record) {
        try {
            String filename = LOGS_DIR + "/failures_" + record.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".log";
            try (FileWriter fw = new FileWriter(filename, true);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                
                bw.write("\n" + "═".repeat(80) + "\n");
                bw.write("[" + record.timestamp.format(TIMESTAMP_FORMAT) + "] " + record.category + "\n");
                bw.write("Step: " + record.step + "\n");
                bw.write("Message: " + record.message + "\n");
                bw.write("Expected: " + record.expectedValue + "\n");
                bw.write("Actual: " + record.actualValue + "\n");
                if (!record.exceptionDetails.equals("N/A")) {
                    bw.write("Exception: " + record.exceptionDetails + "\n");
                }
                bw.write("Thread: " + record.thread + "\n");
                bw.flush();
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to log failure to file: " + e.getMessage());
        }
    }
    
    /**
     * Internal class to represent a failure record
     */
    public static class FailureRecord {
        public LocalDateTime timestamp;
        public String category;
        public String step;
        public String message;
        public String expectedValue;
        public String actualValue;
        public String exceptionDetails;
        public String thread;
        
        public FailureRecord(LocalDateTime timestamp, String category, String step, String message,
                           String expectedValue, String actualValue, String exceptionDetails,
                           StackTraceElement[] stackTrace) {
            this.timestamp = timestamp;
            this.category = category;
            this.step = step;
            this.message = message;
            this.expectedValue = expectedValue;
            this.actualValue = actualValue;
            this.exceptionDetails = exceptionDetails;
            this.thread = Thread.currentThread().getName();
        }
    }
}
