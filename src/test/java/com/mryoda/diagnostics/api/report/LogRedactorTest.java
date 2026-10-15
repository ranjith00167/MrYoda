package com.mryoda.diagnostics.api.report;

import org.testng.Assert;
import org.testng.annotations.Test;

public class LogRedactorTest {

    @Test
    public void masksBearerTokensAndJwts() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VyIjoiMSJ9.c2lnbmF0dXJl";
        String out = LogRedactor.redact("Authorization: Bearer " + jwt + " and raw " + jwt);

        Assert.assertFalse(out.contains(jwt), out);
        Assert.assertFalse(out.contains("eyJ"), out);
        Assert.assertTrue(out.contains("***"), out);
        Assert.assertEquals(LogRedactor.redact("Bearer " + jwt), "Bearer ***");
    }

    @Test
    public void masksSignedUrlParameters() {
        String out = LogRedactor.redact(
                "https://s3/x.pdf?X-Amz-Credential=AKIA123&X-Amz-Signature=deadbeef&X-Amz-Date=20260101");

        Assert.assertFalse(out.contains("deadbeef"), out);
        Assert.assertFalse(out.contains("AKIA123"), out);
        Assert.assertTrue(out.contains("x.pdf"), out);
    }

    @Test
    public void masksKeysTokensAndPersonalData() {
        String out = LogRedactor.redact(
                "encryption.key=abc123SECRET token: xyz789 mobile 9876543210 mail a.b@example.com");

        Assert.assertFalse(out.contains("abc123SECRET"), out);
        Assert.assertFalse(out.contains("xyz789"), out);
        Assert.assertFalse(out.contains("9876543210"), out);
        Assert.assertTrue(out.contains("******3210"), out);
        Assert.assertFalse(out.contains("example.com"), out);
    }

    @Test
    public void leavesOrdinaryDiagnosticsAlone() {
        String line = "[VIS-1] attempt 3/20 HTTP 404 in 10111ms";

        Assert.assertEquals(LogRedactor.redact(line), line);
        Assert.assertNull(LogRedactor.redact(null));
    }

    @Test
    public void stripQueryRemovesSignature() {
        Assert.assertEquals(LogRedactor.stripQuery("https://h/a/b.pdf?X-Amz-Signature=s"), "https://h/a/b.pdf");
    }
}
