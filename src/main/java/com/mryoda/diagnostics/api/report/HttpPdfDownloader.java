package com.mryoda.diagnostics.api.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.function.Supplier;

/** Downloads report PDFs over HTTP(S). Pre-signed S3 URLs are fetched without an Authorization header. */
public class HttpPdfDownloader implements PdfDownloader {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int MAX_BYTES = 30 * 1024 * 1024;

    private final Supplier<String> tokenSupplier;

    public HttpPdfDownloader(Supplier<String> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    @Override
    public DownloadResult download(String url) {
        long started = System.currentTimeMillis();
        if (url == null || url.trim().isEmpty()) {
            return new DownloadResult(0, null, "no URL", 0);
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            // A pre-signed S3 URL carries its auth in the query string; an extra header breaks the signature.
            boolean presigned = url.contains("amazonaws.com") || url.contains("X-Amz-Signature");
            String token = tokenSupplier == null ? null : tokenSupplier.get();
            if (!presigned && token != null && !token.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }
            int status = connection.getResponseCode();
            if (status != 200) {
                String hint = presigned && (status == 403 || status == 404)
                        ? " (pre-signed URL expired, no S3 permission, or object not uploaded yet)" : "";
                return new DownloadResult(status, null, "HTTP " + status + hint,
                        System.currentTimeMillis() - started);
            }
            return new DownloadResult(status, readAll(connection.getInputStream()), null,
                    System.currentTimeMillis() - started);
        } catch (SocketTimeoutException e) {
            return new DownloadResult(0, null, "timeout downloading PDF", System.currentTimeMillis() - started);
        } catch (IOException | RuntimeException e) {
            return new DownloadResult(0, null, e.getClass().getSimpleName() + ": " + e.getMessage(),
                    System.currentTimeMillis() - started);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        try (InputStream stream = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                if (out.size() > MAX_BYTES) {
                    throw new IOException("PDF larger than " + MAX_BYTES + " bytes");
                }
            }
            return out.toByteArray();
        }
    }
}
