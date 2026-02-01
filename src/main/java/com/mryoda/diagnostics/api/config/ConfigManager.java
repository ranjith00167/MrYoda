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
    @DefaultValue("Madhapur")
    String defaultLocationName();

}
