package com.mryoda.diagnostics.api.utils;

import io.restassured.response.Response;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogManager {

    private static final String LOG_DIR = "logs";

    private static String getPerformanceLogPath() {
        return LOG_DIR + "/" + RequestContext.getCurrentFlowName() + "_performance.log";
    }

    private static String getAPIDetailLogPath() {
        return LOG_DIR + "/" + RequestContext.getCurrentFlowName() + "_api_details.log";
    }

    static {
        File dir = new File(LOG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static void logPerformance(String method, String endpoint, long timeInMs) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        String status = timeInMs < 3000 ? "PASS" : "FAIL (SLA Violation)";
        String logEntry = String.format("[%s] %-6s | %-120s | %5d ms | Status: %s\n",
                timestamp, method, endpoint, timeInMs, status);

        appendToFile(getPerformanceLogPath(), logEntry);
    }

    public static void logAPIDetail(String method, String endpoint, Response response) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("Timestamp: ").append(timestamp).append("\n");
        sb.append("Method: ").append(method).append("\n");
        sb.append("Endpoint: ").append(endpoint).append("\n");
        sb.append("Status Code: ").append(response.getStatusCode()).append("\n");
        sb.append("Response Time: ").append(response.getTime()).append(" ms\n");
        sb.append("Response Body: \n").append(response.getBody().asString()).append("\n");
        sb.append("========================================\n\n");

        appendToFile(getAPIDetailLogPath(), sb.toString());
    }

    private static synchronized void appendToFile(String filePath, String content) {
        try (FileWriter fw = new FileWriter(filePath, true)) {
            fw.write(content);
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + filePath + " | Error: " + e.getMessage());
        }
    }
}
