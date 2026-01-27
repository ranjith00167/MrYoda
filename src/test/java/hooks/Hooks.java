package hooks;

import io.cucumber.java.*;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import com.aventstack.extentreports.*;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import utilities.BaseClass;
import utilities.ConfigReader;
import utilities.DriverFactory;
import utilities.ScenarioContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class Hooks {

    private static ExtentReports extent;
    private static Map<String, ExtentTest> featureMap = new HashMap<>();
    private static ThreadLocal<ExtentTest> scenarioNode = new ThreadLocal<>();

    public static ExtentTest getTest() {
        return scenarioNode.get();
    }

    @Before
    public void setUp(Scenario scenario) throws IOException {
        ScenarioContext.scenario = scenario;

        // ================================
        //  Extent Report Setup
        // ================================
        if (extent == null) {
            ExtentSparkReporter spark = new ExtentSparkReporter("target/extent-reports/extent-report.html");

            spark.config().setReportName("Mryoda Test Execution Report");
            spark.config().setDocumentTitle("Mryoda Automation Report");
            spark.config().setTimelineEnabled(true);
            spark.config().setOfflineMode(true);
            spark.config().setTheme(Theme.STANDARD);

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Environment", System.getProperty("env", "QA"));
            extent.setSystemInfo("Browser", System.getProperty("browser", "chrome"));
            extent.setSystemInfo("User", System.getProperty("user.name"));
        }

        // ================================
        //  WebDriver Setup
        // ================================
        String browser = System.getProperty("browser", "chrome");
        if (browser == null || browser.isEmpty() || browser.startsWith("$")) {
             browser = "chrome";
        }
        WebDriver driver = DriverFactory.getDriver(browser);
        BaseClass.driver = driver;

        Collection<String> tags = scenario.getSourceTagNames();

        // ================================
        //  SHEET NAME SELECTION ONLY
        // ================================
//        Map<String, String> tagToSheetKey = Map.of(
//            "@diagnostics", "excel.sheetName",
//            "@dnadecoder", "excel.sheetName_dna"
//        );
//
//        BaseClass.currentSheet = ConfigReader.get("excel.sheetName"); // default sheet
//
//        for (String tag : tags) {
//            String lowerTag = tag.toLowerCase();
//            if (tagToSheetKey.containsKey(lowerTag)) {
//                String configKey = tagToSheetKey.get(lowerTag);
//                BaseClass.currentSheet = ConfigReader.get(configKey);
//                System.out.println("📘 Sheet selected via tag " + tag + ": " + BaseClass.currentSheet);
//            }
//        }

        // ❗ IMPORTANT:
        // DO NOT LOAD EXCEL ROW HERE. 
        // Steps will call: BaseClass.loadExcelRow("3")
        // to get correct testData.

        // ================================
        //  EXTENT REPORT BLOCK
        // ================================
        String uri = scenario.getUri() != null ? scenario.getUri().toString() : "default.feature";
        String featureName = uri.substring(uri.lastIndexOf("/") + 1).replace(".feature", "");

        try {
            ExtentTest featureTest = featureMap.computeIfAbsent(featureName,
                    k -> extent.createTest("Feature: " + k));
            ExtentTest scenarioTest = featureTest.createNode("Scenario: " + scenario.getName());

            if (!tags.isEmpty()) {
                scenarioTest.assignCategory(tags.toArray(new String[0]));
            }

            scenarioTest.assignAuthor("AutomationTeam");
            scenarioNode.set(scenarioTest);
        } catch (Exception e) {
            System.err.println("⚠️ Extent initialization failed: " + e.getMessage());
        }
    }

    @AfterStep
    public void afterStep(Scenario scenario) {
        if (getTest() != null) {
            if (scenario.isFailed()) {
                getTest().log(Status.FAIL, "🔴 Step failed.");
            } else {
                getTest().log(Status.PASS, "🟢 Step passed.");
            }
        }
    }

    @After
    public void tearDown(Scenario scenario) {
        try {
            WebDriver driver = BaseClass.driver;

            if (driver != null) {
                TakesScreenshot ts = (TakesScreenshot) driver;
                byte[] screenshot = ts.getScreenshotAs(OutputType.BYTES);
                String base64 = ts.getScreenshotAs(OutputType.BASE64);

                File screenshotDir = new File("target/extent-reports/screenshots");
                if (!screenshotDir.exists()) screenshotDir.mkdirs();

                String fileName = scenario.getName().replaceAll("[^a-zA-Z0-9.-]", "_") 
                        + "_" + UUID.randomUUID() + ".png";

                try (FileOutputStream out = new FileOutputStream(new File(screenshotDir, fileName))) {
                    out.write(screenshot);
                }

                if (getTest() != null) {
                    getTest().addScreenCaptureFromBase64String(base64, "📸 Screenshot");
                }
                scenario.attach(screenshot, "image/png", "Screenshot");
            }

            if (getTest() != null) {
                if (scenario.isFailed()) {
                    getTest().log(Status.FAIL, "❌ Scenario Failed");
                } else {
                    getTest().log(Status.PASS, "✅ Scenario Passed");
                }
            }

        } catch (Exception e) {
            System.err.println("⚠️ tearDown error: " + e.getMessage());

        } finally {
            if (!scenario.getSourceTagNames().contains("@keepBrowserOpen")) {
                DriverFactory.quitDriver();
            }
            scenarioNode.remove();
        }
    }

    @AfterAll
    public static void afterAll() {
        if (extent != null) {
            extent.flush();
        }
        DriverFactory.quitDriver();
    }
}
