package com.mryoda.diagnostics.api.tests.ui_integration;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.TestNG;
import com.mryoda.diagnostics.api.utils.RequestContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class COD_99_TriggerUITest {

    @BeforeClass
    public void ensureVisitNumbersForUI() {
        System.out.println("\n>>> SETUP: Ensure RequestContext has visit numbers for UI automation <<<");
        List<String> visits = RequestContext.getCurrentVisitNumbers();
        if (visits == null || visits.isEmpty()) {
            Map<String, String> orderVisit = RequestContext.getOrderVisitMap();
            if (orderVisit != null && !orderVisit.isEmpty()) {
                List<String> derived = new ArrayList<>(new java.util.HashSet<>(orderVisit.values()));
                RequestContext.setCurrentVisitNumbers(derived);
                System.out.println("   ✅ Populated visit numbers from Order→Visit map: " + derived);
                return;
            }
            String prop = System.getProperty("visitNumbers");
            if (prop != null && !prop.trim().isEmpty()) {
                List<String> fromProp = new ArrayList<>();
                for (String s : prop.split(",")) {
                    if (s != null && !s.trim().isEmpty())
                        fromProp.add(s.trim());
                }
                if (!fromProp.isEmpty()) {
                    RequestContext.setCurrentVisitNumbers(fromProp);
                    System.out.println("   ✅ Populated visit numbers from system property: " + fromProp);
                    return;
                }
            }
            System.out.println("   ⚠️ No visit numbers found in RequestContext - UI trigger may fail.");
        } else {
            System.out.println("   ✅ Visit numbers already present: " + visits);
        }
    }

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