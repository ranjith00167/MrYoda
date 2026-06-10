package api.phlebio;

/**
 * Phlebio API Endpoint Constants
 * Phlebo Notification Service Base URL:
 *   https://staging-api-phlebo-notification.yodadiagnostics.com
 * Diagnostics Service Base URL:
 *   https://staging-api-diagnostics.yodaprojects.com
 */
public class PhlebioEndpoints {

    private PhlebioEndpoints() {
        // Utility class — no instantiation
    }

    // ── Authentication (Phlebo Notification Service) ────────────────────────
    public static final String LOGIN            = "/api/v1/phlebo/login";
    public static final String LOGOUT           = "/api/v1/phlebo/logout";
    public static final String RESET_PASSWORD   = "/api/v1/phlebo/reset-password";
    public static final String UPDATE_PASSWORD  = "/api/v1/phlebo/password";
    public static final String SEND_LOGIN_OTP   = "/api/v1/phlebo/send-login-otp";
    public static final String LOGIN_WITH_OTP   = "/api/v1/phlebo/login-with-otp";

    // ── Shift Management (Phlebo Notification Service) ──────────────────────
    /** POST  — Assign one or more shifts to a list of phlebotomists (legacy path).
     *  Requires: admin Bearer token. */
    public static final String ASSIGN_SHIFTS    = "/api/v1/phlebo/assign-shifts";

    /** POST  — Assign shifts via the newer /shifts/assign path.
     *  Payload: { phlebo_guids[], from_date, to_date, slots[{start_time, end_time}] }
     *  Requires: admin Bearer token. */
    public static final String SHIFTS_ASSIGN    = "/api/v1/phlebo/shifts/assign";

    /** GET   — Fetch all shifts assigned to a specific phlebotomist.
     *  Path param {phlebo_guid} must be replaced before use.
     *  Requires: admin Bearer token. */
    public static final String GET_SHIFTS       = "/api/v1/phlebo/{phlebo_guid}/shifts";

    /** POST  — Delete (cancel) shifts for a phlebotomist within a date range.
     *  Path param {phlebo_guid} must be replaced before use.
     *  Payload: { from_date, to_date }  (yyyy-MM-dd)
     *  Requires: admin Bearer token. */
    public static final String DELETE_SHIFTS    = "/api/v1/phlebo/{phlebo_guid}/shifts/delete";

    /** POST  — Phlebotomist clocks in for their current shift.
     *  Requires: phlebo's own Bearer token. */
    public static final String CLOCK_IN         = "/api/v1/phlebo/shift/clock-in";

    // ── Registration (Phlebo Notification Service) ──────────────────────────
    /** POST  — Create (register) a new phlebotomist in the system.
     *  Requires: admin Bearer token. */
    public static final String SAVE_PHLEBO      = "/api/v1/phlebo/savePhlebo";

    /** GET   — Fetch a single phlebotomist by their UUID (guid).
     *  Path param {guid} must be replaced before use.
     *  Requires: admin Bearer token + type: Mr.Yoda-Admin header. */
    public static final String GET_PHLEBO_BY_GUID = "/api/v1/phlebo/{guid}";

    /** GET   — List phlebotomists with pagination, filtering, and search.
     *  Query params: page, pageSize, is_active, type, search, zone_id
     *  Requires: Bearer token. */
    public static final String GET_PHLEBO_LIST    = "/api/v1/phlebo/list";

    /** PUT   — Update an existing phlebotomist's profile (name, email, etc.).
     *  Body: { "guid": "<uuid>", "name": "<name>", "email": "<email>" }
     *  Requires: phlebo Bearer token (role=phlebo). */
    public static final String UPDATE_PHLEBO      = "/api/v1/phlebo";

    // ── Admin Authentication (Phlebo Notification Service) ─────────────────
    /** POST  — Admin login on the phlebo notification service.
     *  Payload: { identifier, password }  Returns admin Bearer token. */
    public static final String ADMIN_LOGIN      = "/api/v1/admin/login";

    /** POST  — Alternative admin login (some services use /auth/login). */
    public static final String AUTH_LOGIN       = "/api/v1/auth/login";
}
