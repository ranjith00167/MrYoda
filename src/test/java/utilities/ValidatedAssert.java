package utilities;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * VALIDATION UTILITIES - Convenient Assertion Methods with Logging
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Purpose: Drop-in replacement for Assert methods with automatic failure logging
 * 
 * Usage Examples:
 *   ValidatedAssert.assertEquals(actual, expected, "Step name", "Amount validation");
 *   ValidatedAssert.assertTrue(condition, "Step name", "Condition check");
 *   ValidatedAssert.fail("Step name", "Reason for failure");
 */
public class ValidatedAssert {
    
    /**
     * Assert equals with failure logging
     */
    public static void assertEquals(Object actual, Object expected, String step, String description) {
        if (!equals(actual, expected)) {
            String actualStr = actual == null ? "null" : actual.toString();
            String expectedStr = expected == null ? "null" : expected.toString();
            
            FailureLogger.logAssertionFailure(step, description, expectedStr, actualStr);
            org.testng.Assert.assertEquals(actual, expected, "❌ " + description + " (step: " + step + ")");
        }
    }
    
    /**
     * Assert equals with delta (for doubles) and failure logging
     */
    public static void assertEquals(double actual, double expected, double delta, String step, String description) {
        if (Math.abs(actual - expected) > delta) {
            FailureLogger.logCalculationMismatch(step, description, expected, actual);
            org.testng.Assert.assertEquals(actual, expected, delta, "❌ " + description + " (step: " + step + ")");
        }
    }
    
    /**
     * Assert true with failure logging
     */
    public static void assertTrue(boolean condition, String step, String description) {
        if (!condition) {
            FailureLogger.logAssertionFailure(step, description, "true", "false");
            org.testng.Assert.assertTrue(false, "❌ " + description + " (step: " + step + ")");
        }
    }
    
    /**
     * Assert false with failure logging
     */
    public static void assertFalse(boolean condition, String step, String description) {
        if (condition) {
            FailureLogger.logAssertionFailure(step, description, "false", "true");
            org.testng.Assert.assertFalse(true, "❌ " + description + " (step: " + step + ")");
        }
    }
    
    /**
     * Assert not null with failure logging
     */
    public static void assertNotNull(Object object, String step, String description) {
        if (object == null) {
            FailureLogger.logValidationFailure("NULL_CHECK", step, description + " should not be null", "NOT_NULL", "NULL");
            org.testng.Assert.assertNotNull(object, "❌ " + description + " is null (step: " + step + ")");
        }
    }
    
    /**
     * Assert null with failure logging
     */
    public static void assertNull(Object object, String step, String description) {
        if (object != null) {
            FailureLogger.logValidationFailure("NULL_CHECK", step, description + " should be null", "NULL", object.toString());
            org.testng.Assert.assertNull(object, "❌ " + description + " is not null (step: " + step + ")");
        }
    }
    
    /**
     * Validation fail with logging
     */
    public static void fail(String step, String reason) {
        FailureLogger.logFailure("VALIDATION_FAIL", step, reason, "PASS", "FAIL", null);
        org.testng.Assert.fail("❌ " + reason + " (step: " + step + ")");
    }
    
    /**
     * Compare amounts with failure logging
     */
    public static void assertAmountEqual(double actual, double expected, String step, String description) {
        double delta = 0.1; // Allow ₹0.10 variance
        if (Math.abs(actual - expected) > delta) {
            FailureLogger.logCalculationMismatch(step, description, expected, actual);
            org.testng.Assert.assertEquals(actual, expected, delta, 
                "❌ Amount mismatch: " + description + " (step: " + step + ")");
        } else if (Math.abs(actual - expected) > 0) {
            System.out.println("⚠️ Minor amount variance ₹" + Math.abs(actual - expected) + " accepted (delta: ₹" + delta + ")");
        }
    }
    
    /**
     * Helper method for equals
     */
    private static boolean equals(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
