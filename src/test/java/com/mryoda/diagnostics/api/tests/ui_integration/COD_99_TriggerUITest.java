package com.mryoda.diagnostics.api.tests.ui_integration;

import org.testng.annotations.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import testRunner.RunnerTest;

public class COD_99_TriggerUITest {

    @Test
    public void triggerUIAutomation() {
        System.out.println("\n=======================================================");
        System.out.println(">>> TRIGGERING UI AUTOMATION (Cucumber JUnit Runner) <<<");
        System.out.println("=======================================================");

        java.util.List<String> visits = com.mryoda.diagnostics.api.utils.RequestContext.getCurrentVisitNumbers();
        if (visits == null || visits.isEmpty()) {
            String v = com.mryoda.diagnostics.api.utils.RequestContext.getVisitNumber();
            if (v != null) {
                visits = new java.util.ArrayList<>();
                visits.add(v);
            }
        }

        if (visits == null || visits.isEmpty()) {
            throw new RuntimeException("❌ No Visit Numbers found in RequestContext for UI Automation!");
        }

        System.out.println("📊 Found " + visits.size() + " visits to process in IT Dose.");

        boolean anyFailure = false;
        StringBuilder failureTrace = new StringBuilder();

        for (String visit : visits) {
            System.out.println("\n🚀 RUNNING UI AUTOMATION FOR VISIT: " + visit);
            com.mryoda.diagnostics.api.utils.RequestContext.setVisitNumber(visit);

            // This will run the Cucumber Runner within the same JVM, preserving
            // RequestContext
            Result result = JUnitCore.runClasses(RunnerTest.class);

            System.out.println("   -> Visit " + visit + " Execution Time: " + result.getRunTime() + "ms");

            if (!result.wasSuccessful()) {
                anyFailure = true;
                for (Failure failure : result.getFailures()) {
                    failureTrace.append("\n[Visit: ").append(visit).append("] ").append(failure.toString());
                }
                System.out.println("   ❌ UI Automation FAILED for visit: " + visit);
            } else {
                System.out.println("   ✅ UI Automation PASSED for visit: " + visit);
            }
        }

        if (anyFailure) {
            System.out.println("\n❌ UI Automation failed for one or more visits!");
            System.err.println(failureTrace.toString());
            throw new RuntimeException("UI Automation Failed for visits:" + failureTrace.toString());
        }

        System.out.println("\n✅ ALL UI AUTOMATION SESSIONS COMPLETED SUCCESSFULLY!");
    }
}
