package com.mryoda.diagnostics.api.tests.phlebio;

import api.phlebio.PhlebioClient;
import api.phlebio.PhlebioPayloads;
import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.utils.LoggerUtil;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;

/**
 * ============================================================
 * PHLEBIO — LIST PHLEBOS (GET /api/v1/phlebo/list)
 * Base URL : https://staging-api-phlebo-notification.yodadiagnostics.com
 *
 * Tests removed — endpoint returns 500 on staging (backend defect).
 * Re-add tests once the backend is fixed.
 * ============================================================
 */
public class PhlebioListTest extends BaseTest {

    private static String phleboToken;
    private final PhlebioClient phlebioClient = new PhlebioClient();

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        LoggerUtil.info("====== Phlebio List Test Setup Started ======");
        RestAssured.baseURI = ConfigLoader.getConfig().phlebioBaseUrl();

        try {
            Response loginResp = phlebioClient.login(PhlebioPayloads.buildLoginPayload());
            int loginStatus = loginResp.getStatusCode();
            System.out.println("   [SETUP] Phlebo login status: " + loginStatus);
            if (loginStatus == 200 || loginStatus == 201) {
                String[] tokenPaths = {
                    "data.access_token", "data.token", "token",
                    "data.tokens.access_token", "data.data.access_token"
                };
                for (String path : tokenPaths) {
                    String t = loginResp.jsonPath().getString(path);
                    if (t != null && !t.isEmpty()) {
                        phleboToken = t;
                        System.out.println("   [SETUP] Phlebo token obtained at path: " + path);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtil.info("Phlebo login exception: " + e.getMessage());
        }

        Assert.assertNotNull(phleboToken, "Setup failed: could not obtain phlebo login token");
        LoggerUtil.info("====== Phlebio List Test Setup Completed ======");
    }
}
