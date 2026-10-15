package api.phlebio;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Field-level and cross-API validations for the authentication / shift part of the phlebo flow
 * (PHLEBO_MGMT_01 .. PHLEBO_MGMT_05). Every method is read-only and reports all mismatches together.
 */
public final class PhlebioAuthShiftValidator {

    private static final String[] PROFILE_FIELDS =
            {"name", "mobileNumber", "zoneId", "isActive", "employeeId", "employeeCode", "type"};

    private PhlebioAuthShiftValidator() {
    }

    // ── PHLEBO_MGMT_01 ──────────────────────────────────────────────────────

    /** Validates the phlebo login body and decodes the issued JWTs to cross-check the claims. */
    public static void validatePhleboLogin(Response response, String expectedMobile) {
        JsonPath body = response.jsonPath();
        FieldChecks c = new FieldChecks("PHLEBO_MGMT_01 login");

        c.isTrue("http status", response.getStatusCode() == 200 || response.getStatusCode() == 201,
                "expected 200/201 but was " + response.getStatusCode());
        c.isTrue("success", Boolean.TRUE.equals(body.get("success")), "success flag must be true");
        c.notBlank("message", firstNonBlank(body.getString("message"), body.getString("msg")));

        String guid = body.getString("data.phlebo.guid");
        c.uuid("data.phlebo.guid", guid);
        c.notBlank("data.phlebo.name", body.getString("data.phlebo.name"));
        c.eq("data.phlebo.mobileNumber == login mobile", expectedMobile, body.getString("data.phlebo.mobileNumber"));
        c.eq("data.phlebo.isActive", true, body.get("data.phlebo.isActive"));
        c.notBlank("data.phlebo.zoneId", body.getString("data.phlebo.zoneId"));
        c.notBlank("data.phlebo.employeeId", body.getString("data.phlebo.employeeId"));
        c.notBlank("data.phlebo.type", body.getString("data.phlebo.type"));

        String access = body.getString("data.access_token");
        String refresh = body.getString("data.refresh_token");
        c.isTrue("data.access_token is a JWT", isJwt(access), "access_token must have 3 JWT segments");
        c.isTrue("data.refresh_token is a JWT", isJwt(refresh), "refresh_token must have 3 JWT segments");
        c.isTrue("access_token != refresh_token", access != null && !access.equals(refresh),
                "access and refresh tokens must differ");

        if (isJwt(access)) {
            JSONObject claims = decodeClaims(access);
            c.eq("JWT.phlebo_id == data.phlebo.guid", guid, claims.optString("phlebo_id", null));
            c.eq("JWT.mobile == data.phlebo.mobileNumber", body.getString("data.phlebo.mobileNumber"),
                    claims.optString("mobile", null));
            c.eq("JWT.name == data.phlebo.name", body.getString("data.phlebo.name"), claims.optString("name", null));
            c.eq("JWT.role", "phlebo", claims.optString("role", null));
            c.eq("JWT.type", "ACCESS", claims.optString("type", null));
            long lifetime = claims.optLong("exp") - claims.optLong("iat");
            c.isTrue("JWT.exp > JWT.iat", lifetime > 0, "token lifetime must be positive but was " + lifetime);
            Object expiresIn = body.get("data.expires_in");
            if (expiresIn != null) {
                c.eq("data.expires_in == JWT.exp - JWT.iat", lifetime, expiresIn);
            }
        }
        if (isJwt(refresh)) {
            c.eq("refresh JWT.phlebo_id == data.phlebo.guid", guid, decodeClaims(refresh).optString("phlebo_id", null));
        }
        c.assertAll();
    }

    // ── PHLEBO_MGMT_02 ──────────────────────────────────────────────────────

    /** Validates the admin login body and cross-checks the JWT claims. */
    public static void validateAdminLogin(Response response, String adminIdentifier) {
        JsonPath body = response.jsonPath();
        FieldChecks c = new FieldChecks("PHLEBO_MGMT_02 admin login");

        c.isTrue("http status", response.getStatusCode() == 200 || response.getStatusCode() == 201,
                "expected 200/201 but was " + response.getStatusCode());
        c.isTrue("success", Boolean.TRUE.equals(body.get("success")), "success flag must be true");
        c.uuid("data.userdData.user_guid", body.getString("data.userdData.user_guid"));
        c.notBlank("data.userdData.user_name", body.getString("data.userdData.user_name"));
        c.notBlank("data.userdData.email", body.getString("data.userdData.email"));
        c.notBlank("data.userdData.role_id", body.getString("data.userdData.role_id"));
        c.notBlank("data.userType", body.getString("data.userType"));

        String token = body.getString("data.access_token");
        c.isTrue("data.access_token is a JWT", isJwt(token), "access_token must have 3 JWT segments");
        if (isJwt(token)) {
            JSONObject claims = decodeClaims(token);
            c.eq("JWT.mobile == admin identifier", adminIdentifier, claims.optString("mobile", null));
            c.eq("JWT.is_admin_user", true, claims.opt("is_admin_user"));
            c.notBlank("JWT.role", claims.optString("role", null));
            c.eq("JWT.type", "ACCESS", claims.optString("type", null));
            c.isTrue("JWT.exp > JWT.iat", claims.optLong("exp") > claims.optLong("iat"), "token lifetime must be positive");
        }
        c.assertAll();
    }

    // ── PHLEBO_MGMT_03 / 04 ─────────────────────────────────────────────────

    /**
     * Validates a get-shifts response: header fields vs. the login, internal totals, per-shift ids / zone /
     * status, and that today's 08:00-12:00 shift is present and ACTIVE.
     */
    public static void validateShifts(String scope, Response response, String phleboGuid, String loginName,
                                      String loginZoneId, String today, String startHhmm, String endHhmm) {
        JsonPath body = response.jsonPath();
        FieldChecks c = new FieldChecks(scope);

        c.eq("http status", 200, response.getStatusCode());
        c.isTrue("success", Boolean.TRUE.equals(body.get("success")), "success flag must be true");
        c.eq("data.phlebo_id == login guid", phleboGuid, body.getString("data.phlebo_id"));
        c.eq("data.phlebo_name == login name", loginName, body.getString("data.phlebo_name"));
        c.eq("data.zone_id == login zoneId", loginZoneId, body.getString("data.zone_id"));

        List<Map<String, Object>> days = body.getList("data.shifts_by_date");
        c.isTrue("data.shifts_by_date present", days != null && !days.isEmpty(), "must contain at least one date");
        if (days == null) {
            c.assertAll();
            return;
        }

        int shiftCount = 0;
        boolean todayShiftFound = false;
        for (Map<String, Object> day : days) {
            String date = str(day.get("date"));
            List<?> shifts = day.get("shifts") instanceof List ? (List<?>) day.get("shifts") : List.of();
            shiftCount += shifts.size();
            for (Object s : shifts) {
                Map<?, ?> shiftEntry = asMap(s);
                Map<?, ?> shiftMap = asMap(shiftEntry.get("shift_map"));
                Map<?, ?> shift = asMap(shiftMap.get("shift"));
                String label = "shift[" + date + "]";
                c.eq(label + ".shift_map.phleboId == login guid", phleboGuid, shiftMap.get("phleboId"));
                c.eq(label + ".shift_map.slotId == shift.guid", str(shift.get("guid")), shiftMap.get("slotId"));
                c.eq(label + ".shift.zoneId == login zoneId", loginZoneId, shift.get("zoneId"));
                c.isTrue(label + ".shift_map.shiftDate == date", str(shiftMap.get("shiftDate")).startsWith(date),
                        "shiftDate [" + shiftMap.get("shiftDate") + "] must start with [" + date + "]");
                c.isTrue(label + ".shift.shiftStartTime on " + date, str(shift.get("shiftStartTime")).startsWith(date),
                        "start [" + shift.get("shiftStartTime") + "] must be on " + date);
                c.isTrue(label + " end > start",
                        str(shift.get("shiftEndTime")).compareTo(str(shift.get("shiftStartTime"))) > 0,
                        "end [" + shift.get("shiftEndTime") + "] must be after start [" + shift.get("shiftStartTime") + "]");
                c.notBlank(label + ".shift.status", shift.get("status"));

                if (date.equals(today) && str(shift.get("shiftStartTime")).contains(" " + startHhmm)
                        && str(shift.get("shiftEndTime")).contains(" " + endHhmm)) {
                    todayShiftFound = true;
                    c.eqIgnoreCase("today's " + startHhmm + "-" + endHhmm + " shift.status", "ACTIVE", shift.get("status"));
                    c.eqIgnoreCase("today's shift_map.status", "ACTIVE", shiftMap.get("status"));
                }
            }
        }
        c.isTrue("today's " + startHhmm + "-" + endHhmm + " shift exists", todayShiftFound,
                "no shift for " + today + " " + startHhmm + "-" + endHhmm + " in the response");
        c.eq("data.total_shifts == sum(shifts)", shiftCount, body.get("data.total_shifts"));
        c.eq("data.total_dates == shifts_by_date.size()", days.size(), body.get("data.total_dates"));
        c.assertAll();
    }

    /** Cross-checks the admin-side phlebo profile against what the phlebo login returned. */
    public static void validateProfileMatchesLogin(Response loginResponse, Response profileResponse) {
        FieldChecks c = new FieldChecks("PHLEBO_MGMT_04 profile (GET phlebo by guid) vs login");
        c.eq("http status", 200, profileResponse.getStatusCode());
        Map<String, Object> login = loginResponse.jsonPath().getMap("data.phlebo");
        Map<String, Object> profile = profileResponse.jsonPath().getMap("data");
        if (profile != null && profile.get("phlebo") instanceof Map) {
            profile = asStringKeyMap(profile.get("phlebo"));
        }
        c.isTrue("profile data present", profile != null && !profile.isEmpty(), "profile response has no data");
        if (profile != null && login != null) {
            c.eq("guid", login.get("guid"), profile.get("guid"));
            for (String field : PROFILE_FIELDS) {
                if (profile.containsKey(field)) {
                    Object expectedValue = login.get(field);
                    Object actualValue = profile.get(field);
                    if ("type".equals(field)) {
                        // login returns a postgres-array literal "{phlebo}", the profile API a JSON array ["phlebo"]
                        c.defect("type has the same representation in login and profile", false,
                                "login=" + expectedValue + " profile=" + actualValue);
                        expectedValue = stripArrayChars(expectedValue);
                        actualValue = stripArrayChars(actualValue);
                    }
                    c.eq(field + " (login vs profile)", expectedValue, actualValue);
                } else {
                    c.note("profile response does not expose [" + field + "]");
                }
            }
        }
        c.assertAll();
    }

    // ── PHLEBO_MGMT_05 ──────────────────────────────────────────────────────

    /** Clock-in: success body, or a well-formed business rejection (already clocked in / no active shift). */
    public static void validateClockIn(Response response) {
        JsonPath body = response.jsonPath();
        FieldChecks c = new FieldChecks("PHLEBO_MGMT_05 clock-in");
        int status = response.getStatusCode();
        String message = firstNonBlank(body.getString("message"), body.getString("msg"));

        if (status == 200 || status == 201) {
            c.isTrue("success", Boolean.TRUE.equals(body.get("success")), "success flag must be true on 200/201");
            c.notBlank("message", message);
        } else {
            c.isTrue("business rejection status", status == 400 || status == 404 || status == 422,
                    "unexpected HTTP status " + status);
            c.notBlank("rejection message", message);
            Object success = body.get("success");
            c.isTrue("success is not true on rejection", !Boolean.TRUE.equals(success), "rejection must not report success");
            c.note("clock-in rejected as business state: " + message);
        }
        c.assertAll();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static boolean isJwt(String token) {
        return token != null && token.split("\\.").length == 3;
    }

    private static JSONObject decodeClaims(String jwt) {
        byte[] decoded = Base64.getUrlDecoder().decode(jwt.split("\\.")[1]);
        return new JSONObject(new String(decoded, StandardCharsets.UTF_8));
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isEmpty()) ? a : b;
    }

    private static String stripArrayChars(Object value) {
        return value == null ? null : String.valueOf(value).replaceAll("[\\[\\]{}\" ]", "");
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static Map<?, ?> asMap(Object value) {
        return value instanceof Map ? (Map<?, ?>) value : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asStringKeyMap(Object value) {
        return (Map<String, Object>) value;
    }
}
