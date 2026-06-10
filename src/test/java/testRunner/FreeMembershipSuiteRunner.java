package testRunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.ITestContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 *  FREE MEMBERSHIP HYBRID SUITE RUNNER
 * ══════════════════════════════════════════════════════════════════════════════
 *  Executes the complete end-to-end Pay Online flow for free membership scenarios:
 *
 *  PHASE 1 — UI Automation
 *    Login → Test selection → Cart → Checkout → Click Pay Online
 *
 *  PHASE 2 — API Automation
 *    Initiate Razorpay payment → Verify payment → Assert order status
 *
 *  Feature file : src/test/resources/Feature/TC_16__FreeMembership.feature
 *
 *  Run all scenarios  : tags = "@payOnlineHybrid"
 *  Run single member  : tags = "@hybridLabOrderMember"
 *  Run non-member     : tags = "@hybridLabOrderNonMember"
 *  Run new user       : tags = "@hybridLabOrderNewUser"
 * ══════════════════════════════════════════════════════════════════════════════
 */
@CucumberOptions(
    features = "classpath:Feature/TC_16__FreeMembership.feature",
    glue     = {"stepDefinition", "api", "hooks"},
    plugin   = {
        "pretty",
        "html:target/cucumber-reports/free-membership-report.html",
        "json:target/cucumber-reports/free-membership.json"
    },
    monochrome = true,
    dryRun     = false,
    tags       = "@payOnlineHybrid"
)
public class FreeMembershipSuiteRunner extends AbstractTestNGCucumberTests {

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
