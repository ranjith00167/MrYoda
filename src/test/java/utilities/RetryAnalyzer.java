package utilities;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * TestNG Retry Analyzer - Automatically retries failed tests up to a configurable max count.
 * 
 * Default: 2 retries (test runs up to 3 times total).
 * Override via system property: -Dretry.max=3
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private int retryCount = 0;
    private static final int DEFAULT_MAX_RETRY = 2;

    private int getMaxRetryCount() {
        String maxRetry = System.getProperty("retry.max");
        if (maxRetry != null) {
            try {
                return Integer.parseInt(maxRetry);
            } catch (NumberFormatException e) {
                return DEFAULT_MAX_RETRY;
            }
        }
        return DEFAULT_MAX_RETRY;
    }

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < getMaxRetryCount()) {
            retryCount++;
            System.out.println("⟳ RETRY [" + retryCount + "/" + getMaxRetryCount() + "] - "
                    + result.getTestClass().getName() + "." + result.getName());
            return true;
        }
        return false;
    }
}
