package com.mryoda.diagnostics.api.utils;

import com.mryoda.diagnostics.api.config.ConfigLoader;
import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM encryption / decryption utility.
 *
 * Payload format (both request and response):
 *   base64( IV[12 bytes] + ciphertext + authTag[16 bytes] )
 *   sent / received inside JSON: { "__enc": "<base64>" }
 *
 * Key material:
 *   encryption.key in config.properties is a Base64-encoded 32-byte AES-256 key.
 *   e.g. NEXT_PUBLIC_PAYLOAD_ENCRYPTION_KEY=jE3X6+7Jtn77Z0O/uHVh2V+F4jmbU/PwO5n2f6pW7Fw=
 *
 * Algorithm: AES/GCM/NoPadding, no AAD, 128-bit tag, 12-byte random IV per message.
 *
 * Header: every encrypted request must carry  X-Encrypted: true
 *         (added automatically by RequestBuilder when shouldEncryptRequest() == true).
 *
 * encryption.mode:
 *   off  — plain staging, no crypto
 *   on   — always encrypt requests; responses decrypted when encrypted
 *   auto — responses auto-detected; requests encrypted after first encrypted response seen
 */
public final class CryptoUtil {

    // ── AES-256-GCM constants (fixed by spec) ──────────────────────────────
    private static final String  TRANSFORMATION       = "AES/GCM/NoPadding";
    private static final int     GCM_TAG_BITS         = 128;   // 16 bytes
    private static final int     GCM_TAG_BYTES        = GCM_TAG_BITS / 8;
    private static final int     IV_BYTES             = 12;    // 96-bit nonce
    private static final String  ENVELOPE_FIELD       = "__enc"; // JSON key used by the API

    private static volatile boolean serverEncryptionDetected = false;

    private CryptoUtil() {
    }

    // ── Mode / state ────────────────────────────────────────────────────────
    private static String mode() {
        try {
            String m = ConfigLoader.getConfig().encryptionMode();
            return m == null ? "off" : m.trim().toLowerCase();
        } catch (Throwable ignored) {
            return "off";
        }
    }

    /**
     * Returns true when a valid Base64 AES-256 key (32 decoded bytes) is present in config.
     */
    public static boolean isConfigured() {
        try {
            getSecretKey(); // will throw if key is absent / malformed
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean shouldEncryptRequest() {
        if (!isConfigured()) return false;
        switch (mode()) {
            case "on":   return true;
            case "auto": return serverEncryptionDetected;
            default:     return false;
        }
    }

    private static boolean shouldTryDecrypt() {
        return !"off".equals(mode()) && isConfigured();
    }

    public static void resetDetection() {
        serverEncryptionDetected = false;
    }

    // ── Request side ────────────────────────────────────────────────────────

    /**
     * Encrypts plainBody and wraps it in { "__enc": "<base64>" }.
     * Returns plainBody unchanged when encryption is not active.
     */
    public static String encryptRequestBody(String plainBody) {
        if (plainBody == null || plainBody.isBlank() || !shouldEncryptRequest()) {
            return plainBody;
        }
        try {
            String encoded = encryptToBase64(plainBody);
            return new JSONObject().put(ENVELOPE_FIELD, encoded).toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encrypt request body: " + e.getMessage(), e);
        }
    }

    // ── Response side ────────────────────────────────────────────────────────

    /**
     * Decrypts the response body when it contains { "__enc": "..." }.
     * Returns the original body unchanged for plain-JSON responses.
     */
    public static String decryptResponseBody(String responseBody) {
        if (responseBody == null || responseBody.isBlank() || !shouldTryDecrypt()) {
            return responseBody;
        }

        String trimmed = responseBody.trim();

        if (trimmed.startsWith("{")) {
            try {
                JSONObject json = new JSONObject(trimmed);

                // ── Primary: spec-defined envelope field "__enc" ────────────
                if (json.opt(ENVELOPE_FIELD) instanceof String) {
                    String plain = tryDecrypt(json.getString(ENVELOPE_FIELD));
                    if (plain != null) {
                        serverEncryptionDetected = true;
                        System.out.println("   [CryptoUtil] Decrypted response via '__enc' field.");
                        return plain;
                    }
                }

                // ── Fallback: scan other common envelope names ──────────────
                String[] fallbackFields = {
                    "encrypted_data", "encryptedData", "encrypted", "payload", "cipherText", "cipher"
                };
                for (String field : fallbackFields) {
                    if (json.opt(field) instanceof String) {
                        String plain = tryDecrypt(json.getString(field));
                        if (plain != null) {
                            serverEncryptionDetected = true;
                            System.out.println("   [CryptoUtil] Decrypted response via '" + field + "' field.");
                            return plain;
                        }
                    }
                }

                return responseBody; // plain JSON object
            } catch (Exception ignored) {
                // not parseable JSON — fall through to raw attempt
            }
        } else if (trimmed.startsWith("[")) {
            return responseBody; // plain JSON array
        }

        // ── Last resort: try decrypting the whole string as raw base64 ──────
        String plain = tryDecrypt(trimmed);
        if (plain != null) {
            serverEncryptionDetected = true;
            return plain;
        }
        return responseBody;
    }

    /**
     * Strict decrypt: base64-decode → split IV|ciphertext|tag → AES-256-GCM decrypt.
     * Throws {@link IllegalStateException} if decryption fails for any reason.
     */
    public static String decryptPayload(String base64Payload) {
        if (base64Payload == null || base64Payload.isBlank()) {
            return base64Payload;
        }
        try {
            // 1. Base64-decode the full payload: IV[12] + ciphertext + tag[16]
            byte[] decoded = Base64.getDecoder().decode(stripWrappingQuotes(base64Payload.trim()));

            if (decoded.length < IV_BYTES + GCM_TAG_BYTES) {
                throw new IllegalArgumentException(
                        "Decoded payload too short (" + decoded.length + " bytes); "
                        + "expected at least " + (IV_BYTES + GCM_TAG_BYTES) + " bytes.");
            }

            // 2. Extract IV — first 12 bytes
            byte[] iv = Arrays.copyOfRange(decoded, 0, IV_BYTES);

            // 3. The rest is ciphertext + appended 16-byte GCM auth tag
            byte[] cipherTextWithTag = Arrays.copyOfRange(decoded, IV_BYTES, decoded.length);

            // 4. Decrypt (Java GCM automatically validates + strips the tag)
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE,
                    getSecretKey(),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plainBytes = cipher.doFinal(cipherTextWithTag);

            return new String(plainBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new IllegalStateException("Unable to decrypt payload: " + e.getMessage(), e);
        }
    }

    private static String tryDecrypt(String candidate) {
        try {
            return decryptPayload(candidate);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * AES-256-GCM encrypt.
     * Format: base64( randomIV[12] + ciphertext + authTag[16] )
     */
    private static String encryptToBase64(String plainText) throws Exception {
        // 1. Generate a fresh random 12-byte IV for every message
        byte[] iv = new byte[IV_BYTES];
        new SecureRandom().nextBytes(iv);

        // 2. Encrypt
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE,
                getSecretKey(),
                new GCMParameterSpec(GCM_TAG_BITS, iv));
        // Java appends the 16-byte GCM tag to the end of the output automatically
        byte[] cipherTextWithTag = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // 3. Concatenate: IV + ciphertext + tag  (tag is already at the end)
        byte[] payload = new byte[IV_BYTES + cipherTextWithTag.length];
        System.arraycopy(iv,              0, payload, 0,         IV_BYTES);
        System.arraycopy(cipherTextWithTag, 0, payload, IV_BYTES, cipherTextWithTag.length);

        return Base64.getEncoder().encodeToString(payload);
    }

    // ── Key material ─────────────────────────────────────────────────────────

    /**
     * Loads the AES-256 key.
     * Resolution order:
     *   1. system property  encryption.key
     *   2. env var          NEXT_PUBLIC_PAYLOAD_ENCRYPTION_KEY
     *   3. config property  encryption.key
     * The resolved value must be Base64-encoded and decode to exactly 32 bytes
     * (e.g. jE3X6+7Jtn77Z0O/uHVh2V+F4jmbU/PwO5n2f6pW7Fw=).
     */
    private static SecretKey getSecretKey() {
        String secret = resolveKeyMaterial();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Missing encryption key. Set the NEXT_PUBLIC_PAYLOAD_ENCRYPTION_KEY env var, "
                    + "the encryption.key system property, or encryption.key in config.properties "
                    + "to the Base64-encoded 32-byte AES-256 key.");
        }
        try {
            // Key is Base64-encoded — decode to get the raw 32 bytes
            byte[] keyBytes = Base64.getDecoder().decode(secret.trim());
            if (keyBytes.length != 32) {
                throw new IllegalStateException(
                        "Encryption key must decode to exactly 32 bytes for AES-256. "
                        + "Got " + keyBytes.length + " bytes after Base64 decoding.");
            }
            return new SecretKeySpec(keyBytes, "AES");
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Encryption key is not valid Base64: " + e.getMessage(), e);
        }
    }

    /**
     * Resolves the Base64 key material from system property, environment variable,
     * or config — skipping blanks and obvious placeholder values such as the literal
     * env-var name "NEXT_PUBLIC_PAYLOAD_ENCRYPTION_KEY".
     */
    private static String resolveKeyMaterial() {
        String fromSysProp = System.getProperty("encryption.key");
        if (isUsableKey(fromSysProp)) {
            return fromSysProp.trim();
        }
        String fromEnv = System.getenv("NEXT_PUBLIC_PAYLOAD_ENCRYPTION_KEY");
        if (isUsableKey(fromEnv)) {
            return fromEnv.trim();
        }
        String fromConfig = ConfigLoader.getConfig().encryptionKey();
        return isUsableKey(fromConfig) ? fromConfig.trim() : null;
    }

    private static boolean isUsableKey(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String v = value.trim();
        // Placeholder text (e.g. someone pasted the env-var NAME instead of its value)
        return !v.equalsIgnoreCase("NEXT_PUBLIC_PAYLOAD_ENCRYPTION_KEY")
                && !v.equalsIgnoreCase("${NEXT_PUBLIC_PAYLOAD_ENCRYPTION_KEY}")
                && !v.startsWith("${");
    }

    private static String stripWrappingQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
