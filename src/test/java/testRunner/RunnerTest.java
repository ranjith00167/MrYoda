package testRunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
    features = "classpath:Feature/01_COD_Flow.feature",
    glue = {"stepDefinition", "api", "hooks"},
    plugin = {
        "pretty",
        "html:target/cucumber-reports/cucumber-html-report.html",
        "json:target/cucumber-reports/cucumber.json"
    },
    monochrome = true
)
public class RunnerTest extends AbstractTestNGCucumberTests {
}
