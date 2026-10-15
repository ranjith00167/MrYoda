package com.mryoda.diagnostics.api.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable per-attempt evaluation: every expected test with its state plus counts. */
public final class ReportEvaluation {

    private final List<TestResult> results;

    public ReportEvaluation(List<TestResult> results) {
        this.results = Collections.unmodifiableList(new ArrayList<>(results));
    }

    public static ReportEvaluation empty() {
        return new ReportEvaluation(Collections.<TestResult>emptyList());
    }

    public List<TestResult> getResults() {
        return results;
    }

    public int total() {
        return results.size();
    }

    public int completedCount() {
        return (int) results.stream().filter(r -> r.getState().isCompleted()).count();
    }

    public int failedCount() {
        return (int) results.stream().filter(r -> r.getState() == ResultState.MISMATCH).count();
    }

    /** Not completed and not a value mismatch: still waiting for data. */
    public int pendingCount() {
        return total() - completedCount() - failedCount();
    }

    public int count(ResultState state) {
        return (int) results.stream().filter(r -> r.getState() == state).count();
    }

    public boolean isComplete() {
        return total() > 0 && completedCount() == total();
    }

    public List<TestResult> notCompleted() {
        List<TestResult> out = new ArrayList<>();
        for (TestResult r : results) {
            if (!r.getState().isCompleted()) {
                out.add(r);
            }
        }
        return out;
    }

    public String summary() {
        return completedCount() + "/" + total() + " completed, " + pendingCount() + " pending, "
                + failedCount() + " failed";
    }
}
