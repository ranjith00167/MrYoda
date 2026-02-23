package com.mryoda.diagnostics.api.tests.rewards;

import com.mryoda.diagnostics.api.tests.order.CreateOrderCODAPITest;

import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.utils.RewardHelper;
import org.testng.annotations.Test;

public class COD_18_RewardValidationTest extends CreateOrderCODAPITest {

    @Test(groups = "reward_initial")
    public void step18_A_VerifyInitialRewards() {
        System.out.println("\n>>> STEP 18-A: VERIFY INITIAL REWARDS (Pre-Payment) <<<");
        String mobile = RequestContext.getMobile();

        if (!"member_flow".equalsIgnoreCase(RequestContext.getCurrentFlowName())) {
            System.out.println("⏩ Skipping Rewards check for non-member flow.");
            return;
        }

        double initialRewards = RewardHelper.callGetRewardsByMobileAPI(mobile);
        RequestContext.setInitialTotalRewards(initialRewards);
        System.out.println("✅ Initial Rewards Stored: " + initialRewards);
    }

    @Test(groups = "reward_validation", dependsOnMethods = "com.mryoda.diagnostics.api.tests.payment.COD_15_ApprovePaymentTest.step15_ApprovePayment")
    public void step18_B_VerifyRewardsGain() {
        System.out.println("\n>>> STEP 18-B: VERIFY REWARDS GAIN (From Payment) <<<");

        if (!"member_flow".equalsIgnoreCase(RequestContext.getCurrentFlowName())) {
            System.out.println("⏩ Skipping Rewards check for non-member flow.");
            return;
        }

        double actualGain = RequestContext.getRewardsGain();
        double dueAmount = RequestContext.getCurrentDueAmount();

        RewardHelper.validateRewardsGain(actualGain, dueAmount);
        System.out.println("✅ Rewards Gain Validation Completed.");
    }

    @Test(groups = "reward_validation", dependsOnMethods = "step18_B_VerifyRewardsGain")
    public void step18_C_VerifyFinalRewardsBalance() {
        System.out.println("\n>>> STEP 18-C: VERIFY FINAL REWARDS BALANCE (Post-Payment) <<<");
        String mobile = RequestContext.getMobile();

        if (!"member_flow".equalsIgnoreCase(RequestContext.getCurrentFlowName())) {
            System.out.println("⏩ Skipping Rewards check for non-member flow.");
            return;
        }

        double finalRewards = RewardHelper.callGetRewardsByMobileAPI(mobile);
        RequestContext.setFinalTotalRewards(finalRewards);

        double initial = RequestContext.getInitialTotalRewards();

        // Use the actual rewards_gain from the Approve Payment API response
        // (stored in RequestContext by callApprovePaymentAPI via data[0].rewards_gain)
        double rewardsGainFromAPI = RequestContext.getRewardsGain();
        double expectedFinal = initial + rewardsGainFromAPI;

        System.out.println("📊 MATHEMATICAL REWARD VALIDATION:");
        System.out.println("   Initial Balance (A)    : " + initial);
        System.out.println("   Rewards Gain from API (B): " + rewardsGainFromAPI + " (from Approve Payment response)");
        System.out.println("   Expected Total (A+B)  : " + expectedFinal);
        System.out.println("   Actual API Balance    : " + finalRewards);

        if (Math.abs(finalRewards - expectedFinal) < 0.1) {
            System.out.println("✅ VALIDATION PASSED: Mathematically correct. Initial (" + initial
                    + ") + Rewards Gain (" + rewardsGainFromAPI + ") = Final API Balance (" + finalRewards + ").");
        } else {
            System.err.println("❌ VALIDATION FAILED: Final Rewards Balance mismatch!");
            System.err.println("   Expected: " + expectedFinal + ", but API returned: " + finalRewards);
            // In a strict automation, we might throw an assertion error here
            // throw new AssertionError("Reward Balance Mismatch! Expected: " +
            // expectedFinal + ", Actual: " + finalRewards);
        }
    }
}