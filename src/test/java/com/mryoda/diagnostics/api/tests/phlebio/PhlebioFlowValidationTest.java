package com.mryoda.diagnostics.api.tests.phlebio;

import api.phlebio.PhlebioValidationReport;
import api.phlebio.ValidationLog;
import org.testng.Assert;
import org.testng.ITest;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.List;

/**
 * ============================================================
 * PHLEBIO FLOW -- INDIVIDUAL VALIDATIONS
 * ============================================================
 * Replays every field / cross-API check recorded while {@link PhlebioManagementTest} ran
 * (PHLEBO_MGMT_01..12) as its own test, so each validation appears as a separate result:
 *
 *   PASS    - check held
 *   FAIL    - check failed (wrong value / status / missing field)
 *   SKIPPED - backend defect found (inconsistent / null data). Fails only with -Dphlebo.failOnDefects=true
 *
 * Test name = "[step] check". The detail is in the test parameters and the failure / skip message.
 * Runs after the flow, even when a flow step failed or was skipped.
 * After the last check, a Jira-ready Markdown log (endpoint, expected, actual, test data per issue) is written to
 * docs/validation-logs/Phlebo_Validation_Log_<timestamp>.md (and ..._latest.md).
 * ============================================================
 */
public class PhlebioFlowValidationTest implements ITest {

    private static final String LAST_FLOW_STEP = "com.mryoda.diagnostics.api.tests.phlebio.PhlebioManagementTest"
            + ".PHLEBO_MGMT_12_UpdateOrderTrackingSamplesCollected";
    private static final String NO_CHECKS = "NONE";

    private final ThreadLocal<String> testName = new ThreadLocal<>();

    /** Evaluated lazily, i.e. after the flow steps have recorded their checks. */
    @DataProvider(name = "validations")
    public Object[][] validations() {
        List<ValidationLog.Entry> entries = ValidationLog.snapshot();
        if (entries.isEmpty()) {
            return new Object[][] {{"PHLEBO FLOW", "no validations recorded", NO_CHECKS,
                    "The phlebo flow did not run any validation (flow skipped or failed before the first step)."}};
        }
        Object[][] rows = new Object[entries.size()][];
        for (int i = 0; i < rows.length; i++) {
            ValidationLog.Entry e = entries.get(i);
            rows[i] = new Object[] {e.scope(), e.check(), e.status().name(), e.detail()};
        }
        return rows;
    }

    @BeforeMethod(alwaysRun = true)
    public void nameTest(Method method, Object[] args) {
        testName.set(args != null && args.length >= 2 ? "[" + args[0] + "] " + args[1] : method.getName());
    }

    @Override
    public String getTestName() {
        return testName.get();
    }

    /** Jira-ready log of every failed validation / backend defect: docs/validation-logs/. */
    @AfterClass(alwaysRun = true)
    public void writeValidationLog() {
        PhlebioValidationReport.write();
    }

    @Test(dataProvider = "validations",
          dependsOnMethods = LAST_FLOW_STEP,
          alwaysRun = true,
          description = "PHLEBO validation: one field / cross-API check recorded by the phlebo flow steps")
    public void PHLEBO_VALIDATION(String scope, String check, String status, String detail) {
        String message = "[" + scope + "] " + check + " -> " + detail;

        if (NO_CHECKS.equals(status)) {
            throw new SkipException(detail);
        }
        if (ValidationLog.Status.FAIL.name().equals(status)) {
            Assert.fail(message);
        }
        if (ValidationLog.Status.DEFECT.name().equals(status)) {
            if (Boolean.getBoolean("phlebo.failOnDefects")) {
                Assert.fail("[DEFECT] " + message);
            }
            throw new SkipException("BACKEND DEFECT " + message);
        }
        System.out.println("   [PASS] " + message);
    }
}
