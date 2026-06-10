package api.phlebio;

import org.json.JSONObject;

/**
 * Phlebio Request Payload Builders
 * Centralises all request body construction for Phlebio Authentication APIs.
 *
 * Credentials used across all flows:
 *   Mobile   : 9360651932
 *   Password : 12345678
 */
public class PhlebioPayloads {

    // ── Credentials ──────────────────────────────────────────────────────────
    public static final String PHLEBO_MOBILE   = "9360651932";
    public static final String PHLEBO_PASSWORD = "12345678";
    public static final String FCM_TOKEN       = "test-fcm-token-12345";
    public static final String STATIC_OTP      = "123456";

    private PhlebioPayloads() {
        // Utility class — no instantiation
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    /**
     * Builds the request body for POST /api/v1/phlebo/login
     * {
     *   "mobile"   : "9360651932",
     *   "password" : "12345678",
     *   "token"    : "test-fcm-token-12345"
     * }
     */
    public static JSONObject buildLoginPayload() {
        JSONObject body = new JSONObject();
        body.put("mobile",   PHLEBO_MOBILE);
        body.put("password", PHLEBO_PASSWORD);
        body.put("token",    FCM_TOKEN);
        return body;
    }

    // ── Reset Password — Send OTP ─────────────────────────────────────────────
    /**
     * Builds the request body for POST /api/v1/phlebo/reset-password (step 1)
     * {
     *   "mobile_number" : "9360651932"
     * }
     */
    public static JSONObject buildResetPasswordSendOtpPayload() {
        JSONObject body = new JSONObject();
        body.put("mobile_number", PHLEBO_MOBILE);
        return body;
    }

    // ── Reset Password — Verify OTP ──────────────────────────────────────────
    /**
     * Builds the request body for POST /api/v1/phlebo/reset-password (step 2)
     * {
     *   "mobile_number" : "9360651932",
     *   "otpInput"      : "123456"
     * }
     */
    public static JSONObject buildResetPasswordVerifyOtpPayload(String otp) {
        JSONObject body = new JSONObject();
        body.put("mobile_number", PHLEBO_MOBILE);
        body.put("otpInput",      otp);
        return body;
    }

    // -- Update Password -------------------------------------------------------
    /**
     * Builds the request body for PUT /api/v1/phlebo/password
     * {
     *   "mobile"   : "9360651932",
     *   "password" : "12345678"
     * }
     */
    public static JSONObject buildUpdatePasswordPayload() {
        JSONObject body = new JSONObject();
        body.put("mobile",   PHLEBO_MOBILE);
        body.put("password", PHLEBO_PASSWORD);
        return body;
    }

    // -- Send Login OTP -------------------------------------------------------
    /**
     * Builds the request body for POST /api/v1/phlebo/send-login-otp
     * { "mobile": "9360651932" }
     */
    public static JSONObject buildSendLoginOtpPayload() {
        JSONObject body = new JSONObject();
        body.put("mobile", PHLEBO_MOBILE);
        return body;
    }

    // -- Login With OTP -------------------------------------------------------
    /**
     * Builds the request body for POST /api/v1/phlebo/login-with-otp
     * {
     *   "mobile" : "9360651932",
     *   "otp"    : "123456",
     *   "token"  : "fcm_token"
     * }
     */
    public static JSONObject buildLoginWithOtpPayload(String otp) {
        JSONObject body = new JSONObject();
        body.put("mobile", PHLEBO_MOBILE);
        body.put("otp",    otp);
        body.put("token",  FCM_TOKEN);
        return body;
    }

    // =========================================================
    // NEGATIVE PAYLOAD BUILDERS
    // =========================================================

    // -- Login Negative -------------------------------------------------------
    public static JSONObject buildLoginWrongPasswordPayload() {
        JSONObject body = new JSONObject();
        body.put("mobile",   PHLEBO_MOBILE);
        body.put("password", "WrongPass@999");
        body.put("token",    FCM_TOKEN);
        return body;
    }

    public static JSONObject buildLoginUnregisteredMobilePayload() {
        JSONObject body = new JSONObject();
        body.put("mobile",   "0000000000");
        body.put("password", PHLEBO_PASSWORD);
        body.put("token",    FCM_TOKEN);
        return body;
    }

    public static JSONObject buildLoginEmptyMobilePayload() {
        JSONObject body = new JSONObject();
        body.put("mobile",   "");
        body.put("password", PHLEBO_PASSWORD);
        body.put("token",    FCM_TOKEN);
        return body;
    }

    public static JSONObject buildLoginEmptyPasswordPayload() {
        JSONObject body = new JSONObject();
        body.put("mobile",   PHLEBO_MOBILE);
        body.put("password", "");
        body.put("token",    FCM_TOKEN);
        return body;
    }

    // -- Reset Password Send OTP Negative -------------------------------------
    public static JSONObject buildResetOtpUnregisteredMobilePayload() {
        JSONObject body = new JSONObject();
        body.put("mobile_number", "0000000000");
        return body;
    }

    public static JSONObject buildResetOtpEmptyMobilePayload() {
        JSONObject body = new JSONObject();
        body.put("mobile_number", "");
        return body;
    }

    public static JSONObject buildResetOtpInvalidMobileFormatPayload() {
        JSONObject body = new JSONObject();
        body.put("mobile_number", "ABCD1234");
        return body;
    }

    // -- Update Password Negative ---------------------------------------------
    public static JSONObject buildUpdatePasswordEmptyPasswordPayload() {
        JSONObject body = new JSONObject();
        body.put("mobile",   PHLEBO_MOBILE);
        body.put("password", "");
        return body;
    }

    public static JSONObject buildUpdatePasswordUnregisteredMobilePayload() {
        JSONObject body = new JSONObject();
        body.put("mobile",   "0000000000");
        body.put("password", "SomePass@123");
        return body;
    }

    // -- Send Login OTP Negative ----------------------------------------------
    public static JSONObject buildSendLoginOtpUnregisteredMobilePayload() {
        JSONObject body = new JSONObject();
        body.put("mobile", "0000000000");
        return body;
    }

    public static JSONObject buildSendLoginOtpEmptyMobilePayload() {
        JSONObject body = new JSONObject();
        body.put("mobile", "");
        return body;
    }

    // -- Login With OTP Negative ----------------------------------------------
    public static JSONObject buildLoginWithWrongOtpPayload() {
        JSONObject body = new JSONObject();
        body.put("mobile", PHLEBO_MOBILE);
        body.put("otp",    "000000");
        body.put("token",  FCM_TOKEN);
        return body;
    }

    public static JSONObject buildLoginWithEmptyOtpPayload() {
        JSONObject body = new JSONObject();
        body.put("mobile", PHLEBO_MOBILE);
        body.put("otp",    "");
        body.put("token",  FCM_TOKEN);
        return body;
    }

    // -- Login Boundary -------------------------------------------------------
    /** Mobile shorter than 10 digits — boundary length validation. */
    public static JSONObject buildLoginShortMobilePayload() {
        JSONObject body = new JSONObject();
        body.put("mobile",   "12345");
        body.put("password", PHLEBO_PASSWORD);
        body.put("token",    FCM_TOKEN);
        return body;
    }

    // -- Update Password Additional Negative ----------------------------------
    /** Empty mobile field for update password. */
    public static JSONObject buildUpdatePasswordEmptyMobilePayload() {
        JSONObject body = new JSONObject();
        body.put("mobile",   "");
        body.put("password", PHLEBO_PASSWORD);
        return body;
    }

    // -- Send Login OTP Boundary ----------------------------------------------
    /** Non-numeric mobile format for send-login-otp (e.g., ABCD1234). */
    public static JSONObject buildSendLoginOtpInvalidFormatPayload() {
        JSONObject body = new JSONObject();
        body.put("mobile", "ABCD1234");
        return body;
    }

    // -- Login With OTP Additional Negative -----------------------------------
    /** Unregistered mobile for OTP login. */
    public static JSONObject buildLoginWithOtpUnregisteredMobilePayload() {
        JSONObject body = new JSONObject();
        body.put("mobile", "0000000000");
        body.put("otp",    STATIC_OTP);
        body.put("token",  FCM_TOKEN);
        return body;
    }

    /** Empty mobile field for OTP login. */
    public static JSONObject buildLoginWithOtpEmptyMobilePayload() {
        JSONObject body = new JSONObject();
        body.put("mobile", "");
        body.put("otp",    STATIC_OTP);
        body.put("token",  FCM_TOKEN);
        return body;
    }

    // =========================================================
    // SHIFT MANAGEMENT PAYLOAD BUILDERS
    // =========================================================

    /**
     * Builds the request body for POST /api/v1/phlebo/assign-shifts (positive).
     * Assigns an 08:00–12:00 shift to the given phlebo GUID for today.
     * {
     *   "phlebo_guids" : ["<guid>"],
     *   "from_date"    : "yyyy-MM-dd",
     *   "to_date"      : "yyyy-MM-dd",
     *   "slots"        : [{"start_time": "08:00", "end_time": "12:00"}]
     * }
     */
    public static java.util.Map<String, Object> buildAssignShiftsPayload(String phleboGuid) {
        String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return buildAssignShiftsPayload(phleboGuid, today, today, "08:00", "12:00");
    }

    /**
     * Builds a fully parameterised assign-shifts payload.
     */
    public static java.util.Map<String, Object> buildAssignShiftsPayload(
            String phleboGuid, String fromDate, String toDate,
            String slotStartTime, String slotEndTime) {

        java.util.Map<String, Object> slot = new java.util.HashMap<>();
        slot.put("start_time", slotStartTime);
        slot.put("end_time",   slotEndTime);

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("phlebo_guids", java.util.Collections.singletonList(phleboGuid));
        payload.put("from_date",    fromDate);
        payload.put("to_date",      toDate);
        payload.put("slots",        java.util.Collections.singletonList(slot));
        return payload;
    }

    // -- Assign Shifts Negative -----------------------------------------------

    /** Empty phlebo_guids list — should be rejected with 4xx. */
    public static java.util.Map<String, Object> buildAssignShiftsEmptyGuidsPayload() {
        String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        java.util.Map<String, Object> slot = new java.util.HashMap<>();
        slot.put("start_time", "08:00");
        slot.put("end_time",   "12:00");

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("phlebo_guids", java.util.Collections.emptyList());
        payload.put("from_date",    today);
        payload.put("to_date",      today);
        payload.put("slots",        java.util.Collections.singletonList(slot));
        return payload;
    }

    /** Malformed date format — should be rejected with 4xx. */
    public static java.util.Map<String, Object> buildAssignShiftsBadDatePayload(String phleboGuid) {
        java.util.Map<String, Object> slot = new java.util.HashMap<>();
        slot.put("start_time", "08:00");
        slot.put("end_time",   "12:00");

        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("phlebo_guids", java.util.Collections.singletonList(phleboGuid));
        payload.put("from_date",    "99-99-9999");   // invalid date format
        payload.put("to_date",      "99-99-9999");
        payload.put("slots",        java.util.Collections.singletonList(slot));
        return payload;
    }

    // ── Clock In Payload Builders ────────────────────────────────────────────

    /**
     * Builds the request body for POST /api/v1/phlebo/shift/clock-in (positive).
     * Uses Hyderabad coordinates as the clock-in location.
     * {
     *   "image_url" : "https://example.com/test-selfie.jpg",
     *   "lat"       : 17.3850,
     *   "lng"       : 78.4867
     * }
     */
    public static java.util.Map<String, Object> buildClockInPayload() {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("image_url", "https://example.com/test-selfie.jpg");
        payload.put("lat",       17.3850);
        payload.put("lng",       78.4867);
        return payload;
    }

    /** Clock-in with missing image_url — negative boundary test. */
    public static java.util.Map<String, Object> buildClockInMissingImagePayload() {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("lat", 17.3850);
        payload.put("lng", 78.4867);
        return payload;
    }

    // =========================================================
    // CREATE PHLEBO (savePhlebo) PAYLOAD BUILDERS
    // =========================================================

    /**
     * Builds the request body for POST /api/v1/phlebo/savePhlebo.
     * mobileNumber, password, and email are generated randomly so each test
     * run creates a unique phlebotomist and avoids duplicate-key errors.
     *
     * Example:
     * {
     *   "name"           : "Test Phlebo",
     *   "mobileNumber"   : "8734291056",
     *   "password"       : "Test@4829",
     *   "zoneId"         : "69044ecd1871dcbd7c96c86f",
     *   "email"          : "phlebo_1717000000000@testmail.com",
     *   "isActive"       : true,
     *   "type"           : ["phlebo"],
     *   "home_address"   : "Test Address",
     *   "home_latitude"  : "17.3850",
     *   "home_longitude" : "78.4867"
     * }
     */
    public static JSONObject buildSavePhleboPayload() {
        return buildSavePhleboPayload(
                "69044ecd1871dcbd7c96c86f",  // default staging zoneId
                "Test Address",
                "17.3850",
                "78.4867"
        );
    }

    /**
     * Parameterised builder — callers can supply zone, address, and coords.
     * mobileNumber, password, and email are always randomised.
     */
    public static JSONObject buildSavePhleboPayload(
            String zoneId, String homeAddress,
            String homeLatitude, String homeLongitude) {

        // Random Indian mobile: 7xxxxxxxxx – 9xxxxxxxxx
        String mobileNumber = randomMobile();

        // Random password: uppercase letter + lowercase + special char + 4-digit suffix
        String password = randomPassword();

        // Unique email keyed to the current millisecond timestamp
        String email = "phlebo_" + System.currentTimeMillis() + "@testmail.com";

        org.json.JSONArray typeArray = new org.json.JSONArray();
        typeArray.put("phlebo");

        JSONObject body = new JSONObject();
        body.put("name",           "Test Phlebo");
        body.put("mobileNumber",   mobileNumber);
        body.put("password",       password);
        body.put("zoneId",         zoneId);
        body.put("email",          email);
        body.put("isActive",       true);
        body.put("type",           typeArray);
        body.put("home_address",   homeAddress);
        body.put("home_latitude",  homeLatitude);
        body.put("home_longitude", homeLongitude);
        return body;
    }

    // =========================================================
    // SAVE PHLEBO — FORM DATA BUILDERS
    // =========================================================

    /**
     * Builds form-data parameters for POST /api/v1/phlebo/savePhlebo.
     *
     * Field names match exactly what the browser sends (confirmed via Network tab):
     *   mobile_number  (not mobileNumber)
     *   zone_id        (not zoneId)
     *   type[]         (array notation)
     *
     * mobileNumber and password are randomised on every call to avoid duplicate-key
     * errors on the staging server.
     *
     * Returns a LinkedHashMap so iteration order is deterministic for logging.
     */
    public static java.util.Map<String, String> buildSavePhleboFormParams() {
        return buildSavePhleboFormParams(
                "676e886d854c2069a0d47703",                         // zone_id from staging
                "Test Address, Hyderabad",
                "17.3850",
                "78.4867"
        );
    }

    /**
     * Parameterised form-data builder — callers supply zone, address, and coordinates.
     * mobile_number and password are always randomised.
     */
    public static java.util.Map<String, String> buildSavePhleboFormParams(
            String zoneId, String homeAddress,
            String homeLatitude, String homeLongitude) {

        String mobile   = randomMobile();
        String password = randomPassword();
        String name     = randomName();

        java.util.Map<String, String> params = new java.util.LinkedHashMap<>();
        params.put("name",           name);
        params.put("mobile_number",  mobile);       // correct field name (not mobileNumber)
        params.put("password",       password);
        params.put("zone_id",        zoneId);        // correct field name (not zoneId)
        params.put("home_address",   homeAddress);
        params.put("home_latitude",  homeLatitude);
        params.put("home_longitude", homeLongitude);
        params.put("type[]",         "phlebo");      // array field notation
        return params;
    }

    // ── Random data helpers ───────────────────────────────────────────────────

    /**     * Returns a random realistic full name (first + last).
     * Picks from a pool of common Indian first and last names.
     * Example: "Arjun Sharma", "Priya Nair", "Ravi Patel"
     */
    public static String randomName() {
        String[] firstNames = {
            "Arjun", "Priya", "Ravi", "Sneha", "Kiran",
            "Deepa", "Vijay", "Anita", "Suresh", "Meena",
            "Rahul", "Pooja", "Arun", "Kavita", "Manoj",
            "Sunita", "Amit", "Rekha", "Sanjay", "Divya"
        };
        String[] lastNames = {
            "Sharma", "Patel", "Nair", "Reddy", "Kumar",
            "Singh", "Mehta", "Iyer", "Joshi", "Pillai",
            "Verma", "Gupta", "Das", "Rao", "Naik"
        };
        java.util.concurrent.ThreadLocalRandom rng = java.util.concurrent.ThreadLocalRandom.current();
        return firstNames[rng.nextInt(firstNames.length)] + " "
                + lastNames[rng.nextInt(lastNames.length)];
    }

    /**     * Returns a random 10-digit Indian mobile number (starts with 7, 8, or 9).
     * Uses System.nanoTime seeded via ThreadLocalRandom for uniqueness.
     */
    public static String randomMobile() {
        java.util.concurrent.ThreadLocalRandom rng = java.util.concurrent.ThreadLocalRandom.current();
        // prefix: 7, 8, or 9
        int prefix = 7 + rng.nextInt(3);
        // remaining 9 digits
        long suffix = rng.nextLong(100_000_000L, 1_000_000_000L);
        return prefix + String.valueOf(suffix);
    }

    /**
     * Returns a random password satisfying common complexity rules:
     *   - Starts with an uppercase letter
     *   - Contains a lowercase segment
     *   - Ends with @ + 4 random digits
     * Example: "Txqm@3847"
     */
    public static String randomPassword() {
        java.util.concurrent.ThreadLocalRandom rng = java.util.concurrent.ThreadLocalRandom.current();
        String uppers  = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowers  = "abcdefghijklmnopqrstuvwxyz";
        char upper = uppers.charAt(rng.nextInt(uppers.length()));
        char l1    = lowers.charAt(rng.nextInt(lowers.length()));
        char l2    = lowers.charAt(rng.nextInt(lowers.length()));
        char l3    = lowers.charAt(rng.nextInt(lowers.length()));
        int  digits = rng.nextInt(1000, 9999);
        return "" + upper + l1 + l2 + l3 + "@" + digits;
    }
}
