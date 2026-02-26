package com.mryoda.diagnostics.api.base;

import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import io.restassured.RestAssured;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.utils.LoggerUtil;

/**
 * Base Test Class - Parent class for all test classes
 * Contains setup and teardown methods
 */
public class BaseTest {
    protected static final String DEFAULT_LOCATION = ConfigLoader.getConfig().defaultLocationName();

    @org.testng.annotations.BeforeTest(alwaysRun = true)
    public void beforeTestFlowSetup() {
        System.out.println("\n#########################################################");
        System.out.println("  STARTING NEW XML TEST FLOW - RESETTING CONTEXT");
        System.out.println("#########################################################");
        RequestContext.clearFlowState();
    }

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        LoggerUtil.info("====== Test Setup Started ======");

        // Set Base URI from config
        RestAssured.baseURI = ConfigLoader.getConfig().baseUrl();
        LoggerUtil.info("Base URL: " + RestAssured.baseURI);

        // Enable logging if configured
        if (ConfigLoader.getConfig().enableLogging()) {
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        }

        LoggerUtil.info("====== Test Setup Completed ======");
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        RequestContext.printPerformanceSummary();
        LoggerUtil.info("Environment teardown completed");
    }
}
