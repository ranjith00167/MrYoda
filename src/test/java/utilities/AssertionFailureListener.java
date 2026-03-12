package utilities;

import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * ASSERTION FAILURE LISTENER - Automatic Failure Logging
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Automatically captures failures from:
 * ✓ Assert.fail() calls
 * ✓ Assert.assertEquals() mismatches
 * ✓ Assert.assertTrue()/assertFalse() failures
 * ✓ Exceptions during test execution
 * ✓ Assertion errors
 * 
 * Implementation: Register in testng.xml via <listener>
 *   <listener class-name="utilities.AssertionFailureListener" />
 */
public class AssertionFailureListener extends TestListenerAdapter {
    
    @Override
    public void onTestFailure(ITestResult result) {
        String testClass = result.getTestClass().getName();
        String testMethod = result.getMethod().getMethodName();
        Throwable exception = result.getThrowable();
        
        String failureMessage = exception != null ? exception.getMessage() : "Unknown failure";
        String exceptionType = exception != null ? exception.getClass().getSimpleName() : "UNKNOWN";
        
        FailureLogger.logFailure(
            "TEST_FAILURE_" + exceptionType,
            testClass + "::" + testMethod,
            failureMessage,
            "TEST_PASSED",
            "TEST_FAILED",
            exception
        );
        
        super.onTestFailure(result);
    }
    
    @Override
    public void onTestSkipped(ITestResult result) {
        String testClass = result.getTestClass().getName();
        String testMethod = result.getMethod().getMethodName();
        Throwable exception = result.getThrowable();
        
        String skipReason = exception != null ? exception.getMessage() : "Test skipped";
        
        System.out.println("⊘ TEST SKIPPED: " + testClass + "::" + testMethod);
        System.out.println("   Reason: " + skipReason);
        
        super.onTestSkipped(result);
    }
    
    @Override
    public void onTestSuccess(ITestResult result) {
        String testClass = result.getTestClass().getName();
        String testMethod = result.getMethod().getMethodName();
        long duration = result.getEndMillis() - result.getStartMillis();
        
        System.out.println("✅ TEST PASSED: " + testClass + "::" + testMethod);
        System.out.println("   Duration: " + duration + "ms");
        
        super.onTestSuccess(result);
    }
}
