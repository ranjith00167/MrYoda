package utilities;

import com.aventstack.extentreports.*;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;

import java.util.*;

public class ExtentReportListener implements ConcurrentEventListener {

    private static ExtentReports extent;
    private static ThreadLocal<ExtentTest> featureTest = new ThreadLocal<>();
    private static ThreadLocal<ExtentTest> scenarioTest = new ThreadLocal<>();

    private int passed = 0;
    private int failed = 0;
    private int skipped = 0;
    private int totalSteps = 0;

    private List<String> scenarioSummaryList = new ArrayList<>();

    public ExtentReportListener() {
        // initializeExtentReports(); // Handled by ExtentManager
        extent = ExtentManager.getInstance();
    }
/*
    private void initializeExtentReports() {
        ExtentSparkReporter spark = new ExtentSparkReporter("target/ExtentReport/ExtentReport.html");
        spark.config().setTheme(Theme.STANDARD);
        spark.config().setDocumentTitle("Automation Test Report");
        spark.config().setReportName("EMR Flow Execution Report");
        spark.config().setTimelineEnabled(true);
        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("OS", System.getProperty("os.name"));
        extent.setSystemInfo("User", System.getProperty("user.name"));
    }
*/

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestSourceRead.class, this::onTestSourceRead);
        publisher.registerHandlerFor(TestCaseStarted.class, this::onTestCaseStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::onTestStepFinished);
        publisher.registerHandlerFor(TestCaseFinished.class, this::onTestCaseFinished);
        publisher.registerHandlerFor(TestRunFinished.class, this::onTestRunFinished);
    }

    private void onTestSourceRead(TestSourceRead event) {
        String featureName = event.getUri().getPath();
        featureName = featureName.substring(featureName.lastIndexOf("/") + 1).replace(".feature", "");
        ExtentTest feature = extent.createTest("📁 Feature: " + featureName);
        featureTest.set(feature);
    }

    private void onTestCaseStarted(TestCaseStarted event) {
        ExtentTest scenario = featureTest.get()
            .createNode("🧪 Scenario: " + event.getTestCase().getName());
        scenarioTest.set(scenario);
    }

    private void onTestStepFinished(TestStepFinished event) {
        if (event.getTestStep() instanceof PickleStepTestStep) {
            PickleStepTestStep step = (PickleStepTestStep) event.getTestStep();
            String stepText = step.getStep().getKeyword() + step.getStep().getText();
            Status status = getStatus(event.getResult().getStatus());
            totalSteps++;

            if (event.getResult().getError() != null) {
                scenarioTest.get().log(status, stepText)
                    .fail(event.getResult().getError());
            } else {
                scenarioTest.get().log(status, stepText);
            }
        }
    }

    private void onTestCaseFinished(TestCaseFinished event) {
        io.cucumber.plugin.event.Status status = event.getResult().getStatus();
        String scenarioName = event.getTestCase().getName();

        switch (status) {
            case PASSED:
                passed++;
                scenarioSummaryList.add("✅ " + scenarioName);
                break;
            case FAILED:
                failed++;
                scenarioSummaryList.add("❌ " + scenarioName);
                break;
            case SKIPPED:
            case PENDING:
            case UNDEFINED:
                skipped++;
                scenarioSummaryList.add("⚠️ " + scenarioName);
                break;
            default:
                break;
        }
    }

    private void onTestRunFinished(TestRunFinished event) {
        int total = passed + failed + skipped;

        ExtentTest summary = extent.createTest("📊 Final Test Execution Summary");
        summary.info("✅ Scenarios Passed: **" + passed + "**");
        summary.info("❌ Scenarios Failed: **" + failed + "**");
        summary.info("⚠️ Scenarios Skipped: **" + skipped + "**");
        summary.info("📋 Total Scenarios: **" + total + "**");
        summary.info("📌 Total Steps Executed: **" + totalSteps + "**");

        summary.info("---");

        summary.info("🧾 **Scenario-wise Status:**");

        for (String line : scenarioSummaryList) {
            summary.info("- " + line);
        }

        extent.flush();
    }

    private Status getStatus(io.cucumber.plugin.event.Status status) {
        switch (status) {
            case PASSED:
                return Status.PASS;
            case FAILED:
                return Status.FAIL;
            case SKIPPED:
            case PENDING:
            case UNDEFINED:
                return Status.SKIP;
            default:
                return Status.INFO;
        }
    }
}
