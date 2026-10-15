package api.phlebio;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Collects field-level validation results for one API step so that EVERY mismatch is reported
 * together (instead of stopping at the first failing assertion).
 *
 * Usage: create one instance per test step, run the checks, then call {@link #assertAll()}.
 * Every check is also recorded in {@link ValidationLog}; by default {@link #assertAll()} does not throw and the
 * per-check validation tests report the outcome instead.
 */
public final class FieldChecks {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /** When true (-Dphlebo.failOnDefects=true) backend defects found by {@link #defect} also fail the step. */
    private static final boolean FAIL_ON_DEFECTS = Boolean.getBoolean("phlebo.failOnDefects");

    private final String scope;
    private final List<String> failures = new ArrayList<>();
    private final List<String> defects = new ArrayList<>();
    private int passed;

    public FieldChecks(String scope) {
        this.scope = scope;
        System.out.println("   --- " + scope + ": field & cross-API validation ---");
    }

    /** Actual value must equal expected (compared as trimmed strings; null equals null). */
    public FieldChecks eq(String field, Object expected, Object actual) {
        String e = normalise(expected);
        String a = normalise(actual);
        return record(Objects.equals(e, a), field,
                "expected [" + e + "] but was [" + a + "]", "= [" + a + "]", e, a);
    }

    public FieldChecks eqIgnoreCase(String field, Object expected, Object actual) {
        String e = normalise(expected);
        String a = normalise(actual);
        boolean ok = e == null ? a == null : e.equalsIgnoreCase(a);
        return record(ok, field, "expected [" + e + "] (ignore case) but was [" + a + "]", "= [" + a + "]", e, a);
    }

    /** Like {@link #eq} but skipped (with a note) when the expected value was never captured earlier in the flow. */
    public FieldChecks eqIfKnown(String field, Object expected, Object actual) {
        if (normalise(expected) == null) {
            return note(field + " skipped - expected value was not captured earlier in the flow");
        }
        return eq(field, expected, actual);
    }

    /** Two ISO-8601 timestamps (possibly with different offsets) must denote the same instant. */
    public FieldChecks sameInstant(String field, String expectedIso, String actualIso) {
        boolean ok;
        try {
            ok = OffsetDateTime.parse(expectedIso).toInstant().equals(OffsetDateTime.parse(actualIso).toInstant());
        } catch (RuntimeException e) {
            ok = false;
        }
        return record(ok, field, "expected instant [" + expectedIso + "] but was [" + actualIso + "]",
                "same instant [" + actualIso + "]", expectedIso, actualIso);
    }

    public FieldChecks notBlank(String field, Object actual) {
        String a = normalise(actual);
        return record(a != null && !a.isEmpty(), field, "must not be null/blank", "present [" + a + "]",
                "non-null, non-blank", a);
    }

    public FieldChecks isNull(String field, Object actual) {
        String a = normalise(actual);
        return record(a == null || a.isEmpty(), field, "expected null/empty but was [" + a + "]", "null/empty",
                "null/empty", a);
    }

    public FieldChecks isTrue(String field, boolean condition, String detail) {
        return record(condition, field, detail, detail);
    }

    public FieldChecks matches(String field, String regex, Object actual) {
        String a = normalise(actual);
        return record(a != null && Pattern.compile(regex).matcher(a).matches(), field,
                "[" + a + "] does not match /" + regex + "/", "[" + a + "] matches",
                "matches /" + regex + "/", a);
    }

    public FieldChecks uuid(String field, Object actual) {
        String a = normalise(actual);
        return record(a != null && UUID_PATTERN.matcher(a).matches(), field,
                "[" + a + "] is not a valid UUID", "valid UUID", "valid UUID", a);
    }

    public FieldChecks contains(String field, Object actual, String fragment) {
        String a = normalise(actual);
        return record(a != null && a.toLowerCase().contains(fragment.toLowerCase()), field,
                "[" + a + "] must contain [" + fragment + "]", "contains [" + fragment + "]",
                "contains [" + fragment + "]", a);
    }

    /** Records an informational (non-failing) observation, e.g. a security finding. */
    public FieldChecks note(String message) {
        System.out.println("      [NOTE] " + message);
        return this;
    }

    /** Generic hard check with explicit pass / fail messages. */
    public FieldChecks check(String field, boolean condition, String passMessage, String failMessage) {
        return record(condition, field, failMessage, passMessage);
    }

    /**
     * Soft check for a suspected BACKEND defect (stale/null/inconsistent data that the API under test returns).
     * Always printed as [DEFECT]; it only fails the step when -Dphlebo.failOnDefects=true.
     */
    public FieldChecks defect(String field, boolean condition, String defectMessage) {
        if (condition) {
            passed++;
            System.out.println("      [OK]   " + field);
            ValidationLog.add(scope, field, ValidationLog.Status.PASS, "consistent", null, null);
        } else {
            defects.add(field + " -> " + defectMessage);
            System.out.println("      [DEFECT] " + field + " -> " + defectMessage);
            ValidationLog.add(scope, field, ValidationLog.Status.DEFECT, defectMessage, null, null);
        }
        return this;
    }

    public List<String> failures() {
        return Collections.unmodifiableList(failures);
    }

    public List<String> defects() {
        return Collections.unmodifiableList(defects);
    }

    /** Throws one AssertionError listing every failed check (and defects when -Dphlebo.failOnDefects=true). */
    public void assertAll() {
        System.out.println("   --- " + scope + ": " + passed + " checks passed, " + failures.size() + " failed, "
                + defects.size() + " backend defect(s) ---");
        if (!ValidationLog.inlineAssert()) {
            return; // each check is reported as its own test by PhlebioFlowValidationTest
        }
        List<String> blocking = new ArrayList<>(failures);
        if (FAIL_ON_DEFECTS) {
            for (String d : defects) {
                blocking.add("[DEFECT] " + d);
            }
        }
        if (!blocking.isEmpty()) {
            StringBuilder sb = new StringBuilder(scope).append(": ").append(blocking.size())
                    .append(" validation(s) failed:");
            for (String f : blocking) {
                sb.append("\n  - ").append(f);
            }
            throw new AssertionError(sb.toString());
        }
    }

    private FieldChecks record(boolean ok, String field, String failMessage, String okMessage) {
        return record(ok, field, failMessage, okMessage, null, null);
    }

    private FieldChecks record(boolean ok, String field, String failMessage, String okMessage,
                               String expected, String actual) {
        if (ok) {
            passed++;
            System.out.println("      [OK]   " + field + " " + okMessage);
            ValidationLog.add(scope, field, ValidationLog.Status.PASS, okMessage, expected, actual);
        } else {
            failures.add(field + " -> " + failMessage);
            System.out.println("      [FAIL] " + field + " -> " + failMessage);
            ValidationLog.add(scope, field, ValidationLog.Status.FAIL, failMessage, expected, actual);
        }
        return this;
    }

    private static String normalise(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return "null".equals(s) ? null : s;
    }
}
