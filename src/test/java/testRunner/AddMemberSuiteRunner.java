package testRunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.ITestContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 *  ADD MEMBER — PAY ONLINE SUITE RUNNER
 * ══════════════════════════════════════════════════════════════════════════════
 *  Executes multi-member Pay Online flows for Member, Non-Member, and New User.
 *
 *  PHASE 1 — UI Automation
 *    Login → Test selection → Cart → Checkout (multi-member) → Pay Online
 *
 *  PHASE 2 — API Automation
 *    getCartById verification → Razorpay payment → getOrderById assert
 *
 *  Feature file : src/test/resources/Feature/TC_03_AddMember.feature
 *
 *  Tags:
 *    Member  lab  : @add_member_multi_lab_order_membership
 *    Member  home : @add_member_multi_home_order_membership
 *    NonMem  lab  : @add_member_multi_lab_order_without_membership
 *    NonMem  home : @add_member_multi_home_order_without_membership
 *    NewUser lab  : @add_member_multi_lab_order_new_user
 *    NewUser home : @add_member_multi_home_order_new_user
 * ══════════════════════════════════════════════════════════════════════════════
 */
@CucumberOptions(
    features = "classpath:Feature/TC_03_AddMember.feature",
    glue     = {"stepDefinition", "api", "hooks"},
    plugin   = {
        "pretty",
        "html:target/cucumber-reports/addmember-payonline-report.html",
        "json:target/cucumber-reports/addmember-payonline.json"
    },
    monochrome = true,
    dryRun     = false,
    tags       = "@AddMemberCompleteflow"
)
public class AddMemberSuiteRunner extends AbstractTestNGCucumberTests {

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
