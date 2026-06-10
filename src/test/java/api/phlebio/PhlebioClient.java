package api.phlebio;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import org.json.JSONObject;

/**
 * Phlebio API Client
 * Encapsulates all HTTP calls for the Phlebio Authentication module.
 * Every method returns the raw RestAssured Response for full assertion flexibility.
 */
public class PhlebioClient {

    // ── Login ──────────────────────────────────────────────────────────────
    /**
     * POST /api/v1/phlebo/login
     * No auth header required. Returns access_token and refresh_token inside data{}.
     */
    public Response login(JSONObject payload) {
        return new RequestBuilder()
                .setEndpoint(PhlebioEndpoints.LOGIN)
                .setRequestBody(payload.toString())
                .post();
    }

    // ── Logout ─────────────────────────────────────────────────────────────
    /**
     * POST /api/v1/phlebo/logout
     * Requires Bearer token in Authorization header.
     */
    public Response logout(String accessToken) {
        return new RequestBuilder()
                .setEndpoint(PhlebioEndpoints.LOGOUT)
                .addHeader("Authorization", "Bearer " + accessToken)
                .post();
    }

    // ── Reset Password — Step 1: Send OTP ──────────────────────────────────
    /**
     * POST /api/v1/phlebo/reset-password  (with mobile_number only)
     * Triggers OTP delivery to the registered mobile.
     */
    public Response resetPasswordSendOtp(JSONObject payload) {
        return new RequestBuilder()
                .setEndpoint(PhlebioEndpoints.RESET_PASSWORD)
                .setRequestBody(payload.toString())
                .post();
    }

    // ── Reset Password — Step 2: Verify OTP ────────────────────────────────
    /**
     * POST /api/v1/phlebo/reset-password  (with mobile_number + otpInput)
     * Verifies the OTP entered by the phlebotomist.
     */
    public Response resetPasswordVerifyOtp(JSONObject payload) {
        return new RequestBuilder()
                .setEndpoint(PhlebioEndpoints.RESET_PASSWORD)
                .setRequestBody(payload.toString())
                .post();
    }

    // -- Update Password ----------------------------------------------------
    /**
     * PUT /api/v1/phlebo/password
     * Updates the phlebotomist password after OTP verification.
     */
    public Response updatePassword(JSONObject payload) {
        return new RequestBuilder()
                .setEndpoint(PhlebioEndpoints.UPDATE_PASSWORD)
                .setRequestBody(payload.toString())
                .put();
    }

    // -- Send Login OTP ----------------------------------------------------
    /**
     * POST /api/v1/phlebo/send-login-otp
     * Triggers OTP delivery to the phlebotomist's registered mobile for login.
     */
    public Response sendLoginOtp(JSONObject payload) {
        return new RequestBuilder()
                .setEndpoint(PhlebioEndpoints.SEND_LOGIN_OTP)
                .setRequestBody(payload.toString())
                .post();
    }

    // -- Login With OTP ----------------------------------------------------
    /**
     * POST /api/v1/phlebo/login-with-otp
     * Authenticates the phlebotomist using mobile + OTP + FCM token.
     * Returns access_token and refresh_token same as password login.
     */
    public Response loginWithOtp(JSONObject payload) {
        return new RequestBuilder()
                .setEndpoint(PhlebioEndpoints.LOGIN_WITH_OTP)
                .setRequestBody(payload.toString())
                .post();
    }

    // -- Logout Without Auth Header (negative scenario) --------------------
    /**
     * POST /api/v1/phlebo/logout  — no Authorization header.
     * Expected: 401 Unauthorized
     */
    public Response logoutWithoutAuth() {
        return new RequestBuilder()
                .setEndpoint(PhlebioEndpoints.LOGOUT)
                .post();
    }

    // -- Logout With Invalid Token (negative scenario) ---------------------
    /**
     * POST /api/v1/phlebo/logout  — malformed/expired Bearer token.
     * Expected: 401 Unauthorized
     */
    public Response logoutWithInvalidToken(String invalidToken) {
        return new RequestBuilder()
                .setEndpoint(PhlebioEndpoints.LOGOUT)
                .addHeader("Authorization", "Bearer " + invalidToken)
                .post();
    }

    // =========================================================
    // SHIFT MANAGEMENT
    // =========================================================

    // -- Assign Shifts (admin operation) -----------------------------------
    /**
     * POST /api/v1/phlebo/assign-shifts
     * Assigns one or more time-slots to a list of phlebotomists for a date range.
     * Requires: admin Bearer token in Authorization header.
     */
    public Response assignShifts(java.util.Map<String, Object> payload, String adminToken) {
        return new RequestBuilder()
                .setEndpoint(PhlebioEndpoints.ASSIGN_SHIFTS)
                .addHeader("Authorization", "Bearer " + adminToken)
                .setRequestBody(payload)
                .post();
    }

    /**
     * POST /api/v1/phlebo/assign-shifts — without Authorization header (negative).
     * Expected: 401 Unauthorized
     */
    public Response assignShiftsWithoutAuth(java.util.Map<String, Object> payload) {
        return new RequestBuilder()
                .setEndpoint(PhlebioEndpoints.ASSIGN_SHIFTS)
                .setRequestBody(payload)
                .post();
    }

    /**
     * POST /api/v1/phlebo/assign-shifts — with an invalid/expired token (negative).
     * Expected: 401 Unauthorized
     */
    public Response assignShiftsWithInvalidToken(java.util.Map<String, Object> payload, String invalidToken) {
        return new RequestBuilder()
                .setEndpoint(PhlebioEndpoints.ASSIGN_SHIFTS)
                .addHeader("Authorization", "Bearer " + invalidToken)
                .setRequestBody(payload)
                .post();
    }

    /**
     * POST /api/v1/phlebo/shifts/assign  (newer endpoint path)
     * Assigns shift slots to one or more phlebotomists for a date range.
     * Payload: { phlebo_guids[], from_date, to_date, slots[{start_time, end_time}] }
     * Requires: admin Bearer token in Authorization header.
     */
    public Response assignShiftsV2(java.util.Map<String, Object> payload, String adminToken) {
        return new RequestBuilder()
                .setEndpoint(PhlebioEndpoints.SHIFTS_ASSIGN)
                .addHeader("Authorization", "Bearer " + adminToken)
                .setRequestBody(payload)
                .post();
    }

    // -- Get Phlebo Shifts (admin/phlebo operation) -------------------------
    /**
     * GET /api/v1/phlebo/{phlebo_guid}/shifts
     * Returns all shifts assigned to the given phlebotomist.
     * Requires: admin Bearer token in Authorization header.
     */
    public Response getPhleboShifts(String phleboGuid, String adminToken) {
        String endpoint = PhlebioEndpoints.GET_SHIFTS.replace("{phlebo_guid}", phleboGuid);
        return new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + adminToken)
                .get();
    }

    /**
     * GET /api/v1/phlebo/{phlebo_guid}/shifts?from_date=&to_date=
     * Returns shifts assigned to the given phlebotomist filtered by date range.
     * Requires: admin Bearer token in Authorization header.
     */
    public Response getPhleboShiftsForDateRange(String phleboGuid, String fromDate, String toDate, String adminToken) {
        String endpoint = PhlebioEndpoints.GET_SHIFTS.replace("{phlebo_guid}", phleboGuid);
        return new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + adminToken)
                .addQueryParam("from_date", fromDate)
                .addQueryParam("to_date", toDate)
                .get();
    }

    /**
     * DELETE /api/v1/phlebo/{phlebo_guid}/shifts/delete
     * Deletes (cancels) shifts for the given phlebotomist within the specified date range.
     * Payload: { "from_date": "yyyy-MM-dd", "to_date": "yyyy-MM-dd" }
     * Requires: admin Bearer token in Authorization header.
     */
    public Response deletePhleboShifts(String phleboGuid, java.util.Map<String, Object> payload, String adminToken) {
        String endpoint = PhlebioEndpoints.DELETE_SHIFTS.replace("{phlebo_guid}", phleboGuid);
        return new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + adminToken)
                .setRequestBody(payload)
                .delete();
    }

    /**
     * GET /api/v1/phlebo/{phlebo_guid}/shifts — without Authorization header.
     * Expected: 401 Unauthorized
     */
    public Response getPhleboShiftsWithoutAuth(String phleboGuid) {
        String endpoint = PhlebioEndpoints.GET_SHIFTS.replace("{phlebo_guid}", phleboGuid);
        return new RequestBuilder()
                .setEndpoint(endpoint)
                .get();
    }

    /**
     * GET /api/v1/phlebo/{phlebo_guid}/shifts — with non-existent phlebo GUID.
     * Expected: 404 Not Found or empty data
     */
    public Response getPhleboShiftsForInvalidGuid(String invalidGuid, String adminToken) {
        String endpoint = PhlebioEndpoints.GET_SHIFTS.replace("{phlebo_guid}", invalidGuid);
        return new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + adminToken)
                .get();
    }

    // -- Clock In (phlebo operation) ----------------------------------------
    /**
     * POST /api/v1/phlebo/shift/clock-in
     * Phlebotomist clocks in for their current active shift.
     * Requires: the phlebo's own JWT Bearer token (NOT admin token).
     */
    public Response clockIn(java.util.Map<String, Object> payload, String phleboToken) {
        return new RequestBuilder()
                .setEndpoint(PhlebioEndpoints.CLOCK_IN)
                .addHeader("Authorization", "Bearer " + phleboToken)
                .setRequestBody(payload)
                .post();
    }

    /**
     * POST /api/v1/phlebo/shift/clock-in — without Authorization header (negative).
     * Expected: 401 Unauthorized
     */
    public Response clockInWithoutAuth(java.util.Map<String, Object> payload) {
        return new RequestBuilder()
                .setEndpoint(PhlebioEndpoints.CLOCK_IN)
                .setRequestBody(payload)
                .post();
    }

    /**
     * POST /api/v1/phlebo/shift/clock-in — with invalid/expired token (negative).
     * Expected: 401 Unauthorized
     */
    public Response clockInWithInvalidToken(java.util.Map<String, Object> payload, String invalidToken) {
        return new RequestBuilder()
                .setEndpoint(PhlebioEndpoints.CLOCK_IN)
                .addHeader("Authorization", "Bearer " + invalidToken)
                .setRequestBody(payload)
                .post();
    }

    // =========================================================
    // REGISTRATION — savePhlebo
    // =========================================================

    /**
     * POST /api/v1/phlebo/savePhlebo
     * Creates a new phlebotomist in the system.
     * Requires: admin Bearer token in Authorization header.
     *
     * @param payload  JSON body built by {@link api.phlebio.PhlebioPayloads#buildSavePhleboPayload()}
     * @param adminToken  admin JWT obtained from diagnostics admin login
     */
    public Response savePhlebo(org.json.JSONObject payload, String adminToken) {
        return new RequestBuilder()
                .setEndpoint(PhlebioEndpoints.SAVE_PHLEBO)
                .addHeader("Authorization", "Bearer " + adminToken)
                .setRequestBody(payload.toString())
                .post();
    }

    /**
     * POST /api/v1/phlebo/savePhlebo — without Authorization header (negative).
     * Expected: 401 Unauthorized
     */
    public Response savePhleboWithoutAuth(org.json.JSONObject payload) {
        return new RequestBuilder()
                .setEndpoint(PhlebioEndpoints.SAVE_PHLEBO)
                .setRequestBody(payload.toString())
                .post();
    }

    /**
     * POST /api/v1/phlebo/savePhlebo — with an invalid/expired token (negative).
     * Expected: 401 Unauthorized
     */
    public Response savePhleboWithInvalidToken(org.json.JSONObject payload, String invalidToken) {
        return new RequestBuilder()
                .setEndpoint(PhlebioEndpoints.SAVE_PHLEBO)
                .addHeader("Authorization", "Bearer " + invalidToken)
                .setRequestBody(payload.toString())
                .post();
    }

    // =========================================================
    // savePhlebo — FORM DATA variants
    // The savePhlebo endpoint expects multipart/form-data, NOT JSON.
    // Field names: mobile_number, zone_id, type[]  (confirmed via browser Network tab)
    // =========================================================

    /**
     * POST /api/v1/phlebo/savePhlebo as multipart form-data.
     * Uses RestAssured formParam() so each key/value is sent as a form field.
     *
     * @param formParams  Map built by {@link api.phlebio.PhlebioPayloads#buildSavePhleboFormParams()}
     * @param token       Bearer token (phlebo notification service JWT)
     */
    public Response savePhleboAsFormData(java.util.Map<String, String> formParams, String token) {
        io.restassured.specification.RequestSpecification req = io.restassured.RestAssured.given()
                .relaxedHTTPSValidation()
                .log().all()
                .header("Authorization", "Bearer " + token)
                .header("type", "Mr.Yoda-Admin");
        for (java.util.Map.Entry<String, String> entry : formParams.entrySet()) {
            req.formParam(entry.getKey(), entry.getValue());
        }
        io.restassured.response.Response response = req.post(PhlebioEndpoints.SAVE_PHLEBO)
                .then().extract().response();
        System.out.println("\n--- savePhleboAsFormData RESPONSE ---");
        System.out.println("   Status  : " + response.getStatusCode());
        System.out.println("   Endpoint: " + PhlebioEndpoints.SAVE_PHLEBO);
        return response;
    }

    /**
     * POST /api/v1/phlebo/savePhlebo as form-data — without Authorization header (negative).
     * Includes the type: Mr.Yoda-Admin header so the only missing variable is the auth token.
     * Expected: 401 Unauthorized
     */
    public Response savePhleboAsFormDataWithoutAuth(java.util.Map<String, String> formParams) {
        io.restassured.specification.RequestSpecification req = io.restassured.RestAssured.given()
                .relaxedHTTPSValidation()
                .log().all()
                .header("type", "Mr.Yoda-Admin");
        for (java.util.Map.Entry<String, String> entry : formParams.entrySet()) {
            req.formParam(entry.getKey(), entry.getValue());
        }
        return req.post(PhlebioEndpoints.SAVE_PHLEBO).then().extract().response();
    }

    /**
     * POST /api/v1/phlebo/savePhlebo as form-data — with invalid/expired token (negative).
     * Includes the type: Mr.Yoda-Admin header so the only invalid variable is the token.
     * Expected: 401 Unauthorized
     */
    public Response savePhleboAsFormDataWithInvalidToken(java.util.Map<String, String> formParams,
                                                          String invalidToken) {
        io.restassured.specification.RequestSpecification req = io.restassured.RestAssured.given()
                .relaxedHTTPSValidation()
                .log().all()
                .header("Authorization", "Bearer " + invalidToken)
                .header("type", "Mr.Yoda-Admin");
        for (java.util.Map.Entry<String, String> entry : formParams.entrySet()) {
            req.formParam(entry.getKey(), entry.getValue());
        }
        return req.post(PhlebioEndpoints.SAVE_PHLEBO).then().extract().response();
    }

    // =========================================================
    // GET PHLEBO BY GUID
    // =========================================================

    /**
     * GET /api/v1/phlebo/{guid}
     * Fetches a single phlebotomist by their UUID.
     * Requires: admin Bearer token + type: Mr.Yoda-Admin header.
     */
    public Response getPhleboByGuid(String guid, String adminToken) {
        String endpoint = PhlebioEndpoints.GET_PHLEBO_BY_GUID.replace("{guid}", guid);
        return io.restassured.RestAssured.given()
                .relaxedHTTPSValidation()
                .log().all()
                .header("Authorization", "Bearer " + adminToken)
                .header("type", "Mr.Yoda-Admin")
                .get(endpoint)
                .then().extract().response();
    }

    /**
     * GET /api/v1/phlebo/{guid} — without Authorization header (negative).
     * Expected: 401 Unauthorized
     */
    public Response getPhleboByGuidWithoutAuth(String guid) {
        String endpoint = PhlebioEndpoints.GET_PHLEBO_BY_GUID.replace("{guid}", guid);
        return io.restassured.RestAssured.given()
                .relaxedHTTPSValidation()
                .log().all()
                .header("type", "Mr.Yoda-Admin")
                .get(endpoint)
                .then().extract().response();
    }

    /**
     * GET /api/v1/phlebo/{guid} — with an invalid/expired Bearer token (negative).
     * Expected: 401 Unauthorized
     */
    public Response getPhleboByGuidWithInvalidToken(String guid, String invalidToken) {
        String endpoint = PhlebioEndpoints.GET_PHLEBO_BY_GUID.replace("{guid}", guid);
        return io.restassured.RestAssured.given()
                .relaxedHTTPSValidation()
                .log().all()
                .header("Authorization", "Bearer " + invalidToken)
                .header("type", "Mr.Yoda-Admin")
                .get(endpoint)
                .then().extract().response();
    }

    // =========================================================
    // GET PHLEBO LIST  —  GET /api/v1/phlebo
    // =========================================================

    /**
     * GET /api/v1/phlebo with all supported query parameters.
     *
     * @param page      page number (1-based)
     * @param pageSize  number of records per page
     * @param isActive  filter: true = active phlebos only, false = inactive
     * @param type      filter type string, e.g. "Active"
     * @param search    free-text search (name / mobile); empty string = no filter
     * @param zoneId    zone ObjectId filter; empty string = no filter
     * @param token     Bearer token
     */
    public Response getPhleboList(int page, int pageSize, String isActive,
                                  String type, String search, String zoneId,
                                  String token) {
        return io.restassured.RestAssured.given()
                .relaxedHTTPSValidation()
                .log().all()
                .header("Authorization", "Bearer " + token)
                .queryParam("page",      page)
                .queryParam("pageSize",  pageSize)
                .queryParam("is_active", isActive)
                .queryParam("type",      type)
                .queryParam("search",    search)
                .queryParam("zone_id",   zoneId)
                .get(PhlebioEndpoints.GET_PHLEBO_LIST)
                .then().extract().response();
    }

    /**
     * GET /api/v1/phlebo/list — convenience overload using default search / zone_id (empty strings).
     */
    public Response getPhleboList(int page, int pageSize, String isActive,
                                  String type, String token) {
        return getPhleboList(page, pageSize, isActive, type, "", "", token);
    }

    // =========================================================
    // UPDATE PHLEBO  —  PUT /api/v1/phlebo
    // =========================================================

    /**
     * PUT /api/v1/phlebo — standard positive call (all three required fields).
     *
     * @param guid  Phlebo UUID identifying the record to update
     * @param name  New full name
     * @param email New email address
     * @param token Bearer token (phlebo JWT or admin JWT)
     */
    public Response updatePhlebo(String guid, String name, String email, String token) {
        org.json.JSONObject body = new org.json.JSONObject();
        body.put("guid",  guid);
        body.put("name",  name);
        body.put("email", email);
        return io.restassured.RestAssured.given()
                .relaxedHTTPSValidation()
                .log().all()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type",  "application/json")
                .header("type",          "Mr.Yoda-Admin")
                .body(body.toString())
                .put(PhlebioEndpoints.UPDATE_PHLEBO)
                .then().extract().response();
    }

    /**
     * PUT /api/v1/phlebo — raw body variant for negative testing.
     * Allows callers to omit, empty, or corrupt individual fields.
     *
     * @param body  JSONObject with any subset of fields
     * @param token Bearer token
     */
    public Response updatePhleboRaw(org.json.JSONObject body, String token) {
        return io.restassured.RestAssured.given()
                .relaxedHTTPSValidation()
                .log().all()
                .header("Authorization", "Bearer " + token)
                .header("Content-Type",  "application/json")
                .header("type",          "Mr.Yoda-Admin")
                .body(body.toString())
                .put(PhlebioEndpoints.UPDATE_PHLEBO)
                .then().extract().response();
    }
}
