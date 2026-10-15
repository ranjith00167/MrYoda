package com.mryoda.diagnostics.api.tests.phlebio;

import api.phlebio.PhlebioAuthShiftValidator;
import api.phlebio.PhlebioClient;
import api.phlebio.PhlebioEndpoints;
import api.phlebio.PhlebioOrderFlowValidator;
import api.phlebio.PhlebioPayloads;
import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.utils.LoggerUtil;
import com.mryoda.diagnostics.api.utils.RequestContext;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

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
 * PHLEBO_MGMT_06 : Assign Order  [Assigns the placed order(s) to this phlebotomist]
 * PHLEBO_MGMT_07 : Update Order Tracking  [Phlebotomist starts the visit using static coordinates]
 * PHLEBO_MGMT_08 : Upload Image  [Phlebotomist uploads an image as multipart/form-data]
 * PHLEBO_MGMT_09 : Verify OTP  [Phlebotomist verifies the customer's visit OTP for the order]
 * PHLEBO_MGMT_10 : Get Sample Details  [getOrderById -> order_items[].sample_types + samples_picked + visit number]
 * PHLEBO_MGMT_11 : Generate Barcode  [random barcode mapped to the visit number via ChangeBarCodedata]
 * PHLEBO_MGMT_12 : Update Order Tracking (samples_collected)  [sample types + quantity + barcode + selfie]
 *
 * Execution order is strictly enforced via TestNG priority.
 * State shared between tests via static fields:
 *   - phleboGuid       (from PHLEBO_MGMT_01)
 *   - phleboJwtToken   (from PHLEBO_MGMT_01 — phlebo notification service JWT)
 *   - adminToken       (from PHLEBO_MGMT_02)
 * ============================================================
 */
public class PhlebioManagementTest extends BaseTest {

    // ── Static coordinates used by PHLEBO_MGMT_07 (Update Order Tracking) ────
    private static final String PICKUP_LAT = "17.4543935";
    private static final String PICKUP_LNG = "78.3934546";
    private static final String PICKUP_NAME = "F93V+Q85";
    private static final String START_LAT = "17.4561044";
    private static final String START_LNG = "78.3762596";
    private static final String CURRENT_LAT = "17.4561044";
    private static final String CURRENT_LNG = "78.3762596";

    // ── Image upload (PHLEBO_MGMT_08) ────────────────────────────────────────────────
    private static final String UPLOAD_IMAGE_PATH = "src/test/resources/TestData/phlebo-test-image.jpg";
    private static final String UPLOAD_FIELD_NAME = "image";   // confirmed against the live API ("file" is rejected)
    private static final String UPLOAD_MIME_TYPE  = "image/jpeg";

    // ── Visit OTP (PHLEBO_MGMT_09) — static OTP accepted on staging ──────────────────
    private static final String VISIT_OTP = "123456";

    // ── Shared state ──────────────────────────────────────────────────────────
    private static Response phleboLoginResponse; // full login body (PHLEBO_MGMT_01) for later cross-checks
    private static String phleboName;       // data.phlebo.name from login
    private static String phleboZoneId;     // data.phlebo.zoneId from login
    private static String phleboGuid;
    private static String phleboJwtToken;   // JWT from phlebo notification service (for clock-in)
    private static String adminToken;       // JWT from diagnostics admin login (for shift management)
    private static String orderTrackingId;  // data.assignedOrders[0].orderTracking.guid from assignOrder
    private static String uploadedImageUrl; // data.url from the phlebo image upload
    private static String visitNumber;      // data[0].visit_number from getOrderById (PHLEBO_MGMT_10)
    private static List<Map<String, Object>> samplesPicked = new ArrayList<>();      // data[0].samples_picked
    private static List<Map<String, Object>> samplesToCollect = new ArrayList<>();   // [{sample_type, quantity}] for PHLEBO_MGMT_12
    private static String generatedBarcode; // random barcode from PHLEBO_MGMT_11 (first order)

    // ── Multi-order support (steps 10 / 11 / 12) ─────────────────────────────
    private static List<String> allPhleboVisitNumbers = new ArrayList<>();                         // visit numbers for ALL orders
    private static Map<String, String> orderVisitNumberMap  = new LinkedHashMap<>();               // orderId  → visitNumber
    private static Map<String, String> visitBarcodeMap      = new LinkedHashMap<>();               // visitNumber → barcode
    private static Map<String, List<Map<String, Object>>> orderSamplesMap = new LinkedHashMap<>(); // orderId  → samplesToCollect
    private static Map<String, String> orderTrackingIdMap   = new LinkedHashMap<>();               // orderId  → trackingId

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

        phleboLoginResponse = response;
        phleboName = response.jsonPath().getString("data.phlebo.name");
        phleboZoneId = response.jsonPath().getString("data.phlebo.zoneId");
        PhlebioAuthShiftValidator.validatePhleboLogin(response, PhlebioPayloads.PHLEBO_MOBILE);

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
        PhlebioAuthShiftValidator.validateAdminLogin(response, adminIdentifier);

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

        // Shift already assigned for today (e.g. earlier run) -> precondition is already met, nothing to assign
        String existingShiftMessage = response.jsonPath().getString("message");
        if (status == 400 && existingShiftMessage != null
                && existingShiftMessage.toLowerCase().contains("already has shift")) {
            System.out.println("   [INFO] Shift already assigned for the selected date - skipping assignment: "
                    + existingShiftMessage);
            verifyTodayShiftVisible("PHLEBO_MGMT_03 read-back (shift already present)");
            System.out.println("   [PASS] PHLEBO_MGMT_03 PASSED -- Shift already present.\n");
            return;
        }

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
        verifyTodayShiftVisible("PHLEBO_MGMT_03 read-back (shift created)");
        System.out.println("   [PASS] PHLEBO_MGMT_03 PASSED -- Shifts assigned successfully.\n");
    }

    /** Cross-API: the shift assigned in step 03 must be readable through the get-shifts API. */
    private void verifyTodayShiftVisible(String scope) {
        Response shifts = phlebioClient.getPhleboShifts(phleboGuid, adminToken);
        PhlebioAuthShiftValidator.validateShifts(scope, shifts, phleboGuid, phleboName, phleboZoneId,
                java.time.LocalDate.now().toString(), "08:00", "12:00");
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

        PhlebioAuthShiftValidator.validateShifts("PHLEBO_MGMT_04 get shifts", response, phleboGuid, phleboName,
                phleboZoneId, java.time.LocalDate.now().toString(), "08:00", "12:00");
        PhlebioAuthShiftValidator.validateProfileMatchesLogin(phleboLoginResponse,
                phlebioClient.getPhleboByGuid(phleboGuid, adminToken));

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

        // Success (200/201) or a well-formed business rejection (no active shift / already clocked in)
        if (status == 401) {
            Assert.fail("PHLEBO_MGMT_05: Unexpected 401 — phlebo JWT was accepted at login but rejected here. "
                    + "Check token field extraction path.");
        }
        PhlebioAuthShiftValidator.validateClockIn(response);
        System.out.println("   [PASS] PHLEBO_MGMT_05 PASSED -- Clock-in response validated (HTTP " + status + ").\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHLEBO_MGMT_06 : Assign Order
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 6,
          dependsOnMethods = {"PHLEBO_MGMT_05_ClockIn"},
          description = "PHLEBO MGMT-06: Assign Order — Assign the order(s) created earlier in the COD flow "
                      + "to the phlebotomist who logged in and clocked in (POST /order_tracking/assignOrder).")
    public void PHLEBO_MGMT_06_AssignOrder() {
        System.out.println("\n>>> PHLEBO_MGMT_06: ASSIGN ORDER <<<");

        List<String> orderIds = RequestContext.getCurrentOrderIds();
        if (orderIds == null || orderIds.isEmpty()) {
            throw new org.testng.SkipException(
                    "PHLEBO_MGMT_06: No order IDs in RequestContext. Run this after the order is created (COD_05).");
        }
        Assert.assertNotNull(phleboGuid, "PHLEBO_MGMT_06: Phlebo GUID must be available from PHLEBO_MGMT_01");

        Map<String, Object> payload = new HashMap<>();
        payload.put("order_ids", orderIds);
        payload.put("phlebo_id", phleboGuid);
        payload.put("is_update", false);

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.ASSIGN_ORDER;
        System.out.println("   Target URL      : " + endpoint);
        System.out.println("   Request Payload : " + payload);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + adminToken)
                .setRequestBody(payload)
                .post();

        System.out.println("   Response Status : " + response.getStatusCode());
        System.out.println("   Response Body   : " + response.prettyPrint());

        Assert.assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
                "PHLEBO_MGMT_06: Assign Order must return HTTP 200 or 201. Actual: " + response.getStatusCode());
        Assert.assertTrue(Boolean.TRUE.equals(response.jsonPath().get("success")),
                "PHLEBO_MGMT_06: Response 'success' must be true");

        Object assignedPhlebo = response.jsonPath().get("data.assignedOrders[0].updatedOrder.phlebo_id");
        if (assignedPhlebo != null) {
            Assert.assertEquals(assignedPhlebo.toString(), phleboGuid,
                    "PHLEBO_MGMT_06: Order must be assigned to the logged-in phlebo");
        }

        // Publish context so the downstream COD steps (status / tracking / OTP) keep working
        RequestContext.setCurrentPhleboGuid(phleboGuid);
        System.setProperty("phlebo.token", phleboJwtToken);

        orderTrackingId = response.jsonPath().getString("data.assignedOrders[0].orderTracking.guid");
        Assert.assertNotNull(orderTrackingId,
                "PHLEBO_MGMT_06: Response must contain data.assignedOrders[0].orderTracking.guid");

        // Collect tracking IDs for ALL assigned orders (multi-order support)
        List<String> trackingIds = new ArrayList<>();
        orderTrackingIdMap.clear();
        List<Map<String, Object>> assignedOrders = response.jsonPath().getList("data.assignedOrders");
        if (assignedOrders != null) {
            for (Map<String, Object> ao : assignedOrders) {
                Object trackingObj = ao.get("orderTracking");
                Object orderObj    = ao.get("updatedOrder");
                String tid = null;
                String oid = null;
                if (trackingObj instanceof Map) {
                    Object tg = ((Map<?, ?>) trackingObj).get("guid");
                    if (tg != null) tid = tg.toString();
                }
                if (orderObj instanceof Map) {
                    Object og = ((Map<?, ?>) orderObj).get("guid");
                    if (og != null) oid = og.toString();
                }
                if (tid != null) {
                    trackingIds.add(tid);
                    if (oid != null) orderTrackingIdMap.put(oid, tid);
                    System.out.println("   Tracking: order=" + oid + " → trackingId=" + tid);
                }
            }
        }
        if (trackingIds.isEmpty()) trackingIds.add(orderTrackingId);
        RequestContext.setCurrentOrderTrackingIds(trackingIds);
        System.out.println("   Order Tracking ID : " + orderTrackingId + " | total=" + trackingIds.size());

        PhlebioOrderFlowValidator.validateAssignOrder(response,
                PhlebioOrderFlowValidator.fetchOrder(orderIds.get(0), phleboJwtToken),
                orderIds.get(0), phleboGuid, phleboName, VISIT_OTP, phleboZoneId, "08:00", "12:00");
        System.out.println("   [PASS] PHLEBO_MGMT_06 PASSED -- Order assigned to phlebo " + phleboGuid + ".\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHLEBO_MGMT_07 : Update Order Tracking
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 7,
          dependsOnMethods = {"PHLEBO_MGMT_06_AssignOrder"},
          description = "PHLEBO MGMT-07: Update Order Tracking — Phlebotomist starts the visit (status 'inprogress') "
                      + "using static coordinates (POST /order_tracking/updateOrderTracking).")
    public void PHLEBO_MGMT_07_UpdateOrderTracking() {
        System.out.println("\n>>> PHLEBO_MGMT_07: UPDATE ORDER TRACKING <<<");

        List<String> orderIds = RequestContext.getCurrentOrderIds();
        Assert.assertNotNull(orderTrackingId, "PHLEBO_MGMT_07: Order tracking id must be available from PHLEBO_MGMT_06");
        Assert.assertTrue(orderIds != null && !orderIds.isEmpty(), "PHLEBO_MGMT_07: Order id must be available");

        Map<String, String> pickupLocation = new HashMap<>();
        pickupLocation.put("lat", PICKUP_LAT);
        pickupLocation.put("lng", PICKUP_LNG);
        pickupLocation.put("name", PICKUP_NAME);

        Map<String, String> startLocation = new HashMap<>();
        startLocation.put("lat", START_LAT);
        startLocation.put("lng", START_LNG);

        Map<String, String> currentLocation = new HashMap<>();
        currentLocation.put("lat", CURRENT_LAT);
        currentLocation.put("lng", CURRENT_LNG);

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_ORDER_TRACKING;

        // Mark ALL orders as inprogress — each has its own tracking ID from step 06
        for (int idx = 0; idx < orderIds.size(); idx++) {
            String oid = orderIds.get(idx);
            String tid = orderTrackingIdMap.getOrDefault(oid, orderTrackingId);

            System.out.println("\n   --- Order " + (idx + 1) + "/" + orderIds.size()
                    + " | orderId=" + oid + " trackingId=" + tid + " ---");

            Map<String, Object> payload = new HashMap<>();
            payload.put("status", "inprogress");
            payload.put("order_tracking_id", tid);
            payload.put("order_id", oid);
            payload.put("pickup_location", pickupLocation);
            payload.put("start_location", startLocation);
            payload.put("current_location", currentLocation);

            System.out.println("   Target URL      : " + endpoint);
            System.out.println("   Request Payload : " + payload);

            Response response = new RequestBuilder()
                    .setEndpoint(endpoint)
                    .addHeader("Authorization", "Bearer " + phleboJwtToken)
                    .setRequestBody(payload)
                    .post();

            System.out.println("   Response Status : " + response.getStatusCode());
            System.out.println("   Response Body   : " + response.prettyPrint());

            Assert.assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
                    "PHLEBO_MGMT_07: Update Order Tracking must return HTTP 200 or 201 for order " + oid
                            + ". Actual: " + response.getStatusCode() + " Body: " + response.getBody().asString());

            // Full validator only on first order
            if (idx == 0) {
                PhlebioOrderFlowValidator.validateInProgress(response,
                        PhlebioOrderFlowValidator.fetchOrder(oid, phleboJwtToken),
                        tid, oid, phleboGuid, PICKUP_LAT, PICKUP_LNG);
            }
        }

        System.out.println("   [PASS] PHLEBO_MGMT_07 PASSED -- Order tracking updated to 'inprogress' for "
                + orderIds.size() + " order(s).\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHLEBO_MGMT_08 : Upload Image
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 8,
          dependsOnMethods = {"PHLEBO_MGMT_07_UpdateOrderTracking"},
          description = "PHLEBO MGMT-08: Upload Image — Phlebotomist uploads a JPG as multipart/form-data "
                      + "(POST /api/v1/phlebo/upload) and receives the stored image URL.")
    public void PHLEBO_MGMT_08_UploadImage() {
        System.out.println("\n>>> PHLEBO_MGMT_08: UPLOAD IMAGE <<<");

        File image = new File(UPLOAD_IMAGE_PATH);
        Assert.assertTrue(image.exists() && image.length() > 0,
                "PHLEBO_MGMT_08: Test image missing or empty at " + image.getAbsolutePath());
        System.out.println("   Target URL : " + PhlebioEndpoints.UPLOAD_IMAGE);
        System.out.println("   Image file : " + image.getPath() + " (" + image.length() + " bytes)");

        // multiPart() lets RestAssured set Content-Type (with boundary) and Content-Length itself
        long uploadStartedAt = System.currentTimeMillis();
        Response response = RestAssured.given()
                .relaxedHTTPSValidation()
                .log().all()
                .header("Authorization", "Bearer " + phleboJwtToken)
                .multiPart(UPLOAD_FIELD_NAME, image, UPLOAD_MIME_TYPE)
                .when()
                .post(PhlebioEndpoints.UPLOAD_IMAGE)
                .then().extract().response();

        System.out.println("   Response Status : " + response.getStatusCode());
        System.out.println("   Response Body   : " + response.prettyPrint());

        Assert.assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
                "PHLEBO_MGMT_08: Upload Image must return HTTP 200 or 201. Actual: " + response.getStatusCode()
                        + " Body: " + response.getBody().asString());
        Assert.assertTrue(Boolean.TRUE.equals(response.jsonPath().get("success")),
                "PHLEBO_MGMT_08: Response 'success' must be true");

        String imageUrl = response.jsonPath().getString("data.url");
        Assert.assertNotNull(imageUrl, "PHLEBO_MGMT_08: Response must contain data.url");
        Assert.assertTrue(imageUrl.startsWith("https://") && imageUrl.toLowerCase().endsWith(".jpg"),
                "PHLEBO_MGMT_08: data.url must be an https URL of the uploaded .jpg. Actual: " + imageUrl);

        uploadedImageUrl = imageUrl;
        PhlebioOrderFlowValidator.validateUpload(response, image, uploadStartedAt);
        System.out.println("   Uploaded Image URL : " + uploadedImageUrl);
        System.out.println("   [PASS] PHLEBO_MGMT_08 PASSED -- Image uploaded successfully.\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHLEBO_MGMT_09 : Verify OTP
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 9,
          dependsOnMethods = {"PHLEBO_MGMT_08_UploadImage"},
          description = "PHLEBO MGMT-09: Verify OTP — Phlebotomist verifies the visit OTP for each assigned order "
                      + "(POST /order_tracking/verifyotp).")
    public void PHLEBO_MGMT_09_VerifyOtp() {
        System.out.println("\n>>> PHLEBO_MGMT_09: VERIFY OTP <<<");

        List<String> orderIds = RequestContext.getCurrentOrderIds();
        if (orderIds == null || orderIds.isEmpty()) {
            throw new org.testng.SkipException("PHLEBO_MGMT_09: No order IDs in RequestContext.");
        }

        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.VERIFY_OTP;
        System.out.println("   Target URL : " + endpoint);

        for (String orderId : orderIds) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("orderid", orderId);
            payload.put("OTP", VISIT_OTP);
            System.out.println("   Request Payload : " + payload);

            // The mobile app sends the raw phlebo JWT (no "Bearer " prefix)
            Response response = new RequestBuilder()
                    .setEndpoint(endpoint)
                    .addHeader("Authorization", phleboJwtToken)
                    .setRequestBody(payload)
                    .post();

            System.out.println("   Response Status : " + response.getStatusCode());
            System.out.println("   Response Body   : " + response.prettyPrint());

            Assert.assertEquals(response.getStatusCode(), 200,
                    "PHLEBO_MGMT_09: Verify OTP must return HTTP 200 for order " + orderId
                            + ". Body: " + response.getBody().asString());
            Assert.assertTrue(Boolean.TRUE.equals(response.jsonPath().get("success")),
                    "PHLEBO_MGMT_09: Response 'success' must be true for order " + orderId);
            PhlebioOrderFlowValidator.validateVerifyOtp(response,
                    PhlebioOrderFlowValidator.fetchOrder(orderId, phleboJwtToken), orderId, phleboGuid);
        }
        System.out.println("   [PASS] PHLEBO_MGMT_09 PASSED -- OTP verified for " + orderIds.size() + " order(s).\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHLEBO_MGMT_10 : Get Sample Details
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 10,
          dependsOnMethods = {"PHLEBO_MGMT_09_VerifyOtp"},
          description = "PHLEBO MGMT-10: Get Sample Details — GET /order/getOrderById/{order_id}. Captures the sample "
                      + "types (order_items[].sample_types[].DefaultSampleType), samples_picked and visit number "
                      + "for use in the barcode and samples_collected steps.")
    public void PHLEBO_MGMT_10_GetSampleDetails() {
        System.out.println("\n>>> PHLEBO_MGMT_10: GET SAMPLE DETAILS <<<");

        List<String> orderIds = RequestContext.getCurrentOrderIds();
        if (orderIds == null || orderIds.isEmpty()) {
            throw new org.testng.SkipException("PHLEBO_MGMT_10: No order IDs in RequestContext.");
        }

        // Clear multi-order maps before repopulating
        allPhleboVisitNumbers.clear();
        orderVisitNumberMap.clear();
        orderSamplesMap.clear();

        // Process ALL orders — each gets its own visit number, sample list, and barcode in step 11
        for (int idx = 0; idx < orderIds.size(); idx++) {
            String oid = orderIds.get(idx);
            System.out.println("\n   --- Processing order " + (idx + 1) + "/" + orderIds.size() + " : " + oid + " ---");
            String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.GET_ORDER_BY_ID + oid;

            Response response = fetchOrderAwaitingVisitNumber(endpoint);
            System.out.println("   Response Status : " + response.getStatusCode());
            Assert.assertEquals(response.getStatusCode(), 200,
                    "PHLEBO_MGMT_10: getOrderById must return HTTP 200 for order " + oid
                            + ". Body: " + response.getBody().asString());

            String vn = response.jsonPath().getString("data[0].visit_number");
            Assert.assertTrue(vn != null && vn.matches("^MYD\\d+$"),
                    "PHLEBO_MGMT_10: order " + oid + " still has no real visit_number (MYD#####) after "
                            + VISIT_NUMBER_POLL_ATTEMPTS + " reads [" + vn + "]");

            // Build sample list for this order
            Map<String, String> uniqueSampleTypes = new LinkedHashMap<>();
            List<Map<String, Object>> orderItems = response.jsonPath().getList("data[0].order_items");
            if (orderItems != null) {
                for (Map<String, Object> item : orderItems) {
                    Object sampleTypes = item.get("sample_types");
                    if (!(sampleTypes instanceof List)) continue;
                    for (Object st : (List<?>) sampleTypes) {
                        if (st instanceof Map) {
                            Object name = ((Map<?, ?>) st).get("DefaultSampleType");
                            if (name != null && !name.toString().trim().isEmpty()) {
                                uniqueSampleTypes.putIfAbsent(name.toString().trim().toUpperCase(), name.toString().trim());
                            }
                        }
                    }
                }
            }
            List<Map<String, Object>> picked = response.jsonPath().getList("data[0].samples_picked");
            List<Map<String, Object>> toCollect = new ArrayList<>();
            if (picked != null && !picked.isEmpty()) {
                toCollect.addAll(picked);
            } else {
                for (String st : uniqueSampleTypes.values()) {
                    Map<String, Object> sample = new LinkedHashMap<>();
                    sample.put("sample_type", st);
                    sample.put("quantity", 1);
                    toCollect.add(sample);
                }
            }

            allPhleboVisitNumbers.add(vn);
            orderVisitNumberMap.put(oid, vn);
            orderSamplesMap.put(oid, toCollect);
            RequestContext.mapOrderToVisit(oid, vn);
            System.out.println("   Visit Number     : " + vn);
            System.out.println("   Samples to collect: " + toCollect);

            // For backward compat keep single-value fields set to the first order's data
            if (idx == 0) {
                visitNumber      = vn;
                samplesPicked    = picked != null ? new ArrayList<>(picked) : new ArrayList<>();
                samplesToCollect = toCollect;
                RequestContext.setVisitNumber(vn);
                PhlebioOrderFlowValidator.validateSampleDetails(response, oid, phleboGuid, phleboName, VISIT_OTP,
                        PICKUP_LAT, PICKUP_LNG);
            }
        }

        System.out.println("   [PASS] PHLEBO_MGMT_10 PASSED -- Sample details captured for "
                + orderIds.size() + " order(s): " + allPhleboVisitNumbers + "\n");
    }

    private static final int VISIT_NUMBER_POLL_ATTEMPTS = 10;
    private static final long VISIT_NUMBER_POLL_INTERVAL_MS = 3000L;

    /** GETs the order until data[0].visit_number is a real lab number, or the attempts are exhausted. */
    private Response fetchOrderAwaitingVisitNumber(String endpoint) {
        Response response = null;
        for (int attempt = 1; attempt <= VISIT_NUMBER_POLL_ATTEMPTS; attempt++) {
            response = new RequestBuilder()
                    .setEndpoint(endpoint)
                    .addHeader("Authorization", "Bearer " + phleboJwtToken)
                    .get();
            String visit = response.getStatusCode() == 200
                    ? response.jsonPath().getString("data[0].visit_number") : null;
            if (visit != null && visit.matches("^MYD\\d+$")) {
                return response;
            }
            System.out.println("   visit_number not generated yet [" + visit + "] (read " + attempt + "/"
                    + VISIT_NUMBER_POLL_ATTEMPTS + ")");
            try {
                Thread.sleep(VISIT_NUMBER_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHLEBO_MGMT_11 : Generate Barcode
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 11,
          dependsOnMethods = {"PHLEBO_MGMT_10_GetSampleDetails"},
          description = "PHLEBO MGMT-11: Generate Barcode — Maps a random 8-digit barcode to the visit number "
                      + "(POST ChangeBarCodedata on the UAT LIS service).")
    public void PHLEBO_MGMT_11_GenerateBarcode() {
        System.out.println("\n>>> PHLEBO_MGMT_11: GENERATE BARCODE <<<");
        Assert.assertNotNull(visitNumber, "PHLEBO_MGMT_11: Visit number must be available from PHLEBO_MGMT_10");

        // Use allPhleboVisitNumbers (all orders); fall back to the single visitNumber if empty
        List<String> visitsToBarcode = allPhleboVisitNumbers.isEmpty()
                ? Collections.singletonList(visitNumber)
                : allPhleboVisitNumbers;

        visitBarcodeMap.clear();
        System.out.println("   Generating barcodes for " + visitsToBarcode.size() + " visit(s): " + visitsToBarcode);

        for (int idx = 0; idx < visitsToBarcode.size(); idx++) {
            String vn      = visitsToBarcode.get(idx);
            String barcode = String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 100_000_000));

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("labno", vn);
            payload.put("Barcodeno", barcode);

            System.out.println("\n   --- Barcode " + (idx + 1) + "/" + visitsToBarcode.size()
                    + " | visit=" + vn + " barcode=" + barcode + " ---");
            System.out.println("   Target URL      : " + PhlebioEndpoints.CHANGE_BARCODE);

            Response response = new RequestBuilder()
                    .setEndpoint(PhlebioEndpoints.CHANGE_BARCODE)
                    .setRequestBody(payload)
                    .post();

            System.out.println("   Response Status : " + response.getStatusCode());
            System.out.println("   Response Body   : " + response.getBody().asString());

            Assert.assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
                    "PHLEBO_MGMT_11: ChangeBarCodedata must return HTTP 200 or 201 for visit " + vn
                            + ". Actual: " + response.getStatusCode() + " Body: " + response.getBody().asString());

            visitBarcodeMap.put(vn, barcode);

            // Keep first-order backward-compat fields
            if (idx == 0) {
                generatedBarcode = barcode;
                PhlebioOrderFlowValidator.validateBarcode(response, vn, barcode,
                        PhlebioOrderFlowValidator.fetchOrder(RequestContext.getCurrentOrderIds().get(0), phleboJwtToken)
                                .jsonPath().getString("data[0].visit_number"));
            }
        }

        System.out.println("   [PASS] PHLEBO_MGMT_11 PASSED -- Barcodes generated for all visits: " + visitBarcodeMap + "\n");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PHLEBO_MGMT_12 : Update Order Tracking (samples_collected)
    // ─────────────────────────────────────────────────────────────────────────
    @Test(priority = 12,
          dependsOnMethods = {"PHLEBO_MGMT_11_GenerateBarcode"},
          description = "PHLEBO MGMT-12: Update Order Tracking — Move the order to 'samples_collected' with the sample "
                      + "types/quantities from PHLEBO_MGMT_10, the barcode from PHLEBO_MGMT_11 and the uploaded selfie "
                      + "(POST /order_tracking/updateOrderTracking).")
    public void PHLEBO_MGMT_12_UpdateOrderTrackingSamplesCollected() {
        System.out.println("\n>>> PHLEBO_MGMT_12: UPDATE ORDER TRACKING (SAMPLES COLLECTED) <<<");

        Assert.assertNotNull(orderTrackingId, "PHLEBO_MGMT_12: Order tracking id must be available from PHLEBO_MGMT_06");
        Assert.assertFalse(samplesToCollect.isEmpty(), "PHLEBO_MGMT_12: Samples list must be available from PHLEBO_MGMT_10");
        Assert.assertNotNull(generatedBarcode, "PHLEBO_MGMT_12: Barcode must be available from PHLEBO_MGMT_11");
        Assert.assertNotNull(uploadedImageUrl, "PHLEBO_MGMT_12: Selfie URL must be available from PHLEBO_MGMT_08");

        List<String> orderIds = RequestContext.getCurrentOrderIds();
        String endpoint = APIEndpoints.DIAGNOSTICS_BASE_URL + APIEndpoints.UPDATE_ORDER_TRACKING;

        // Determine which orders + tracking IDs to process
        // Build a list of [orderId, trackingId, visitNumber, barcode, samples]
        List<String> processedOrderIds = new ArrayList<>();
        for (String oid : orderIds) {
            String tid = orderTrackingIdMap.getOrDefault(oid, orderTrackingId); // fallback to first tracking ID
            String vn  = orderVisitNumberMap.getOrDefault(oid, visitNumber);
            String bc  = visitBarcodeMap.getOrDefault(vn, generatedBarcode);
            List<Map<String, Object>> samples = orderSamplesMap.getOrDefault(oid, samplesToCollect);
            processedOrderIds.add(oid);

            System.out.println("\n   --- Order " + (processedOrderIds.size()) + "/" + orderIds.size()
                    + " | orderId=" + oid + " trackingId=" + tid + " visit=" + vn + " barcode=" + bc + " ---");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("status", "samples_collected");
            payload.put("order_tracking_id", tid);
            payload.put("samples_collected", samples);
            payload.put("barcode_id", bc);
            payload.put("consent_form_type", "");
            payload.put("consent_form_image", new ArrayList<String>());
            payload.put("phlebo_selfie", uploadedImageUrl);

            System.out.println("   Target URL      : " + endpoint);
            System.out.println("   Request Payload : " + payload);

            Response response = new RequestBuilder()
                    .setEndpoint(endpoint)
                    .addHeader("Authorization", "Bearer " + phleboJwtToken)
                    .setRequestBody(payload)
                    .post();

            System.out.println("   Response Status : " + response.getStatusCode());
            System.out.println("   Response Body   : " + response.getBody().asString());

            Assert.assertTrue(response.getStatusCode() == 200 || response.getStatusCode() == 201,
                    "PHLEBO_MGMT_12: samples_collected update must return 200/201 for order " + oid
                            + ". Actual: " + response.getStatusCode() + " Body: " + response.getBody().asString());
            Assert.assertTrue(Boolean.TRUE.equals(response.jsonPath().get("success")),
                    "PHLEBO_MGMT_12: Response 'success' must be true for order " + oid);

            // Full validator only on first order (validator requires exact single-order data)
            if (processedOrderIds.size() == 1) {
                PhlebioOrderFlowValidator.validateSamplesCollected(response,
                        PhlebioOrderFlowValidator.fetchOrder(oid, phleboJwtToken), tid, oid, phleboGuid,
                        samples, bc, uploadedImageUrl, vn);
            }
        }

        System.out.println("   [PASS] PHLEBO_MGMT_12 PASSED -- samples_collected updated for "
                + processedOrderIds.size() + " order(s).\n");
    }
}
