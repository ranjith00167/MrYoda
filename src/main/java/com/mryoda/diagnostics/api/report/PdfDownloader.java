package com.mryoda.diagnostics.api.report;

/** Downloads one report PDF. Implementations must not throw; failures are reported in the result. */
public interface PdfDownloader {

    /** Result of one download. {@code bytes} is empty (never null) on failure. */
    final class DownloadResult {
        private final int status;
        private final byte[] bytes;
        private final String error;
        private final long elapsedMs;

        public DownloadResult(int status, byte[] bytes, String error, long elapsedMs) {
            this.status = status;
            this.bytes = bytes == null ? new byte[0] : bytes;
            this.error = error;
            this.elapsedMs = elapsedMs;
        }

        public int getStatus() {
            return status;
        }

        public byte[] getBytes() {
            return bytes;
        }

        /** Null when the download succeeded. */
        public String getError() {
            return error;
        }

        public long getElapsedMs() {
            return elapsedMs;
        }

        public boolean isSuccess() {
            return error == null && status == 200;
        }
    }

    DownloadResult download(String url);
}
