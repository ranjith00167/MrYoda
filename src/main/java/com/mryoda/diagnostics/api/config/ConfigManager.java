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

}
