package com.mryoda.diagnostics.api.report;

/** Per-test evaluation outcome with the evidence (or lack of it) behind the state. */
public final class TestResult {

    private final ExpectedTest test;
    private final ResultState state;
    private final String value;
    private final String unit;
    private final String reason;

    public TestResult(ExpectedTest test, ResultState state, String value, String unit, String reason) {
        this.test = test;
        this.state = state;
        this.value = value;
        this.unit = unit;
        this.reason = reason;
    }

    public ExpectedTest getTest() {
        return test;
    }

    public ResultState getState() {
        return state;
    }

    /** Result value read from the PDF, or null when none was found. */
    public String getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    /** Human-readable explanation of the state; always populated. */
    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return test + " -> " + state
                + (value != null ? " value=" + value + (unit != null ? " " + unit : "") : "")
                + " (" + reason + ")";
    }
}
