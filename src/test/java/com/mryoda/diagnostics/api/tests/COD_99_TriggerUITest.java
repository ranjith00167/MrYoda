package com.mryoda.diagnostics.api.tests;

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
        
        // This will run the Cucumber Runner within the same JVM, preserving RequestContext
        Result result = JUnitCore.runClasses(RunnerTest.class);
        
        System.out.println("\n=======================================================");
        System.out.println("      UI AUTOMATION EXECUTION SUMMARY");
        System.out.println("=======================================================");
        System.out.println("Total Tests Run: " + result.getRunCount());
        System.out.println("Failed Tests: " + result.getFailureCount());
        System.out.println("Ignored Tests: " + result.getIgnoreCount());
        System.out.println("Execution Time: " + result.getRunTime() + "ms");
        
        if (!result.wasSuccessful()) {
             System.out.println("\n❌ UI Automation Failed with existing errors:");
             for (Failure failure : result.getFailures()) {
                 System.out.println(failure.toString());
                 System.out.println(failure.getTrace());
             }
             throw new RuntimeException("UI Automation Failed via JUnitCore Trigger");
        } else {
             System.out.println("✅ UI Automation Completed Successfully!");
        }
    }
}
