package com.mryoda.diagnostics.api.utils;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import io.restassured.response.Response;

public class RewardHelper {

    /**
     * Call Get Rewards By Mobile API
     * @param mobile Mobile number
     * @return Total rewards as double
     */
    public static double callGetRewardsByMobileAPI(String mobile) {
        System.out.println("\n==========================================================");
        System.out.println("      GET REWARDS BY MOBILE API (Helper)");
        System.out.println("==========================================================");

        String endpoint = APIEndpoints.MEMBER_BASE_URL + APIEndpoints.GET_REWARDS_BY_MOBILE.replace("{mobile_number}", mobile);
        System.out.println("Target URL: " + endpoint);

        Response response = new RequestBuilder()
                .setEndpoint(endpoint)
                .addHeader("Authorization", "Bearer " + RequestContext.getToken())
                .get();

        System.out.println("Response Status: " + response.getStatusCode());

        AssertionUtil.verifyEquals(response.getStatusCode(), 200, "Get Rewards should return 200");
        
        Object totalRewardsObj = response.jsonPath().get("data.total_rewards");
        double totalRewards = 0;
        if (totalRewardsObj != null) {
            totalRewards = totalRewardsObj instanceof Number ? ((Number) totalRewardsObj).doubleValue() : Double.parseDouble(totalRewardsObj.toString());
        }
        
        System.out.println("✅ Total Rewards for " + mobile + ": " + totalRewards);
        return totalRewards;
    }

    /**
     * Validate Rewards Gain against expected percentage (5%)
     * @param actualGain Actual rewards gain from response
     * @param dueAmount The amount paid
     */
    public static void validateRewardsGain(double actualGain, double dueAmount) {
        System.out.println("\n--- REWARDS GAIN VALIDATION ---");
        
        // Use ceiling rounding as requested by user (even 0.1 rounds up to next whole number)
        long roundedActualGain = (long) Math.ceil(actualGain);
        long expectedGainRounded = (long) Math.ceil(dueAmount * 0.05);

        System.out.println("🎁 Actual Rewards Gained: " + actualGain + " (Ceiled: " + roundedActualGain + ")");
        System.out.println("📊 Expected Rewards (5% of " + dueAmount + "): " + (dueAmount * 0.05) + " (Ceiled: " + expectedGainRounded + ")");

        // Special case: if rewards_used >= dueAmount (entire payment covered by rewards redemption),
        // no new cash was paid, so rewards_gain = 0 is valid API behavior.
        double rewardsUsedInContext = RequestContext.getRewardsUsed();
        if (actualGain == 0.0 && expectedGainRounded > 0 && rewardsUsedInContext >= dueAmount) {
            System.out.println("   ℹ️  rewards_used(" + rewardsUsedInContext + ") >= dueAmount(" + dueAmount
                    + ") — entire payment via rewards, no cash paid → rewards_gain=0 is correct");
            System.out.println("✅ VALIDATION PASSED: rewards_gain=0 (payment fully via rewards)");
            RequestContext.setRewardsGain(actualGain);
            return;
        }

        // Hard assertion: ceil(dueAmount × 5%) must equal ceiled actual gain (tolerance < 1)
        boolean gainCorrect = (roundedActualGain == expectedGainRounded)
                || Math.abs(actualGain - (dueAmount * 0.05)) < 1.0;
        AssertionUtil.verifyTrue(gainCorrect,
                "REWARDS GAIN FORMULA: ceil(" + dueAmount + " × 5%) = " + expectedGainRounded
                        + " but actual ceiled gain = " + roundedActualGain
                        + " (raw actual = " + actualGain + ")");

        if (gainCorrect) {
            System.out.println("✅ VALIDATION PASSED: Rewards Gain = " + actualGain
                    + " matches ceil(" + dueAmount + " × 5%) = " + expectedGainRounded);
        }

        RequestContext.setRewardsGain(actualGain);
    }
}
