package testRunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.ITestContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 *  NEW USER REGISTRATION — PAY ONLINE SUITE RUNNER
 * ══════════════════════════════════════════════════════════════════════════════
 *  Executes the complete New User end-to-end Pay Online flow:
 *
 *  PHASE 1 — UI Automation
 *    Create account (random mobile) → Profile registration → Add address
 *    → Test selection → Cart → Checkout → Click Pay Online
 *
 *  PHASE 2 — API Automation
 *    getCartById verification → Razorpay payment → getOrderById assert
 *
 *  Feature file : src/test/resources/Feature/TC_10_UserRegistration.feature
 *
 *  Run new user registration + order: tags = "@NewUserRegistrationNew"
 * ══════════════════════════════════════════════════════════════════════════════
 */
@CucumberOptions(
    features = "classpath:Feature/TC_10_UserRegistration.feature",
    glue     = {"stepDefinition", "api", "hooks"},
    plugin   = {
        "pretty",
        "html:target/cucumber-reports/new-user-registration-report.html",
        "json:target/cucumber-reports/new-user-registration.json"
    },
    monochrome = true,
    dryRun     = false,
    tags       = "@NewUserRegistrationNew"
)
public class NewUserRegistrationSuiteRunner extends AbstractTestNGCucumberTests {

    /**
     * Reads cucumber.filter.tags from the TestNG XML parameter and sets it as a
     * system property so Cucumber picks up the correct scenario-level tag filter.
     * Runs before @BeforeClass (AbstractTestNGCucumberTests.setUpClass).
     */
    @BeforeTest
    public void applyTagFilter(ITestContext context) {
        String tags = context.getCurrentXmlTest().getParameter("cucumber.filter.tags");
        if (tags != null && !tags.isEmpty()) {
            System.setProperty("cucumber.filter.tags", tags);
        }
    }

    @Override
    @DataProvider(parallel = false)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
