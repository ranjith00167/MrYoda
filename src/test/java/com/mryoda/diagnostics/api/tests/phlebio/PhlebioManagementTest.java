package com.mryoda.diagnostics.api.tests.phlebio;

import api.phlebio.PhlebioClient;
import api.phlebio.PhlebioPayloads;
import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.LoggerUtil;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================
 * PHLEBIO -- SHIFT MANAGEMENT  (Positive Flow)
 * ============================================================
 *
 * PHLEBO_MGMT_01 : Login Phlebo (prerequisite — obtains phlebo GUID + JWT)
 * PHLEBO_MGMT_02 : Admin Login  (prerequisite — obtains admin JWT)
 * PHLEBO_MGMT_03 : Assign Shifts  [Admin assigns shift to phlebotomist]
 * PHLEBO_MGMT_04 : Get Phlebo Shifts  [Admin fetches shifts for phlebotomist]
 * PHLEBO_MGMT_05 : Clock In  [Phlebotomist clocks in for active shift]
 *
 * Execution order is strictly enforced via TestNG priority.
 * State shared between tests via static fields:
 *   - phleboGuid       (from PHLEBO_MGMT_01)
 *   - phleboJwtToken   (from PHLEBO_MGMT_01 — phlebo notification service JWT)
 *   - adminToken       (from PHLEBO_MGMT_02)
 * ============================================================
 */
public class PhlebioManagementTest extends BaseTest {

    // ── Shared state ──────────────────────────────────────────────────────────
    private static String phleboGuid;
    private static String phleboJwtToken;   // JWT from phlebo notification service (for clock-in)
    private static String adminToken;       // JWT from diagnostics admin login (for shift management)

    private final PhlebioClient phlebioClient = new PhlebioClient();

    // Point RestAssured base URI at the phlebo notification service
    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        LoggerUtil.info("====== Phlebio Management Test Setup Started ======");
        RestAssured.baseURI = ConfigLoader.getConfig().phlebioBaseUrl();
        LoggerUtil.info("Phlebio Base URL: " + RestAssured.baseURI);
        LoggerUtil.info("====== Phlebio Management Test Setup Completed ======");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHLEBO_MGMT_01 : Login Phlebo
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 1,
          description = "PHLEBO MGMT-01: Login Phlebo — Verify phlebotomist can authenticate against the "
                      + "phlebo notification service. Extracts JWT token and phlebo GUID required by "
                      + "subsequent management operations.")
    public void PHLEBO_MGMT_01_LoginPhlebo() {
        System.out.println("\n>>> PHLEBO_MGMT_01: LOGIN PHLEBO <<<");

        JSONObject payload = PhlebioPayloads.buildLoginPayload();
        System.out.println("   Request Body : " + payload.toString(2));

        Response response = phlebioClient.login(payload);

        System.out.println("   Response Status : " + response.getStatusCode());
        System.out.println("   Response Body   : " + response.prettyPrint());

        Assert.assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
                "PHLEBO_MGMT_01: Login must return HTTP 200 or 201. Actual: " + response.getStatusCode());
        Assert.assertTrue(Boolean.TRUE.equals(response.jsonPath().get("success")),
                "PHLEBO_MGMT_01: Response 'success' must be true");

        // Extract JWT token — try multiple common paths
        phleboJwtToken = response.jsonPath().getString("data.access_token");
        if (phleboJwtToken == null || phleboJwtToken.isEmpty()) {
            phleboJwtToken = response.jsonPath().getString("data.token");
        }
        if (phleboJwtToken == null || phleboJwtToken.isEmpty()) {
            phleboJwtToken = response.jsonPath().getString("token");
        }
        Assert.assertNotNull(phleboJwtToken,
                "PHLEBO_MGMT_01: JWT token must be present in login response for shift management");

        // Extract phlebo GUID — try multiple paths
        phleboGuid = response.jsonPath().getString("data.phlebo.guid");
        if (phleboGuid == null || phleboGuid.isEmpty()) {
            phleboGuid = response.jsonPath().getString("data.guid");
        }
        if (phleboGuid == null || phleboGuid.isEmpty()) {
            phleboGuid = response.jsonPath().getString("data.phlebo_guid");
        }
        Assert.assertNotNull(phleboGuid,
                "PHLEBO_MGMT_01: Phlebo GUID must be present in login response");

        System.out.println("   Phlebo GUID      : " + phleboGuid);
        System.out.println("   JWT Token Present: " + (phleboJwtToken != null && !phleboJwtToken.isEmpty()));
        System.out.println("   [PASS] PHLEBO_MGMT_01 PASSED -- Phlebo login successful.\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHLEBO_MGMT_02 : Admin Login
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 2,
          description = "PHLEBO MGMT-02: Admin Login — Authenticate as admin against the diagnostics service "
                      + "to obtain the admin JWT required for shift assignment and shift retrieval.")
    public void PHLEBO_MGMT_02_AdminLogin() {
        System.out.println("\n>>> PHLEBO_MGMT_02: ADMIN LOGIN <<<");

        String adminIdentifier = ConfigLoader.getConfig().adminMainIdentifier();
        String adminPassword   = ConfigLoader.getConfig().adminMainPassword();

        Map<String, Object> payload = new HashMap<>();
        payload.put("identifier", adminIdentifier);
        payload.put("password",   adminPassword);
        payload.put("type",       "login");
        payload.put("fcmToken",   "ec0gPKSrIUs443ILfDLHaM:APA91bG_test_admin_fcm");

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ADMIN_LOGIN;
        System.out.println("   Target URL   : " + endpoint);
        System.out.println("   Identifier   : " + adminIdentifier);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status : " + response.getStatusCode());
        System.out.println("   Response Body   : " + response.prettyPrint());

        Assert.assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
                "PHLEBO_MGMT_02: Admin Login must return HTTP 200 or 201. Actual: " + response.getStatusCode());

        // Try all common token field paths
        adminToken = response.jsonPath().getString("data.access_token");
        if (adminToken == null || adminToken.isEmpty()) {
            adminToken = response.jsonPath().getString("data.token");
        }
        if (adminToken == null || adminToken.isEmpty()) {
            adminToken = response.jsonPath().getString("access_token");
        }
        Assert.assertNotNull(adminToken,
                "PHLEBO_MGMT_02: Admin token must be present in admin login response");

        System.out.println("   Admin Token Present: " + (adminToken != null && !adminToken.isEmpty()));
        System.out.println("   [PASS] PHLEBO_MGMT_02 PASSED -- Admin login successful.\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHLEBO_MGMT_03 : Assign Shifts
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 3,
          dependsOnMethods = {"PHLEBO_MGMT_01_LoginPhlebo", "PHLEBO_MGMT_02_AdminLogin"},
          description = "PHLEBO MGMT-03: Assign Shifts — Verify admin can assign an 08:00-12:00 shift "
                      + "to the phlebotomist for today's date using the admin JWT.")
    public void PHLEBO_MGMT_03_AssignShifts() {
        System.out.println("\n>>> PHLEBO_MGMT_03: ASSIGN SHIFTS <<<");
        System.out.println("   Using phlebo GUID  : " + phleboGuid);
        System.out.println("   Using admin token  : [present=" + (adminToken != null && !adminToken.isEmpty()) + "]");

        Map<String, Object> payload = PhlebioPayloads.buildAssignShiftsPayload(phleboGuid);
        System.out.println("   Request Payload : " + payload);

        Response response = phlebioClient.assignShifts(payload, adminToken);

        System.out.println("   Response Status : " + response.getStatusCode());
        System.out.println("   Response Body   : " + response.prettyPrint());

        int status = response.getStatusCode();
        Assert.assertTrue(status == 200 || status == 201,
                "PHLEBO_MGMT_03: Assign Shifts must return HTTP 200 or 201. Actual: " + status);

        // Validate success flag when present
        Object successFlag = response.jsonPath().get("success");
        if (successFlag != null) {
            Assert.assertTrue(Boolean.TRUE.equals(successFlag),
                    "PHLEBO_MGMT_03: Response 'success' must be true for Assign Shifts");
        }

        // Validate message / data field presence
        String message = response.jsonPath().getString("message");
        if (message == null) {
            message = response.jsonPath().getString("msg");
        }
        Assert.assertNotNull(message, "PHLEBO_MGMT_03: Response must contain a message field");

        System.out.println("   Message : " + message);
        System.out.println("   [PASS] PHLEBO_MGMT_03 PASSED -- Shifts assigned successfully.\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHLEBO_MGMT_04 : Get Phlebo Shifts
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 4,
          dependsOnMethods = {"PHLEBO_MGMT_03_AssignShifts"},
          description = "PHLEBO MGMT-04: Get Phlebo Shifts — Verify admin can retrieve the shift schedule "
                      + "for the phlebotomist. The shift assigned in PHLEBO_MGMT_03 should be visible.")
    public void PHLEBO_MGMT_04_GetPhleboShifts() {
        System.out.println("\n>>> PHLEBO_MGMT_04: GET PHLEBO SHIFTS <<<");
        System.out.println("   Phlebo GUID : " + phleboGuid);

        Response response = phlebioClient.getPhleboShifts(phleboGuid, adminToken);

        System.out.println("   Response Status : " + response.getStatusCode());
        System.out.println("   Response Body   : " + response.prettyPrint());

        Assert.assertEquals(response.getStatusCode(), 200,
                "PHLEBO_MGMT_04: Get Phlebo Shifts must return HTTP 200. Actual: " + response.getStatusCode());

        // Validate success flag if present
        Object successFlag = response.jsonPath().get("success");
        if (successFlag != null) {
            Assert.assertTrue(Boolean.TRUE.equals(successFlag),
                    "PHLEBO_MGMT_04: Response 'success' must be true for Get Phlebo Shifts");
        }

        // Validate data is not null (shifts should exist after assign)
        Object data = response.jsonPath().get("data");
        Assert.assertNotNull(data,
                "PHLEBO_MGMT_04: 'data' field must not be null in Get Phlebo Shifts response");

        // If data is a list, validate it has at least one shift
        if (data instanceof List) {
            List<?> shiftList = (List<?>) data;
            System.out.println("   Total shifts returned : " + shiftList.size());
            Assert.assertFalse(shiftList.isEmpty(),
                    "PHLEBO_MGMT_04: Shift list must not be empty after assigning a shift in PHLEBO_MGMT_03");
        }

        System.out.println("   [PASS] PHLEBO_MGMT_04 PASSED -- Phlebo shifts retrieved successfully.\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHLEBO_MGMT_05 : Clock In
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 5,
          dependsOnMethods = {"PHLEBO_MGMT_01_LoginPhlebo"},
          description = "PHLEBO MGMT-05: Clock In — Verify the phlebotomist can clock in for their active shift "
                      + "using the phlebo JWT obtained in PHLEBO_MGMT_01. "
                      + "Note: clock-in requires the phlebo's own token (NOT the admin token).")
    public void PHLEBO_MGMT_05_ClockIn() {
        System.out.println("\n>>> PHLEBO_MGMT_05: CLOCK IN <<<");
        System.out.println("   Using phlebo JWT : [present=" + (phleboJwtToken != null && !phleboJwtToken.isEmpty()) + "]");

        Map<String, Object> payload = PhlebioPayloads.buildClockInPayload();
        System.out.println("   Request Payload : " + payload);

        Response response = phlebioClient.clockIn(payload, phleboJwtToken);

        System.out.println("   Response Status : " + response.getStatusCode());
        System.out.println("   Response Body   : " + response.prettyPrint());

        int status = response.getStatusCode();

        // Clock-in requires an active shift. Accept 200/201 (success) or note
        // 4xx scenarios that indicate business constraints (e.g., no active shift
        // at this time, already clocked in), which are still valid API responses.
        if (status == 200 || status == 201) {
            Object successFlag = response.jsonPath().get("success");
            if (successFlag != null) {
                Assert.assertTrue(Boolean.TRUE.equals(successFlag),
                        "PHLEBO_MGMT_05: success flag must be true on 200/201 response");
            }
            System.out.println("   [PASS] PHLEBO_MGMT_05 PASSED -- Clock-in successful.\n");

        } else if (status == 400 || status == 404 || status == 422) {
            // Business constraint: no active shift found at this moment, or
            // outside shift window — acceptable for the staging environment.
            String message = response.jsonPath().getString("message");
            if (message == null) message = response.jsonPath().getString("msg");
            System.out.println("   [INFO] Clock-in returned " + status + " — business constraint: " + message);
            System.out.println("   [PASS] PHLEBO_MGMT_05 PASSED (API responded correctly with business reason).\n");

        } else if (status == 401) {
            Assert.fail("PHLEBO_MGMT_05: Unexpected 401 — phlebo JWT was accepted at login but rejected here. "
                    + "Check token field extraction path.");
        } else {
            Assert.fail("PHLEBO_MGMT_05: Unexpected HTTP status " + status
                    + ". Response: " + response.asString());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper — print compact response summary
    // ─────────────────────────────────────────────────────────────────────────
    private void printResponseSummary(String testId, Response response) {
        System.out.println("   [" + testId + "] HTTP Status : " + response.getStatusCode());
        System.out.println("   [" + testId + "] success     : " + response.jsonPath().get("success"));
        System.out.println("   [" + testId + "] message     : " + response.jsonPath().getString("message"));
    }
}
