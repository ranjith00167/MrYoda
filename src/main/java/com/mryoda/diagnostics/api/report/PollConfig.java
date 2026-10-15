package com.mryoda.diagnostics.api.report;

import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.config.ConfigManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Immutable polling policy. Resolution order per setting: JVM system property
 * ({@code -Dreport.poll.intervalMs=5000}), then {@code config.properties}, then the default.
 */
public final class PollConfig {

    private final long intervalMs;
    private final int maxAttempts;
    private final long overallTimeoutMs;
    private final int transientRetries;
    private final long transientBackoffMs;
    private final Set<Integer> transientStatuses;
    private final boolean acceptPdfOnly;
    private final int minPdfChars;
    private final int staleRecheckEvery;

    private PollConfig(Builder b) {
        this.intervalMs = b.intervalMs;
        this.maxAttempts = b.maxAttempts;
        this.overallTimeoutMs = b.overallTimeoutMs;
        this.transientRetries = b.transientRetries;
        this.transientBackoffMs = b.transientBackoffMs;
        this.transientStatuses = Collections.unmodifiableSet(new HashSet<>(b.transientStatuses));
        this.acceptPdfOnly = b.acceptPdfOnly;
        this.minPdfChars = b.minPdfChars;
        this.staleRecheckEvery = b.staleRecheckEvery;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Defaults match the previous COD_17 behaviour: 20 attempts x 15s, results accepted from the PDF. */
    public static PollConfig defaults() {
        return builder().build();
    }

    public static PollConfig fromConfig() {
        ConfigManager cfg = null;
        try {
            cfg = ConfigLoader.getConfig();
        } catch (RuntimeException ignored) {
            // config.properties unavailable (e.g. plain unit test) - fall back to defaults
        }
        Builder b = builder();
        b.intervalMs(longSetting("report.poll.intervalMs", cfg == null ? null : cfg.reportPollIntervalMs(), 15_000L));
        b.maxAttempts((int) longSetting("report.poll.maxAttempts", cfg == null ? null : (long) cfg.reportPollMaxAttempts(), 20L));
        b.overallTimeoutMs(longSetting("report.poll.timeoutMs", cfg == null ? null : cfg.reportPollTimeoutMs(), 600_000L));
        b.transientRetries((int) longSetting("report.poll.transientRetries", cfg == null ? null : (long) cfg.reportPollTransientRetries(), 3L));
        b.transientBackoffMs(longSetting("report.poll.transientBackoffMs", cfg == null ? null : cfg.reportPollTransientBackoffMs(), 2_000L));
        b.acceptPdfOnly(boolSetting("report.acceptPdfOnly", cfg == null ? null : cfg.reportAcceptPdfOnly(), true));
        b.minPdfChars((int) longSetting("report.pdf.minChars", cfg == null ? null : (long) cfg.reportPdfMinChars(), 20L));
        b.staleRecheckEvery((int) longSetting("report.pdf.recheckEvery", cfg == null ? null : (long) cfg.reportPdfRecheckEvery(), 3L));
        return b.build();
    }

    private static long longSetting(String key, Long configured, long fallback) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.trim().isEmpty()) {
            try {
                return Long.parseLong(sys.trim());
            } catch (NumberFormatException ignored) {
                // fall through to configured value
            }
        }
        return configured != null ? configured : fallback;
    }

    private static boolean boolSetting(String key, Boolean configured, boolean fallback) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.trim().isEmpty()) {
            return Boolean.parseBoolean(sys.trim());
        }
        return configured != null ? configured : fallback;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public long getOverallTimeoutMs() {
        return overallTimeoutMs;
    }

    public int getTransientRetries() {
        return transientRetries;
    }

    public long getTransientBackoffMs() {
        return transientBackoffMs;
    }

    public boolean isTransient(int httpStatus) {
        return transientStatuses.contains(httpStatus);
    }

    /** true: a value found in the PDF completes a test even if the API entry is still header-only. */
    public boolean isAcceptPdfOnly() {
        return acceptPdfOnly;
    }

    public int getMinPdfChars() {
        return minPdfChars;
    }

    /** While tests are pending, re-download an unchanged PDF every N attempts in case it was overwritten. */
    public int getStaleRecheckEvery() {
        return staleRecheckEvery;
    }

    public static final class Builder {
        private long intervalMs = 15_000L;
        private int maxAttempts = 20;
        private long overallTimeoutMs = 600_000L;
        private int transientRetries = 3;
        private long transientBackoffMs = 2_000L;
        private Set<Integer> transientStatuses = new HashSet<>(Arrays.asList(429, 500, 502, 503, 504));
        private boolean acceptPdfOnly = true;
        private int minPdfChars = 20;
        private int staleRecheckEvery = 3;

        public Builder intervalMs(long v) {
            this.intervalMs = Math.max(0, v);
            return this;
        }

        public Builder maxAttempts(int v) {
            this.maxAttempts = Math.max(1, v);
            return this;
        }

        public Builder overallTimeoutMs(long v) {
            this.overallTimeoutMs = Math.max(1, v);
            return this;
        }

        public Builder transientRetries(int v) {
            this.transientRetries = Math.max(0, v);
            return this;
        }

        public Builder transientBackoffMs(long v) {
            this.transientBackoffMs = Math.max(0, v);
            return this;
        }

        public Builder acceptPdfOnly(boolean v) {
            this.acceptPdfOnly = v;
            return this;
        }

        public Builder minPdfChars(int v) {
            this.minPdfChars = Math.max(1, v);
            return this;
        }

        public Builder staleRecheckEvery(int v) {
            this.staleRecheckEvery = Math.max(1, v);
            return this;
        }

        public PollConfig build() {
            return new PollConfig(this);
        }
    }
}
