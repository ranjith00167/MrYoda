package com.mryoda.diagnostics.api.tests.auth;

import com.mryoda.diagnostics.api.tests.order.CreateOrderCODAPITest;

import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.utils.TokenManager;
import org.testng.Assert;
import org.testng.annotations.Test;

public class COD_01_LoginTest extends CreateOrderCODAPITest {

    private String normalizeUserType(String userType) {
        if (userType == null) {
            return "";
        }
        String normalized = userType.trim().toLowerCase().replace("_", "").replace("-", "");
        if ("member".equals(normalized)) {
            return "member";
        }
        if ("nonmember".equals(normalized) || "existingmember".equals(normalized)) {
            return "nonmember";
        }
        if ("newuser".equals(normalized)) {
            return "new_user";
        }
        return normalized;
    }

    @Test(description = "QA Automation: Verify Login And Setup")
    public void step01_LoginAndSetup() {
        System.out.println("\n>>> STEP 1: DETAILED FLOW - LOGIN & SETUP <<<");

        // --- CLEAR STALE CONTEXT (Critical for testng_all sequential flows) ---
        System.out.println("   🧹 Clearing Context for clean flow execution...");
        RequestContext.setVisitNumber(null);
        RequestContext.setCurrentOrderId(null);
        RequestContext.setCurrentPaymentId(null);

        // Clear ScenarioContext variables to avoid using previous flow's data
        // (ScenarioContext lives in the Playwright project — use reflection to avoid compile error)
        try {
            Class<?> sc = Class.forName("utilities.ScenarioContext");
            sc.getField("extractedSinNo").set(null, null);
            sc.getField("extractedEmrId").set(null, null);
            sc.getField("extractedFirstName").set(null, null);
            sc.getField("orderId").set(null, null);
            System.out.println("   ✅ ScenarioContext Cleared.");
        } catch (Throwable t) {
            System.out.println("   ⚠️ Could not clear ScenarioContext: " + t.getMessage());
        }

        String userType = org.testng.Reporter.getCurrentTestResult().getTestContext().getCurrentXmlTest()
                .getParameter("userType");
        String normalizedUserType = normalizeUserType(userType);
        System.out.println("   Test Configuration User Type: " + (userType != null ? userType : "Default/Mixed")
                + " | normalized: " + (normalizedUserType.isEmpty() ? "default" : normalizedUserType));

        String token = null;
        String userId = null;

        if ("member".equals(normalizedUserType)) {
            token = RequestContext.getMemberToken();
            userId = RequestContext.getMemberUserId();
            System.out.println("   Using MEMBER credentials.");
        } else if ("nonmember".equals(normalizedUserType)) {
            token = RequestContext.getNonMemberToken();
            userId = RequestContext.getNonMemberUserId();
            System.out.println("   Using NON-MEMBER credentials.");
        } else if ("new_user".equals(normalizedUserType)) {
            token = RequestContext.getNewUserToken();
            userId = RequestContext.getNewUserUserId();

            if (token == null)
                token = RequestContext.getToken();
            if (userId == null)
                userId = RequestContext.getUserId();

            System.out.println("   Using NEW USER credentials.");
        } else {
            token = RequestContext.getToken();
            userId = RequestContext.getUserId();

            if (token == null) {
                token = RequestContext.getMemberToken();
                userId = RequestContext.getMemberUserId();
            }
        }

        if (token == null) {
            System.out.println("Token missing for " + userType + ", attempting generateToken...");

            // Determine user type and mobile number
            String mobile = "9003730394"; // Default member
            String tokenUserType = TokenManager.MEMBER;

            if ("nonmember".equals(normalizedUserType)) {
                mobile = "9666666665"; // Non-member mobile
                tokenUserType = TokenManager.NON_MEMBER;
            } else if ("new_user".equals(normalizedUserType)) {
                // New User usually requires registration, but if we assume it exists or use
                // generic
                mobile = "9003730394"; // Fallback or needs distinct logic
                tokenUserType = TokenManager.NEW_USER;
            }

            try {
                token = TokenManager.generateToken(mobile, tokenUserType);
                if (tokenUserType.equals(TokenManager.MEMBER)) {
                    userId = RequestContext.getMemberUserId();
                } else if (tokenUserType.equals(TokenManager.NON_MEMBER)) {
                    userId = RequestContext.getNonMemberUserId();
                } else {
                    userId = RequestContext.getUserId();
                }
            } catch (Exception e) {
                System.out.println("❌ Token Generation Failed: " + e.getMessage());
                e.printStackTrace();
                // Fallback to ensureLogin if strictly needed, or fail
                ensureLogin();
                token = RequestContext.getToken();
                userId = RequestContext.getUserId();
            }
        } else {
            System.out.println("✅ Token found from context.");
        }

        Assert.assertNotNull(token, "Token should not be null");
        Assert.assertNotNull(userId, "UserId should not be null");

        // Store globally for subsequent steps (Generic context)
        RequestContext.setToken(token);
        RequestContext.setUserId(userId);

        System.out.println("✅ Setup Complete for User: " + userId);

        // Ensure TokenManager updated specific contexts, but we also set generic ones
        // above.

        // clearCart(token, userId); // Removed to allow AddToCart step to function in
        // modular flow
    }
}
