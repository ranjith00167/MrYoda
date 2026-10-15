package utils;

import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * AES-256-GCM decryption helper for encrypted API payloads.
 *
 * Payload format (per spec):
 *   base64( IV[12 bytes] + ciphertext + authTag[16 bytes] )
 *   delivered inside JSON as  { "__enc": "&lt;base64&gt;" }
 *
 * Algorithm : AES/GCM/NoPadding, 128-bit tag, 12-byte random IV per message,
 *             NO AAD.
 * Output    : UTF-8 JSON string.
 *
 * Key resolution order:
 *   1. env var  API_DECRYPTION_KEY                    (documented entry point)
 *   2. env var  NEXT_PUBLIC_PAYLOAD_ENCRYPTION_KEY     (project standard)
 *   3. sys prop encryption.key
 *   4. config  encryption.key  (src/test/resources/config.properties)
 *
 * The key must be Base64-encoded and decode to exactly 32 bytes (AES-256).
 * A wrong key or a tampered/invalid authentication tag is rejected by the GCM
 * tag check — unverified plaintext is never returned.
 */
public final class EncryptionUtils {

    private static final int  IV_LENGTH_BYTES   = 12;
    private static final int  TAG_LENGTH_BYTES  = 16;
    private static final int  TAG_LENGTH_BITS   = 128;
    private static final String TRANSFORMATION  = "AES/GCM/NoPadding";

    private static final String PRIMARY_ENV_VAR  = "API_DECRYPTION_KEY";
    private static final String PROJECT_ENV_VAR  = "NEXT_PUBLIC_PAYLOAD_ENCRYPTION_KEY";
    private static final String KEY_SYS_PROP     = "encryption.key";
    private static final String KEY_CONFIG_PROP  = "encryption.key";

    private EncryptionUtils() {
    }

    // ── Key material ─────────────────────────────────────────────────────────

    /**
     * Resolves the Base64-encoded AES-256 key from the environment, a system
     * property, or test config — skipping blanks and placeholder text.
     */
    private static String resolveEncodedKey() {
        String fromEnv = System.getenv(PRIMARY_ENV_VAR);
        if (isUsableKey(fromEnv)) {
            return fromEnv.trim();
        }

        String fromProjectEnv = System.getenv(PROJECT_ENV_VAR);
        if (isUsableKey(fromProjectEnv)) {
            return fromProjectEnv.trim();
        }

        String fromSysProp = System.getProperty(KEY_SYS_PROP);
        if (isUsableKey(fromSysProp)) {
            return fromSysProp.trim();
        }

        String fromConfig = readConfigValue(KEY_CONFIG_PROP);
        if (isUsableKey(fromConfig)) {
            return fromConfig.trim();
        }

        throw new IllegalStateException(
                "Set the " + PRIMARY_ENV_VAR + " environment variable "
                        + "(or " + PROJECT_ENV_VAR + ", the encryption.key system "
                        + "property, or encryption.key in config.properties) to the "
                        + "Base64-encoded 32-byte AES-256 key.");
    }

    /** True when a valid Base64 AES-256 key (32 decoded bytes) is reachable. */
    public static boolean isConfigured() {
        try {
            decodeKey(resolveEncodedKey());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static byte[] decodeKey(String encodedKey) {
        byte[] key = Base64.getDecoder().decode(encodedKey);
        if (key.length != 32) {
            throw new IllegalArgumentException(
                    "Expected a 32-byte AES-256 key but got " + key.length
                            + " bytes after Base64 decoding.");
        }
        return key;
    }

    private static boolean isUsableKey(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String v = value.trim();
        // Reject the literal env-var name / unexpanded placeholders so a bad
        // paste can never be mistaken for a real key.
        if (v.equalsIgnoreCase(PRIMARY_ENV_VAR) || v.equalsIgnoreCase(PROJECT_ENV_VAR)) {
            return false;
        }
        if (v.startsWith("${")) {
            return false;
        }
        try {
            return decodeKey(v).length == 32;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Reads a property from src/test/resources/config.properties. */
    private static String readConfigValue(String key) {
        java.util.Properties properties = new java.util.Properties();
        try (java.io.InputStream input = EncryptionUtils.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (Exception ignored) {
            // fall through — key simply not resolvable from config
        }
        return properties.getProperty(key);
    }

    // ── Decryption ───────────────────────────────────────────────────────────

    /**
     * Decrypts a single Base64 payload of the form IV + ciphertext + tag.
     *
     * @param encryptedPayload Base64 text taken from the "__enc" field.
     * @return decrypted UTF-8 JSON string.
     */
    public static String decrypt(String encryptedPayload) throws Exception {

        if (encryptedPayload == null || encryptedPayload.isBlank()) {
            throw new IllegalArgumentException("Encrypted payload is null or empty.");
        }

        byte[] key = decodeKey(resolveEncodedKey());

        byte[] payload = Base64.getDecoder().decode(encryptedPayload.trim());

        if (payload.length < IV_LENGTH_BYTES + TAG_LENGTH_BYTES) {
            throw new IllegalArgumentException("Encrypted payload is too short.");
        }

        byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH_BYTES);

        // Ciphertext is followed by the 16-byte GCM tag.
        byte[] ciphertextAndTag = Arrays.copyOfRange(
                payload, IV_LENGTH_BYTES, payload.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);

        cipher.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(TAG_LENGTH_BITS, iv)
        );

        // Throws AEADBadTagException when the key is wrong or the payload was
        // modified — plaintext is never returned unverified.
        byte[] plaintext = cipher.doFinal(ciphertextAndTag);

        return new String(plaintext, StandardCharsets.UTF_8);
    }

    /**
     * Dual-mode response resolver: returns decrypted JSON when the body carries
     * a "__enc" envelope, and the original body untouched for plain JSON.
     *
     * This keeps one test usable against both an encrypted and a plain staging
     * environment. When the body is encrypted but cannot be decrypted the
     * failure is surfaced instead of being silently swallowed.
     *
     * @param responseBody raw response body.
     * @param encField     value of the "__enc" field, or null when absent.
     * @return decrypted JSON string, or the plain body unchanged.
     */
    public static String decryptResponseBody(String responseBody, String encField) {
        if (encField == null || encField.isBlank()) {
            return responseBody;                     // plain (unencrypted) response
        }
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "Response is encrypted (__enc present) but no AES-256 key is "
                            + "configured. Set " + PRIMARY_ENV_VAR + ".");
        }
        try {
            return decrypt(encField);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to decrypt '__enc' response: " + e.getMessage(), e);
        }
    }
}
