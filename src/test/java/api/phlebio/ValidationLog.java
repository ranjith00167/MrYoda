package api.phlebio;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Run-wide record of every individual field/cross-API check executed by {@link FieldChecks}.
 * The flow steps only record; {@code PhlebioFlowValidationTest} replays each entry as its own test
 * so every validation shows up as a separate PASS / FAIL / SKIP (defect) line in the report.
 */
public final class ValidationLog {

    public enum Status { PASS, FAIL, DEFECT }

    /** One executed check. */
    public static final class Entry {
        private final String scope;
        private final String check;
        private final Status status;
        private final String detail;
        private final String expected;
        private final String actual;

        Entry(String scope, String check, Status status, String detail, String expected, String actual) {
            this.scope = scope;
            this.check = check;
            this.status = status;
            this.detail = detail;
            this.expected = expected;
            this.actual = actual;
        }

        /** Expected value / condition when the check compares two values; otherwise null. */
        public String expected() {
            return expected;
        }

        /** Actual value found when the check compares two values; otherwise null. */
        public String actual() {
            return actual;
        }

        public String scope() {
            return scope;
        }

        public String check() {
            return check;
        }

        public Status status() {
            return status;
        }

        public String detail() {
            return detail;
        }
    }

    private static final List<Entry> ENTRIES = new CopyOnWriteArrayList<>();

    private ValidationLog() {
    }

    /**
     * Default: checks are deferred to the per-check validation tests (the flow steps do not fail on them).
     * -Dphlebo.inlineAssert=true restores the old behaviour where a step fails on its own failed checks.
     */
    public static boolean inlineAssert() {
        return Boolean.getBoolean("phlebo.inlineAssert");
    }

    static void add(String scope, String check, Status status, String detail, String expected, String actual) {
        ENTRIES.add(new Entry(scope, check, status, detail, expected, actual));
    }

    /** Immutable snapshot of all checks recorded so far, in execution order. */
    public static List<Entry> snapshot() {
        return new ArrayList<>(ENTRIES);
    }
}
