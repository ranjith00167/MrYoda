package com.mryoda.diagnostics.api.config;

import org.aeonbits.owner.Config;

/**
 * Configuration Manager using Owner Framework Reads configuration from
 * config.properties file
 */
@Config.Sources({ "classpath:config.properties" })
public interface ConfigManager extends Config {

    @Key("base.url")
    String baseUrl();

    @Key("phlebio.base.url")
    @DefaultValue("https://staging-api-phlebo-notification.yodadiagnostics.com")
    String phlebioBaseUrl();

    @Key("environment")
    String environment();

    @Key("static.otp")
    String staticOtp();

    @Key("country.code")
    String countryCode();

    @Key("nonMemberMobile.number")
    String nonMemberMobile();

    @Key("mobile.number")
    String memberMobile();

    @Key("new.user.mobile.prefix")
    @DefaultValue("988")
    String newUserMobilePrefix();

    @Key("report.path")
    @DefaultValue("test-output/reports/")
    String reportPath();

    @Key("enable.logging")
    @DefaultValue("true")
    boolean enableLogging();

    @Key("razorpay.key")
    String razorpayKey();

    @Key("razorpay.secret")
    String razorpaySecret();

    @Key("default.location.name")
    @DefaultValue("Ameerpet (HQ)")
    String defaultLocationName();

    @Key("username_ITDose")
    String adminUsername();

    @Key("password_ITDose")
    String adminPassword();

    @Key("admin.main.identifier")
    @DefaultValue("admin@yopmail.com")
    String adminMainIdentifier();

    @Key("admin.main.password")
    @DefaultValue("admin")
    String adminMainPassword();

    /** Identifier (mobile or email) of the admin account on the phlebo notification service.
     *  This account must have role="admin" in its JWT to call savePhlebo. */
    @Key("phlebo.admin.identifier")
    @DefaultValue("")
    String phleboAdminIdentifier();

    /** Password of the phlebo notification service admin account. */
    @Key("phlebo.admin.password")
    @DefaultValue("")
    String phleboAdminPassword();

    /** off | on | auto. See CryptoUtil for semantics. */
    @Key("encryption.mode")
    @DefaultValue("auto")
    String encryptionMode();

    /** Optional JSON field to wrap the encrypted request body in. Blank = raw ciphertext body. */
    @Key("encryption.request.field")
    @DefaultValue("")
    String encryptionRequestField();

    @Key("encryption.transformation")
    @DefaultValue("AES/GCM/NoPadding")
    String encryptionTransformation();

    @Key("encryption.key")
    @DefaultValue("")
    String encryptionKey();

    @Key("encryption.iv")
    @DefaultValue("")
    String encryptionIv();

    @Key("turnstile.token")
    @DefaultValue("test-token")
    String turnstileToken();

    // ---- Report (lab PDF) polling - see PollConfig; every key can be overridden with -D<key> ----

    @Key("report.poll.intervalMs")
    @DefaultValue("15000")
    Long reportPollIntervalMs();

    @Key("report.poll.maxAttempts")
    @DefaultValue("20")
    int reportPollMaxAttempts();

    @Key("report.poll.timeoutMs")
    @DefaultValue("600000")
    Long reportPollTimeoutMs();

    @Key("report.poll.transientRetries")
    @DefaultValue("3")
    int reportPollTransientRetries();

    @Key("report.poll.transientBackoffMs")
    @DefaultValue("2000")
    Long reportPollTransientBackoffMs();

    /** true: a result value in the PDF completes a test even when the API entry is still header-only. */
    @Key("report.acceptPdfOnly")
    @DefaultValue("true")
    boolean reportAcceptPdfOnly();

    @Key("report.pdf.minChars")
    @DefaultValue("20")
    int reportPdfMinChars();

    @Key("report.pdf.recheckEvery")
    @DefaultValue("3")
    int reportPdfRecheckEvery();

}
