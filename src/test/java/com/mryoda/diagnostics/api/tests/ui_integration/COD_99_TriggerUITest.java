package com.mryoda.diagnostics.api.tests.ui_integration;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.TestNG;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.tests.order.COD_16_VisitStatusAPITest;
import com.mryoda.diagnostics.api.tests.order.COD_17_ReportGenerationTest;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import io.restassured.RestAssured;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        // Filter out visit numbers belonging to cancelled orders
        // Primary filter: directly stored cancelled visit numbers (most reliable)
        Set<String> cancelledVisits = RequestContext.getCancelledVisitNumbers();
        // Secondary filter: resolve via orderVisitMap for any order IDs not yet mapped to a visit
        Set<String> cancelledOrders = RequestContext.getCancelledOrderIds();
        if (cancelledOrders != null && !cancelledOrders.isEmpty()) {
            Map<String, String> ovm = RequestContext.getOrderVisitMap();
            if (ovm != null) {
                for (String cancelledOrderId : cancelledOrders) {
                    String v = ovm.get(cancelledOrderId);
                    if (v != null) {
                        if (cancelledVisits == null) cancelledVisits = new java.util.HashSet<>();
                        cancelledVisits.add(v);
                    }
                }
            }
        }
        if (cancelledVisits != null && !cancelledVisits.isEmpty()) {
            visits = new ArrayList<>(visits); // make mutable copy
            for (String cv : cancelledVisits) {
                if (visits.remove(cv)) {
                    System.out.println("   ⚠️ Excluded CANCELLED visit from UI trigger: " + cv);
                }
            }
            if (visits.isEmpty()) {
                System.out.println("   ℹ️ All visits were cancelled — skipping UI automation entirely.");
                return;
            }
        }

        System.out.println("📊 Found " + visits.size() + " visits to process (UI → COD_16 → COD_17 per visit).");

        // Keep a full copy — restored after the loop so downstream context is correct.
        List<String> allVisits = new ArrayList<>(visits);

        for (String visit : allVisits) {

            System.out.println("\n" + "=".repeat(60));
            System.out.println("🚀 PROCESSING VISIT: " + visit);
            System.out.println("=".repeat(60));

            // ── Step A: Scope RequestContext to this single visit ─────────────
            // Use a mutable list so setVisitNumber()'s internal add() won't throw
            // UnsupportedOperationException on the second+ iterations of the loop.
            List<String> singleVisitList = new ArrayList<>();
            singleVisitList.add(visit);
            RequestContext.setCurrentVisitNumbers(singleVisitList);
            RequestContext.setVisitNumber(visit);

            // ── Step B: Run @ITDose UI automation (Cucumber runner) ──────────
            // CRITICAL: Set the tag filter to @ITDose so the inner runner picks up the
            // correct scenario from 01_COD_Flow.feature.
            String originalTags = System.getProperty("cucumber.filter.tags");
            System.setProperty("cucumber.filter.tags", "@ITDose");

            TestNG testng = new TestNG();
            testng.setTestClasses(new Class[] { testRunner.RunnerTest.class });
            long start = System.currentTimeMillis();
            testng.run();
            long end = System.currentTimeMillis();

            if (originalTags != null) {
                System.setProperty("cucumber.filter.tags", originalTags);
            } else {
                System.clearProperty("cucumber.filter.tags");
            }

            System.out.println("   -> Visit " + visit + " UI Execution Time: " + (end - start) + "ms");

            if (testng.hasFailure()) {
                System.out.println("   ❌ UI Automation FAILED for visit: " + visit);
                throw new RuntimeException("UI Automation failed for visit: " + visit);
            }
            System.out.println("   ✅ UI Automation PASSED for visit: " + visit);

            // ── Step C: COD_16 — Visit Status check for this visit ───────────
            System.out.println("\n   📡 [COD_16] Visit Status check for visit: " + visit);
            try {
                RestAssured.baseURI = ConfigLoader.getConfig().baseUrl();
                new COD_16_VisitStatusAPITest().testGetVisitStatus();
                System.out.println("   ✅ COD_16 passed for visit: " + visit);
            } catch (Throwable ex) {
                System.out.println("   ❌ COD_16 FAILED for visit " + visit + ": " + ex.getMessage());
                throw new RuntimeException("COD_16 failed for visit " + visit, ex);
            }

            // ── Step D: COD_17 — Report Generation check for this visit ─────
            System.out.println("\n   📄 [COD_17] Report Generation check for visit: " + visit);
            try {
                new COD_17_ReportGenerationTest().testGetReportAndVerifyPDF();
                System.out.println("   ✅ COD_17 passed for visit: " + visit);
            } catch (Throwable ex) {
                System.out.println("   ❌ COD_17 FAILED for visit " + visit + ": " + ex.getMessage());
                throw new RuntimeException("COD_17 failed for visit " + visit, ex);
            }
        }

        // Restore the full visit list and mark all visits as processed.
        // COD_16 and COD_17 declared in the XML suite after COD_99 will see this flag
        // and skip execution (avoids double processing).
        RequestContext.setCurrentVisitNumbers(allVisits);
        RequestContext.setVisitsProcessedByUI(true);

        System.out.println("\n✅ ALL VISITS PROCESSED (UI → COD_16 → COD_17) FOR EACH VISIT SUCCESSFULLY!");
        Thread.sleep(8000);
    }
}