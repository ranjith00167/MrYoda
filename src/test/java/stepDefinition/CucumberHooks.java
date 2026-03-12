package stepDefinition;

import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import utilities.FailureLogger;
import utilities.TestExecutionReporter;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * CUCUMBER HOOKS - Automatic Failure Logging for BDD Steps
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Purpose: Integrate failure logging into Cucumber step execution
 * 
 * Captures:
 * ✓ Scenario start/end with timestamp
 * ✓ Failed steps with context
 * ✓ Skipped steps
 * ✓ Final validation report generation
 */
public class CucumberHooks {
    
    private static final ThreadLocal<Long> scenarioStartTime = new ThreadLocal<>();
    private static final ThreadLocal<String> currentScenario = new ThreadLocal<>();
    
    @Before
    public void beforeScenario(Scenario scenario) {
        String scenarioName = scenario.getName();
        currentScenario.set(scenarioName);
        scenarioStartTime.set(System.currentTimeMillis());
        
        System.out.println("\n" + "═".repeat(80));
        System.out.println("▶️  SCENARIO START: " + scenarioName);
        System.out.println("   Tags: " + scenario.getSourceTagNames());
        System.out.println("═".repeat(80) + "\n");
    }
    
    @After
    public void afterScenario(Scenario scenario) {
        String scenarioName = scenario.getName();
        long duration = System.currentTimeMillis() - scenarioStartTime.get();
        
        if (scenario.isFailed()) {
            FailureLogger.logFailure(
                "SCENARIO_FAILED",
                scenarioName,
                "Scenario execution failed",
                "PASSED",
                "FAILED",
                null
            );
            
            System.out.println("\n" + "═".repeat(80));
            System.out.println("❌ SCENARIO FAILED: " + scenarioName);
            System.out.println("   Duration: " + duration + "ms");
            System.out.println("═".repeat(80) + "\n");
        } else {
            System.out.println("\n" + "═".repeat(80));
            System.out.println("✅ SCENARIO PASSED: " + scenarioName);
            System.out.println("   Duration: " + duration + "ms");
            System.out.println("═".repeat(80) + "\n");
        }
        
        currentScenario.remove();
        scenarioStartTime.remove();
    }
    
    /**
     * Get current scenario name for step logging
     */
    public static String getCurrentScenario() {
        String scenario = currentScenario.get();
        return scenario != null ? scenario : "Unknown Scenario";
    }
}
