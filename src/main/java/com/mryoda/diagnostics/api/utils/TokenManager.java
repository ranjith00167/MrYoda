package com.mryoda.diagnostics.api.utils;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import io.restassured.response.Response;
import org.json.JSONObject;

public class TokenManager {

    // User type constants
    public static final String MEMBER = "MEMBER";
    public static final String NON_MEMBER = "NON_MEMBER";
    public static final String NEW_USER = "NEW_USER";
    public static final String GENERIC = "GENERIC";

    @Deprecated
    public static final String EXISTING_MEMBER = "EXISTING_MEMBER";

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 3000;

    /**
     * Generate token with user type - stores all fields in appropriate
     * RequestContext fields
     */
    public static String generateToken(String mobile, String userType) {

        System.out.println("\n==================================================");
        System.out.println("========== TOKEN GENERATION START (" + userType + ") ==========");
        System.out.println("==================================================");

        String countryCode = ConfigLoader.getConfig().countryCode();
        String otp = ConfigLoader.getConfig().staticOtp();

        Response verifyResponse = null;
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (attempt > 1) {
                    System.out.println("   ⟳ Retry attempt " + attempt + "/" + MAX_RETRIES + " (waiting " + RETRY_DELAY_MS + "ms)...");
                    Thread.sleep(RETRY_DELAY_MS);
                }

                // STEP 1: REQUEST OTP
                JSONObject otpReq = new JSONObject();
                otpReq.put("mobile", mobile);
                otpReq.put("country_code", countryCode);

                new RequestBuilder()
                        .setEndpoint(APIEndpoints.OTP_REQUEST)
                        .setRequestBody(otpReq.toString())
                        .expectStatus(200)
                        .post();

                // STEP 2: VERIFY OTP
                JSONObject verifyReq = new JSONObject();
                verifyReq.put("mobile", mobile);
                verifyReq.put("country_code", countryCode);
                verifyReq.put("otp", otp);

                verifyResponse = new RequestBuilder()
                        .setEndpoint(APIEndpoints.OTP_VERIFY)
                        .setRequestBody(verifyReq.toString())
                        .expectStatus(200)
                        .post();

                // If we get here, success - break retry loop
                lastException = null;
                break;
            } catch (Exception e) {
                lastException = e;
                System.out.println("   ⚠️ Attempt " + attempt + " failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }

        if (lastException != null) {
            throw new RuntimeException("Token generation failed after " + MAX_RETRIES + " attempts: " + lastException.getMessage(), lastException);
        }

        String token = verifyResponse.jsonPath().getString("data.access_token");
        String firstName = verifyResponse.jsonPath().getString("data.first_name");
        String lastName = verifyResponse.jsonPath().getString("data.last_name");
        String actualMobile = verifyResponse.jsonPath().getString("data.mobile");
        String userId = verifyResponse.jsonPath().getString("data.guid");

        AssertionUtil.verifyNotNull(token, "Token must not be null");

        // SAVE INTO REQUEST CONTEXT BASED ON USER TYPE
        switch (userType) {
            case MEMBER:
                RequestContext.setMemberToken(token);
                RequestContext.setMemberFirstName(firstName);
                RequestContext.setMemberLastName(lastName);
                RequestContext.setMemberUserId(userId);
                // Also set generic context for compatibility
                RequestContext.setToken(token);
                RequestContext.setUserId(userId);
                break;

            case NON_MEMBER:
            case EXISTING_MEMBER:
                RequestContext.setNonMemberToken(token);
                RequestContext.setNonMemberFirstName(firstName);
                RequestContext.setNonMemberLastName(lastName);
                RequestContext.setNonMemberUserId(userId);
                // Also set generic context for compatibility
                RequestContext.setToken(token);
                RequestContext.setUserId(userId);
                break;

            case NEW_USER:
                RequestContext.setNewUserToken(token);
                RequestContext.setNewUserFirstName(firstName);
                RequestContext.setNewUserLastName(lastName);
                RequestContext.setNewUserUserId(userId);
                // Also set generic context for compatibility
                RequestContext.setToken(token);
                RequestContext.setUserId(userId);
                break;

            case "phlebo":
                // For phlebotomist, we store in System properties and specific Phlebo GUID
                // field
                // to avoid overwriting the current customer's context
                System.setProperty("phlebo.token", token);
                RequestContext.setCurrentPhleboGuid(userId);
                System.out.println("   [TokenManager] Phlebo Token & GUID stored separately.");
                break;

            case GENERIC:
            default:
                RequestContext.setToken(token);
                RequestContext.setFirstName(firstName);
                RequestContext.setLastName(lastName);
                RequestContext.setUserId(userId);
                break;
        }

        System.out.println("Token generated for " + userType + ": " + token);
        return token;
    }

    public static String generateToken(String mobile) {
        return generateToken(mobile, GENERIC);
    }
}
