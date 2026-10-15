package com.mryoda.diagnostics.api.utils;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Unit tests for {@link CryptoUtil} — AES-256-GCM payload encryption/decryption.
 *
 * Payload format under test:
 *   base64( IV[12] + ciphertext + authTag[16] ) inside JSON: { "__enc": "<base64>" }
 * Key: Base64-encoded 32 bytes (config: encryption.key / NEXT_PUBLIC_PAYLOAD_ENCRYPTION_KEY).
 */
public class CryptoUtilTest {

    private static final String KEY_B64 = "jE3X6+7Jtn77Z0O/uHVh2V+F4jmbU/PwO5n2f6pW7Fw=";

    @AfterMethod
    public void tearDown() {
        CryptoUtil.resetDetection();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /** Independent AES-256-GCM encryptor (spec: 12-byte IV, 16-byte tag, no AAD). */
    private static String encryptReference(String plainText, String keyB64) throws Exception {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(Base64.getDecoder().decode(keyB64), "AES"),
                new GCMParameterSpec(128, iv));
        byte[] ctAndTag = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        byte[] payload = new byte[iv.length + ctAndTag.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(ctAndTag, 0, payload, iv.length, ctAndTag.length);
        return Base64.getEncoder().encodeToString(payload);
    }

    /** Flips CryptoUtil's internal "server speaks encrypted" flag (auto-mode state). */
    private static void markServerEncryptionDetected() throws Exception {
        Field f = CryptoUtil.class.getDeclaredField("serverEncryptionDetected");
        f.setAccessible(true);
        f.set(null, true);
    }

    // ── Tests ───────────────────────────────────────────────────────────────

    @Test
    public void keyIsConfigured() {
        Assert.assertTrue(CryptoUtil.isConfigured(),
                "AES-256 key must resolve (config encryption.key / NEXT_PUBLIC_PAYLOAD_ENCRYPTION_KEY)");
    }

    @Test
    public void decryptPayload_decryptsSpecFormat() throws Exception {
        String plain = "{\"mobile\":\"8056477884\",\"otp\":\"123456\"}";
        String enc = encryptReference(plain, KEY_B64);

        Assert.assertEquals(CryptoUtil.decryptPayload(enc), plain);
    }

    @Test
    public void decryptPayload_decryptsBase64EnvelopeField() throws Exception {
        String plain = "{\"status\":\"success\",\"orders\":[]}";
        String envelope = new JSONObject().put("__enc", encryptReference(plain, KEY_B64)).toString();

        Assert.assertEquals(CryptoUtil.decryptResponseBody(envelope), plain);
    }

    @Test
    public void decryptResponseBody_plainJsonPassesThroughUntouched() {
        String plain = "{\"status\":\"success\",\"data\":[1,2,3]}";
        Assert.assertEquals(CryptoUtil.decryptResponseBody(plain), plain,
                "Plain JSON must pass through without decryption");
    }

    @Test
    public void decryptResponseBody_plainJsonArrayPassesThroughUntouched() {
        String plain = "[{\"id\":1},{\"id\":2}]";
        Assert.assertEquals(CryptoUtil.decryptResponseBody(plain), plain);
    }

    @Test
    public void decryptResponseBody_nullAndBlankSafe() {
        Assert.assertNull(CryptoUtil.decryptResponseBody(null));
        Assert.assertEquals(CryptoUtil.decryptResponseBody(""), "");
    }

    @Test
    public void decryptResponseBody_garbageCiphertextReturnsOriginal() {
        String envelope = new JSONObject()
                .put("__enc", Base64.getEncoder().encodeToString("not-gcm-ciphertext".getBytes(StandardCharsets.UTF_8)))
                .toString();
        // Must not throw; un-decryptable payloads fall back to the raw body
        Assert.assertEquals(CryptoUtil.decryptResponseBody(envelope), envelope);
    }

    @Test
    public void roundTrip_encryptThenDecrypt() throws Exception {
        markServerEncryptionDetected(); // auto-mode: pretend the server already spoke encrypted

        String plain = "{\"mobile\":\"8220220227\",\"action\":\"sendOtp\"}";
        String envelope = CryptoUtil.encryptRequestBody(plain);

        Assert.assertNotEquals(envelope, plain, "Body should have been encrypted");
        JSONObject json = new JSONObject(envelope);
        Assert.assertTrue(json.has("__enc"), "Encrypted body must be wrapped in { \"__enc\": \"...\" }");

        // Payload layout: 12-byte IV + ciphertext + 16-byte tag
        byte[] decoded = Base64.getDecoder().decode(json.getString("__enc"));
        Assert.assertTrue(decoded.length >= 28, "Decoded payload must be at least IV(12) + tag(16) bytes");

        Assert.assertEquals(CryptoUtil.decryptResponseBody(envelope), plain);
        Assert.assertEquals(CryptoUtil.decryptPayload(json.getString("__enc")), plain);
    }

    @Test
    public void roundTrip_usesFreshIvPerMessage() throws Exception {
        markServerEncryptionDetected();

        String plain = "{\"same\":\"payload\"}";
        String enc1 = new JSONObject(CryptoUtil.encryptRequestBody(plain)).getString("__enc");
        String enc2 = new JSONObject(CryptoUtil.encryptRequestBody(plain)).getString("__enc");

        Assert.assertNotEquals(enc1, enc2, "Each message must use a fresh random IV");
    }
}