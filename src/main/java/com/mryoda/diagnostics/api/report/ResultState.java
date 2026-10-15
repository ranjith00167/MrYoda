package com.mryoda.diagnostics.api.report;

/** Synchronization state of one expected test in a report. */
public enum ResultState {
    /** A result value is in the PDF and the API marks the test as non-header (completed). */
    SYNCED(true),
    /** A result value is in the PDF but the API entry is still header-only (metadata lag). */
    PDF_ONLY(true),
    /** Package / panel header found by name; no value is required for it. */
    GROUP_PRESENT(true),
    /** The name or code is in the PDF but no result value could be read next to it. */
    NAME_ONLY(false),
    /** Neither name, code nor alias was found in the PDF. */
    NOT_FOUND(false),
    /** A value was found but differs from the expected business value. */
    MISMATCH(false);

    private final boolean completed;

    ResultState(boolean completed) {
        this.completed = completed;
    }

    public boolean isCompleted() {
        return completed;
    }
}
