package utilities;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.aventstack.extentreports.reporter.configuration.ViewName;

public class ExtentManager {
    private static ExtentReports extent;

    public static ExtentReports getInstance() {
        if (extent == null) {
            createInstance("target/ExtentReport/MrYoda_Automation_Report.html");
        }
        return extent;
    }

    private static ExtentReports createInstance(String fileName) {
        ExtentSparkReporter htmlReporter = new ExtentSparkReporter(fileName);
        htmlReporter.config().setTheme(Theme.STANDARD);
        htmlReporter.config().setDocumentTitle("MrYoda API Automation - Test Results");
        htmlReporter.config().setReportName("MrYoda API Automation - Test Results");
        htmlReporter.config().setEncoding("utf-8");
        htmlReporter.config().setTimelineEnabled(true);
        htmlReporter.config().setOfflineMode(true);

        htmlReporter.viewConfigurer().viewOrder()
                .as(new ViewName[]{ViewName.DASHBOARD, ViewName.TEST, ViewName.CATEGORY, ViewName.EXCEPTION})
                .apply();

        extent = new ExtentReports();
        extent.attachReporter(htmlReporter);
        extent.setSystemInfo("OS", System.getProperty("os.name"));
        extent.setSystemInfo("Java Version", System.getProperty("java.version"));
        extent.setSystemInfo("User", System.getProperty("user.name"));
        extent.setSystemInfo("Environment", "Staging");
        extent.setSystemInfo("App", "MrYoda Diagnostics");
        extent.setSystemInfo("Base URL", "https://staging-api-diagnostics.yodaprojects.com");

        return extent;
    }
}
