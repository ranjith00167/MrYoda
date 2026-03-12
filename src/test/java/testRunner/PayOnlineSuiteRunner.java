package testRunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.ITestContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 *  PAY ONLINE HYBRID SUITE RUNNER
 * ══════════════════════════════════════════════════════════════════════════════
 *  Executes the complete end-to-end Pay Online flow:
 *
 *  PHASE 1 — UI Automation
 *    Login → Test selection → Cart → Checkout → Click Pay Online
 *
 *  PHASE 2 — API Automation
 *    Initiate Razorpay payment → Verify payment → Assert order status
 *
 *  Feature file : src/test/resources/Feature/TC_11_PayOnline_HybridFlow.feature
 *
 *  Run all scenarios  : tags = "@payOnlineHybrid"
 *  Run single member  : tags = "@hybridLabOrderMember or @hybridHomeOrderMember"
 *  Run non-member     : tags = "@hybridLabOrderNonMember or @hybridHomeOrderNonMember"
 * ══════════════════════════════════════════════════════════════════════════════
 */
@CucumberOptions(
    features = "classpath:Feature/TC_11_PayOnline_HybridFlow.feature",
    glue     = {"stepDefinition", "api", "hooks"},
    plugin   = {
        "pretty",
        "html:target/cucumber-reports/payonline-hybrid-report.html",
        "json:target/cucumber-reports/payonline-hybrid.json"
    },
    monochrome = true,
    dryRun     = false,
    tags       = "@payOnlineHybrid"
)
public class PayOnlineSuiteRunner extends AbstractTestNGCucumberTests {

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

    /**
     * Override to allow parallel scenario execution when needed.
     * Set parallel = true on the @DataProvider below to enable parallel runs.
     */
    @Override
    @DataProvider(parallel = false)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
