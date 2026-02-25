package com.mryoda.diagnostics.api.tests.ui_integration;

import org.testng.annotations.Test;
import org.testng.TestNG;
import com.mryoda.diagnostics.api.utils.RequestContext;

import java.util.ArrayList;
import java.util.List;

public class COD_99_TriggerUITest {

    @Test
    public void triggerUIAutomation() throws Throwable{

        System.out.println("\n=======================================================");
        System.out.println(">>> TRIGGERING UI AUTOMATION (Cucumber TestNG Runner) <<<");
        System.out.println("=======================================================");

        List<String> visits = RequestContext.getCurrentVisitNumbers();

        if (visits == null || visits.isEmpty()) {
            String v = RequestContext.getVisitNumber();
            if (v != null) {
                visits = new ArrayList<>();
                visits.add(v);
            }
        }

        if (visits == null || visits.isEmpty()) {
            throw new RuntimeException("❌ No Visit Numbers found in RequestContext for UI Automation!");
        }

        System.out.println("📊 Found " + visits.size() + " visits to process in IT Dose.");

        for (String visit : visits) {

            System.out.println("\n🚀 RUNNING UI AUTOMATION FOR VISIT: " + visit);
            RequestContext.setVisitNumber(visit);

            TestNG testng = new TestNG();
            testng.setTestClasses(new Class[] { testRunner.RunnerTest.class });

            long start = System.currentTimeMillis();
            testng.run();
            long end = System.currentTimeMillis();

            System.out.println("   -> Visit " + visit + " Execution Time: " + (end - start) + "ms");

            if (testng.hasFailure()) {
                System.out.println("   ❌ UI Automation FAILED for visit: " + visit);
                throw new RuntimeException("UI Automation failed for visit: " + visit);
            } else {
                System.out.println("   ✅ UI Automation PASSED for visit: " + visit);
            }
        }

        System.out.println("\n✅ ALL UI AUTOMATION SESSIONS COMPLETED SUCCESSFULLY!");
        Thread.sleep(8000);    }
}