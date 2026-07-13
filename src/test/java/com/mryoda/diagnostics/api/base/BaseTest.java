package com.mryoda.diagnostics.api.base;

import com.mryoda.diagnostics.api.utils.ApiReportContext;
import com.mryoda.diagnostics.api.utils.RequestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.utils.LoggerUtil;

/**
 * Base Test Class - Parent class for all test classes
 * Contains setup and teardown methods
 */
public class BaseTest {
    protected static final String DEFAULT_LOCATION = ConfigLoader.getConfig().defaultLocationName();

    private static boolean filterRegistered = false;

    @org.testng.annotations.BeforeSuite(alwaysRun = true)
    public void beforeTestFlowSetup() {
        System.out.println("\n#########################################################");
        System.out.println("  STARTING NEW XML TEST FLOW - RESETTING CONTEXT");
        System.out.println("#########################################################");
        RequestContext.clearFlowState();

        // Register global REST Assured filter to capture all API calls for reporting
        if (!filterRegistered) {
            RestAssured.filters(new Filter() {
                @Override
                public Response filter(FilterableRequestSpecification requestSpec,
                                       FilterableResponseSpecification responseSpec,
                                       FilterContext ctx) {
                    String method = requestSpec.getMethod();
                    String uri = requestSpec.getURI();
                    String reqBody = (requestSpec.getBody() != null) ? requestSpec.getBody().toString() : "";

                    long startTime = System.currentTimeMillis();
                    Response response = ctx.next(requestSpec, responseSpec);
                    long duration = System.currentTimeMillis() - startTime;

                    int expectedStatus = ApiReportContext.getAndResetExpectedStatus();
                    int[] expectedRange = ApiReportContext.getAndResetExpectedStatusRange();

                    ApiReportContext.addRecord(new ApiReportContext.ApiCallRecord(
                            method, uri, reqBody,
                            response.getStatusCode(), response.getBody().asString(),
                            duration, expectedStatus, expectedRange[0], expectedRange[1], ""));

                    return response;
                }
            });
            filterRegistered = true;
        }
    }

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        LoggerUtil.info("====== Test Setup Started ======");

        // Set Base URI from config
        RestAssured.baseURI = ConfigLoader.getConfig().baseUrl();
        LoggerUtil.info("Base URL: " + RestAssured.baseURI);

        // Set connection and socket timeouts (30s) to prevent hanging on cold starts
        RestAssured.config = RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", 30000)
                        .setParam("http.socket.timeout", 30000));

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
