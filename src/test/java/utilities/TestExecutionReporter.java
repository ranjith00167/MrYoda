package utilities;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * TEST EXECUTION REPORT GENERATOR - Comprehensive Validation Report
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Purpose: Generate detailed HTML reports of test execution with:
 * ✓ Overall summary (pass/fail counts, execution time)
 * ✓ Failure breakdown by category and step
 * ✓ Detailed failure records with timestamps
 * ✓ Statistical analysis
 * ✓ Actionable recommendations for failures
 */
public class TestExecutionReporter {
    
    private static final String REPORT_DIR = "reports";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    static {
        new File(REPORT_DIR).mkdirs();
    }
    
    /**
     * Generate comprehensive execution report
     */
    public static void generateExecutionReport(String testSuiteName, int totalTests, int passedTests) {
        try {
            List<FailureLogger.FailureRecord> failures = FailureLogger.getAllFailures();
            int failedTests = failures.size();
            float passRate = totalTests > 0 ? (float) passedTests / totalTests * 100 : 0;
            
            String filename = REPORT_DIR + "/EXECUTION_REPORT_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".html";
            
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html>\n");
            html.append("<head>\n");
            html.append("<title>MrYoda - Test Execution Report</title>\n");
            html.append("<meta charset=\"UTF-8\">\n");
            html.append("<style>\n");
            html.append(getStylesheet());
            html.append("</style>\n");
            html.append("</head>\n");
            html.append("<body>\n");
            
            // Header
            html.append("<div class=\"header\">\n");
            html.append("<h1>🧪 MrYoda Test Execution Report</h1>\n");
            html.append("<p class=\"suite-name\">").append(testSuiteName).append("</p>\n");
            html.append("<p class=\"generated\">Generated: ").append(LocalDateTime.now().format(TIMESTAMP_FORMAT)).append("</p>\n");
            html.append("</div>\n");
            
            // Summary cards
            html.append("<div class=\"summary-cards\">\n");
            html.append("<div class=\"card total\">").append("<h3>Total Tests</h3>").append("<p class=\"value\">").append(totalTests).append("</p>").append("</div>\n");
            html.append("<div class=\"card pass\">").append("<h3>Passed</h3>").append("<p class=\"value\">").append(passedTests).append("</p>").append("</div>\n");
            html.append("<div class=\"card fail\">").append("<h3>Failed</h3>").append("<p class=\"value\">").append(failedTests).append("</p>").append("</div>\n");
            html.append("<div class=\"card rate\">").append("<h3>Pass Rate</h3>").append("<p class=\"value\">").append(String.format("%.1f%%", passRate)).append("</p>").append("</div>\n");
            html.append("</div>\n");
            
            // Failure analysis
            if (!failures.isEmpty()) {
                html.append("<h2>📊 Failure Analysis</h2>\n");
                html.append(generateFailureAnalysis(failures));
                html.append("<h2>❌ Detailed Failures</h2>\n");
                html.append(generateDetailedFailures(failures));
            } else {
                html.append("<div class=\"no-failures\">✅ All tests passed! No failures to report.</div>\n");
            }
            
            // Recommendations
            html.append(generateRecommendations(failures));
            
            html.append("</body>\n");
            html.append("</html>\n");
            
            // Write to file
            try (FileWriter fw = new FileWriter(filename)) {
                fw.write(html.toString());
                System.out.println("\n✅ Execution Report Generated: " + filename);
            }
            
        } catch (IOException e) {
            System.err.println("❌ Failed to generate execution report: " + e.getMessage());
        }
    }
    
    /**
     * Generate failure analysis section
     */
    private static String generateFailureAnalysis(List<FailureLogger.FailureRecord> failures) {
        Map<String, Integer> categoryCount = new HashMap<>();
        Map<String, List<String>> categoryDetails = new HashMap<>();
        
        for (FailureLogger.FailureRecord f : failures) {
            categoryCount.put(f.category, categoryCount.getOrDefault(f.category, 0) + 1);
            categoryDetails.computeIfAbsent(f.category, k -> new ArrayList<>()).add(f.message);
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<table class=\"analysis-table\">\n");
        html.append("<tr><th>Category</th><th>Count</th><th>Percentage</th></tr>\n");
        
        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            float pct = (float) entry.getValue() / failures.size() * 100;
            html.append("<tr>\n");
            html.append("<td>").append(entry.getKey()).append("</td>\n");
            html.append("<td><span class=\"fail-count\">").append(entry.getValue()).append("</span></td>\n");
            html.append("<td><span class=\"fail-pct\">").append(String.format("%.1f%%", pct)).append("</span></td>\n");
            html.append("</tr>\n");
        }
        
        html.append("</table>\n");
        return html.toString();
    }
    
    /**
     * Generate detailed failures section
     */
    private static String generateDetailedFailures(List<FailureLogger.FailureRecord> failures) {
        StringBuilder html = new StringBuilder();
        
        for (int i = 0; i < failures.size(); i++) {
            FailureLogger.FailureRecord f = failures.get(i);
            html.append("<div class=\"failure-item\">\n");
            html.append("<div class=\"failure-header\">\n");
            html.append("<span class=\"failure-number\">#").append(i + 1).append("</span>\n");
            html.append("<span class=\"failure-category\">").append(f.category).append("</span>\n");
            html.append("<span class=\"failure-time\">").append(f.timestamp.format(TIMESTAMP_FORMAT)).append("</span>\n");
            html.append("</div>\n");
            
            html.append("<div class=\"failure-details\">\n");
            html.append("<p><strong>Step:</strong> ").append(escapeHtml(f.step)).append("</p>\n");
            html.append("<p><strong>Message:</strong> ").append(escapeHtml(f.message)).append("</p>\n");
            html.append("<p><strong>Expected:</strong> <code>").append(escapeHtml(f.expectedValue)).append("</code></p>\n");
            html.append("<p><strong>Actual:</strong> <code>").append(escapeHtml(f.actualValue)).append("</code></p>\n");
            
            if (!f.exceptionDetails.equals("N/A")) {
                html.append("<p><strong>Exception:</strong></p>\n");
                html.append("<pre class=\"exception\">").append(escapeHtml(f.exceptionDetails)).append("</pre>\n");
            }
            
            html.append("</div>\n");
            html.append("</div>\n");
        }
        
        return html.toString();
    }
    
    /**
     * Generate recommendations section
     */
    private static String generateRecommendations(List<FailureLogger.FailureRecord> failures) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"recommendations\">\n");
        html.append("<h2>💡 Recommendations</h2>\n");
        
        if (failures.isEmpty()) {
            html.append("<p>✅ All tests passed successfully!</p>\n");
        } else {
            Map<String, Integer> categories = new HashMap<>();
            for (FailureLogger.FailureRecord f : failures) {
                categories.put(f.category, categories.getOrDefault(f.category, 0) + 1);
            }
            
            String topFailure = categories.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN");
            
            html.append("<ul>\n");
            html.append("<li>🔍 <strong>Top Failure Category:</strong> ").append(topFailure).append(" - Focus on fixing these issues.</li>\n");
            html.append("<li>📋 <strong>Total Failures:</strong> ").append(failures.size()).append(" - Review each failure in detailed list above.</li>\n");
            html.append("<li>🔧 <strong>Action Items:</strong> See failure details and implement fixes.</li>\n");
            html.append("<li>⚡ <strong>Re-run Tests:</strong> Execute again after fixes to verify resolution.</li>\n");
            html.append("</ul>\n");
        }
        
        html.append("</div>\n");
        return html.toString();
    }
    
    /**
     * Get CSS stylesheet
     */
    private static String getStylesheet() {
        return "* { margin: 0; padding: 0; box-sizing: border-box; }\n" +
               "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5; color: #333; line-height: 1.6; }\n" +
               ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 40px; text-align: center; border-radius: 8px; margin-bottom: 30px; }\n" +
               ".header h1 { font-size: 32px; margin-bottom: 10px; }\n" +
               ".suite-name { font-size: 18px; opacity: 0.9; margin-bottom: 5px; }\n" +
               ".generated { font-size: 12px; opacity: 0.8; }\n" +
               ".summary-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 30px; }\n" +
               ".card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); text-align: center; }\n" +
               ".card h3 { font-size: 14px; color: #666; margin-bottom: 10px; }\n" +
               ".card .value { font-size: 32px; font-weight: bold; }\n" +
               ".card.pass .value { color: #4caf50; }\n" +
               ".card.fail .value { color: #d32f2f; }\n" +
               ".card.total .value { color: #1976d2; }\n" +
               ".card.rate .value { color: #ff9800; }\n" +
               "h2 { color: #333; margin: 30px 0 20px 0; border-bottom: 2px solid #667eea; padding-bottom: 10px; }\n" +
               "table { width: 100%; border-collapse: collapse; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin-bottom: 20px; }\n" +
               "th { background: #667eea; color: white; padding: 12px; text-align: left; }\n" +
               "td { padding: 12px; border-bottom: 1px solid #eee; }\n" +
               "tr:hover { background-color: #f9f9f9; }\n" +
               ".fail-count { background: #ffebee; color: #d32f2f; padding: 4px 8px; border-radius: 4px; font-weight: bold; }\n" +
               ".fail-pct { background: #fff3e0; color: #f57c00; padding: 4px 8px; border-radius: 4px; }\n" +
               ".failure-item { background: white; border-left: 4px solid #d32f2f; padding: 15px; margin-bottom: 15px; border-radius: 4px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n" +
               ".failure-header { display: flex; gap: 15px; margin-bottom: 12px; align-items: center; flex-wrap: wrap; }\n" +
               ".failure-number { background: #d32f2f; color: white; padding: 4px 12px; border-radius: 20px; font-weight: bold; }\n" +
               ".failure-category { background: #e0e0e0; color: #333; padding: 4px 12px; border-radius: 4px; font-size: 12px; font-weight: bold; }\n" +
               ".failure-time { font-size: 12px; color: #999; }\n" +
               ".failure-details p { margin-bottom: 8px; }\n" +
               ".failure-details strong { color: #667eea; }\n" +
               "code, pre { background: #f5f5f5; border: 1px solid #ddd; padding: 8px; border-radius: 4px; font-family: 'Courier New', monospace; font-size: 12px; word-wrap: break-word; }\n" +
               ".exception { max-height: 200px; overflow-y: auto; }\n" +
               ".recommendations { background: #e8f5e9; border-left: 4px solid #4caf50; padding: 20px; border-radius: 4px; margin-top: 30px; }\n" +
               ".recommendations h2 { border-bottom: 2px solid #4caf50; margin-top: 0; }\n" +
               ".recommendations ul { margin-left: 20px; }\n" +
               ".recommendations li { margin-bottom: 10px; }\n" +
               ".no-failures { background: #e8f5e9; color: #2e7d32; padding: 20px; border-radius: 8px; text-align: center; font-size: 18px; margin: 20px 0; }";
    }
    
    /**
     * Escape HTML characters
     */
    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
