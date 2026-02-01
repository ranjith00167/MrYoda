package testRunner;

import org.junit.runner.RunWith;
import org.junit.runner.JUnitCore;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/Feature",
    glue = {"stepDefinition", "api", "hooks"},
    plugin = {
        "pretty",
        "html:target/cucumber-reports/cucumber-html-report.html",
        "json:target/cucumber-reports/cucumber.json",
        "utilities.ExtentReportListener" 
    },
    monochrome = true,
    dryRun = false,
    tags = "@ITDose"
)
public class RunnerTest {
    
    // Add this main method to run from Eclipse directly
    public static void main(String[] args) {
      JUnitCore.runClasses(RunnerTest.class);
    }
}