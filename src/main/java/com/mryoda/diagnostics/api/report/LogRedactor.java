package com.mryoda.diagnostics.api.report;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Masks secrets and personal data before text reaches console or log files. */
public final class LogRedactor {

    private static final Pattern BEARER = Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]+");
    private static final Pattern JWT = Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*");
    private static final Pattern SIGNED_QUERY = Pattern.compile(
            "(?i)(X-Amz-(?:Signature|Credential|Security-Token|Algorithm|Date|SignedHeaders|Expires)|Signature|AWSAccessKeyId)=[^&\\s\"']+");
    private static final Pattern KEY_VALUE = Pattern.compile(
            "(?i)(\"?(?:authorization|access[_-]?token|refresh[_-]?token|token|password|passwd|secret|api[_-]?key|"
                    + "encryption[._-]?key|turnstileToken|fcm_token)\"?\\s*[:=]\\s*\"?)[^\\s\"',&}]+");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern MOBILE = Pattern.compile("(?<![0-9])([6-9][0-9]{5})([0-9]{4})(?![0-9])");

    private LogRedactor() {
    }

    public static String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String out = BEARER.matcher(text).replaceAll("$1***");
        out = JWT.matcher(out).replaceAll("***");
        out = SIGNED_QUERY.matcher(out).replaceAll("$1=***");
        out = KEY_VALUE.matcher(out).replaceAll("$1***");
        out = EMAIL.matcher(out).replaceAll("***@***");
        Matcher m = MOBILE.matcher(out);
        return m.replaceAll("******$2");
    }

    /** A URL safe to log: query string (signed S3 parameters) removed. */
    public static String stripQuery(String url) {
        if (url == null) {
            return null;
        }
        int q = url.indexOf('?');
        return q < 0 ? url : url.substring(0, q);
    }
}
