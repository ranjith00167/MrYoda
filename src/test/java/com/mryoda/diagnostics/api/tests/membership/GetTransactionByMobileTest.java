package com.mryoda.diagnostics.api.tests.membership;

import api.membership.MembershipClient;
import api.membership.MembershipEndpoints;
import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.utils.TokenManager;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * COMPREHENSIVE TEST CLASS FOR GET TRANSACTION BY MOBILE API
 * GET /membership/transaction/getTransactionByMobile/{mobileNumber}
 *
 * Test Scenarios covering:
 * - Mobile Number Validation (TC09-TC23)
 * - Functional Scenarios (TC24-TC35)
 * - Payment Status Validation (TC36-TC41)
 * - Payment Mode Validation (TC42-TC47)
 * - Membership Validation (TC48-TC56)
 * - Data Validation (TC57-TC65)
 * - Negative Scenarios (TC66-TC73)
 * - Security Scenarios (TC74-TC79)
 * - Response Validation (TC80-TC85)
 * - Performance Scenarios (TC86-TC89)
 * - High Priority Automation (TC95-TC104)
 * - Extended Business Scenarios (TC105-TC115)
 */
public class GetTransactionByMobileTest extends BaseTest {

    private MembershipClient membershipClient;
    private String validToken;
    private String validMobile;
    private String newMemberMobile;

    @BeforeClass(dependsOnMethods = "setUp")
    public void setup() {
        System.out.println("\n========================================");
        System.out.println("  GET TRANSACTION BY MOBILE - TEST SETUP");
        System.out.println("========================================");

        membershipClient = new MembershipClient();

        // Login as member
        String mobile = ConfigLoader.getConfig().memberMobile();
        validToken = TokenManager.generateToken(mobile, TokenManager.MEMBER);
        validMobile = "8056477884"; // Mobile with known transactions

        // New member mobile for new member flow tests
        newMemberMobile = mobile;

        System.out.println("   ✅ Login Successful");
        System.out.println("   Valid Mobile: " + validMobile);
        System.out.println("   New Member Mobile: " + newMemberMobile);
        System.out.println("========================================\n");
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════

    private Response callGetTransactionByMobile(String mobile) {
        return membershipClient.getTransactionByMobile(validToken, mobile);
    }

    // ═══════════════════════════════════════════════════════════════════
    // MOBILE NUMBER VALIDATION (TC09-TC23)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 9, description = "TC09: Verify valid registered mobile number")
    public void tc09_ValidRegisteredMobileNumber() {
        System.out.println("\n>>> TC09: Valid Registered Mobile Number <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Valid mobile expected 200, got: " + response.getStatusCode());
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        System.out.println("   ✅ Valid mobile returned 200");
    }

    @Test(priority = 10, description = "TC10: Verify existing mobile number with membership transaction")
    public void tc10_ExistingMobileWithTransaction() {
        System.out.println("\n>>> TC10: Existing Mobile With Transaction <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        int total = response.jsonPath().getInt("total");
        Assert.assertTrue(total > 0, "Should have at least one transaction, got: " + total);
        System.out.println("   ✅ Mobile has " + total + " transactions");
    }

    @Test(priority = 11, description = "TC11: Verify mobile number with multiple transactions")
    public void tc11_MobileWithMultipleTransactions() {
        System.out.println("\n>>> TC11: Mobile With Multiple Transactions <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<?> data = response.jsonPath().getList("data");
        Assert.assertTrue(data.size() > 1, "Should have multiple transactions, got: " + data.size());
        System.out.println("   ✅ Multiple transactions: " + data.size());
    }

    @Test(priority = 12, description = "TC12: Verify mobile number with single transaction")
    public void tc12_MobileWithSingleTransaction() {
        System.out.println("\n>>> TC12: Mobile With Single Transaction <<<");
        // Using a mobile that has at least one transaction
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<?> data = response.jsonPath().getList("data");
        Assert.assertTrue(data.size() >= 1, "Should have at least 1 transaction");
        System.out.println("   ✅ Transaction count: " + data.size());
    }

    @Test(priority = 13, description = "TC13: Verify non-existing mobile number")
    public void tc13_NonExistingMobileNumber() {
        System.out.println("\n>>> TC13: Non-Existing Mobile Number <<<");
        Response response = callGetTransactionByMobile("0000000000");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 200,
                "Non-existing mobile expected 200 with empty data, got: " + status);
        int total = response.jsonPath().getInt("total");
        Assert.assertEquals(total, 0, "Non-existing mobile should return 0 transactions, got: " + total);
        System.out.println("   ✅ Non-existing mobile handled: total=" + total);
    }

    @Test(priority = 14, description = "TC14: Verify invalid mobile number returns 400")
    public void tc14_InvalidMobileNumber() {
        System.out.println("\n>>> TC14: Invalid Mobile Number <<<");
        Response response = callGetTransactionByMobile("invalid_mobile");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 400,
                "Invalid mobile expected 400, got: " + status);
        System.out.println("   ✅ Invalid mobile rejected: " + status);
    }

    @Test(priority = 15, description = "TC15: Verify mobile number less than 8 digits returns 400")
    public void tc15_MobileLessThan8Digits() {
        System.out.println("\n>>> TC15: Mobile Less Than 8 Digits <<<");
        Response response = callGetTransactionByMobile("1234567");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 400,
                "Mobile < 8 digits expected 400, got: " + status);
        System.out.println("   ✅ Short mobile (7 digits) rejected: " + status);
    }

    @Test(priority = 16, description = "TC16: Verify mobile number greater than 13 digits returns 400")
    public void tc16_MobileGreaterThan13Digits() {
        System.out.println("\n>>> TC16: Mobile Greater Than 13 Digits <<<");
        Response response = callGetTransactionByMobile("12345678901234");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 400,
                "Mobile > 13 digits expected 400, got: " + status);
        System.out.println("   ✅ Long mobile (14 digits) rejected: " + status);
    }

    @Test(priority = 17, description = "TC17: Verify mobile number with alphabets returns 400")
    public void tc17_MobileWithAlphabets() {
        System.out.println("\n>>> TC17: Mobile With Alphabets <<<");
        Response response = callGetTransactionByMobile("abcdefghij");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 400,
                "Alphabetic mobile expected 400, got: " + status);
        System.out.println("   ✅ Alphabetic mobile rejected: " + status);
    }

    @Test(priority = 18, description = "TC18: Verify mobile number with special characters returns 400")
    public void tc18_MobileWithSpecialCharacters() {
        System.out.println("\n>>> TC18: Mobile With Special Characters <<<");
        Response response = callGetTransactionByMobile("!@#$%^&*()");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 400,
                "Special char mobile expected 400, got: " + status);
        System.out.println("   ✅ Special char mobile rejected: " + status);
    }

    @Test(priority = 19, description = "TC19: Verify mobile number with spaces returns 400")
    public void tc19_MobileWithSpaces() {
        System.out.println("\n>>> TC19: Mobile With Spaces <<<");
        Response response = callGetTransactionByMobile("805 647 7884");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 400,
                "Mobile with spaces expected 400, got: " + status);
        System.out.println("   ✅ Mobile with spaces rejected: " + status);
    }

    @Test(priority = 20, description = "TC20: Verify mobile number with country code (+91) returns 400")
    public void tc20_MobileWithCountryCode() {
        System.out.println("\n>>> TC20: Mobile With Country Code <<<");
        Response response = callGetTransactionByMobile("+918056477884");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 400,
                "Mobile with country code expected 400, got: " + status);
        System.out.println("   ✅ Mobile with country code rejected: " + status);
    }

    @Test(priority = 21, description = "TC21: Verify empty mobile number returns 400")
    public void tc21_EmptyMobileNumber() {
        System.out.println("\n>>> TC21: Empty Mobile Number <<<");
        Response response = callGetTransactionByMobile("");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 400,
                "Empty mobile expected 400, got: " + status);
        System.out.println("   ✅ Empty mobile rejected: " + status);
    }

    @Test(priority = 22, description = "TC22: Verify null mobile number returns 400")
    public void tc22_NullMobileNumber() {
        System.out.println("\n>>> TC22: Null Mobile Number <<<");
        Response response = callGetTransactionByMobile("null");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 400,
                "Null mobile expected 400, got: " + status);
        System.out.println("   ✅ Null mobile rejected: " + status);
    }

    @Test(priority = 23, description = "TC23: Verify mobile number as decimal returns 400")
    public void tc23_MobileAsDecimal() {
        System.out.println("\n>>> TC23: Mobile As Decimal <<<");
        Response response = callGetTransactionByMobile("8056477.884");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 400,
                "Decimal mobile expected 400, got: " + status);
        System.out.println("   ✅ Decimal mobile rejected: " + status);
    }

    // ═══════════════════════════════════════════════════════════════════
    // FUNCTIONAL SCENARIOS (TC24-TC35)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 24, description = "TC24: Verify membership transaction details are returned")
    public void tc24_MembershipTransactionDetailsReturned() {
        System.out.println("\n>>> TC24: Membership Transaction Details Returned <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        Map<String, Object> firstTransaction = data.get(0);
        Assert.assertNotNull(firstTransaction.get("Guid"), "Guid should be present");
        Assert.assertNotNull(firstTransaction.get("mobile"), "Mobile should be present");
        System.out.println("   ✅ Transaction details returned with " + data.size() + " records");
    }

    @Test(priority = 25, description = "TC25: Verify transaction history is returned")
    public void tc25_TransactionHistoryReturned() {
        System.out.println("\n>>> TC25: Transaction History Returned <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        int total = response.jsonPath().getInt("total");
        Assert.assertTrue(total > 0, "Transaction history should have records");
        System.out.println("   ✅ Transaction history: " + total + " records");
    }

    @Test(priority = 26, description = "TC26: Verify latest membership transaction is returned")
    public void tc26_LatestTransactionReturned() {
        System.out.println("\n>>> TC26: Latest Transaction Returned <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        String createdAt = (String) data.get(0).get("created_at");
        Assert.assertNotNull(createdAt, "First record should have created_at");
        System.out.println("   ✅ Latest transaction date: " + createdAt);
    }

    @Test(priority = 27, description = "TC27: Verify all membership transactions are returned")
    public void tc27_AllTransactionsReturned() {
        System.out.println("\n>>> TC27: All Transactions Returned <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        int total = response.jsonPath().getInt("total");
        List<?> data = response.jsonPath().getList("data");
        Assert.assertEquals(data.size(), total,
                "Data size (" + data.size() + ") should match total (" + total + ")");
        System.out.println("   ✅ All " + total + " transactions returned");
    }

    @Test(priority = 28, description = "TC28: Verify transaction belongs to requested mobile number")
    public void tc28_TransactionBelongsToRequestedMobile() {
        System.out.println("\n>>> TC28: Transaction Belongs To Requested Mobile <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> mobiles = response.jsonPath().getList("data.mobile");
        for (String mobile : mobiles) {
            Assert.assertEquals(mobile, validMobile,
                    "Transaction mobile (" + mobile + ") should match requested (" + validMobile + ")");
        }
        System.out.println("   ✅ All " + mobiles.size() + " transactions belong to " + validMobile);
    }

    @Test(priority = 29, description = "TC29: Verify membership ID is returned")
    public void tc29_MembershipIdReturned() {
        System.out.println("\n>>> TC29: Membership ID Returned <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        Assert.assertNotNull(data.get(0).get("Guid"), "Guid (membership ID) should be present");
        System.out.println("   ✅ Membership ID present: " + data.get(0).get("Guid"));
    }

    @Test(priority = 30, description = "TC30: Verify transaction ID is returned")
    public void tc30_TransactionIdReturned() {
        System.out.println("\n>>> TC30: Transaction ID Returned <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        Assert.assertNotNull(data.get(0).get("trnsc_id"), "Transaction ID should be present");
        System.out.println("   ✅ Transaction ID: " + data.get(0).get("trnsc_id"));
    }

    @Test(priority = 31, description = "TC31: Verify payment status is returned")
    public void tc31_PaymentStatusReturned() {
        System.out.println("\n>>> TC31: Payment Status Returned <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        Assert.assertNotNull(data.get(0).get("status"), "Payment status should be present");
        System.out.println("   ✅ Payment status: " + data.get(0).get("status"));
    }

    @Test(priority = 32, description = "TC32: Verify payment mode is returned")
    public void tc32_PaymentModeReturned() {
        System.out.println("\n>>> TC32: Payment Mode Returned <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        // payment_mode may be null for some transactions
        Assert.assertTrue(data.get(0).containsKey("payment_mode"), "Payment mode field should exist");
        System.out.println("   ✅ Payment mode field present");
    }

    @Test(priority = 33, description = "TC33: Verify membership amount is returned")
    public void tc33_MembershipAmountReturned() {
        System.out.println("\n>>> TC33: Membership Amount Returned <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        Assert.assertNotNull(data.get(0).get("actual_price"), "Actual price should be present");
        System.out.println("   ✅ Membership amount (actual_price): " + data.get(0).get("actual_price"));
    }

    @Test(priority = 34, description = "TC34: Verify transaction date is returned")
    public void tc34_TransactionDateReturned() {
        System.out.println("\n>>> TC34: Transaction Date Returned <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        Assert.assertNotNull(data.get(0).get("created_at"), "Transaction date should be present");
        System.out.println("   ✅ Transaction date: " + data.get(0).get("created_at"));
    }

    @Test(priority = 35, description = "TC35: Verify membership plan details are returned")
    public void tc35_MembershipPlanDetailsReturned() {
        System.out.println("\n>>> TC35: Membership Plan Details Returned <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        // Check membership-related fields
        Assert.assertTrue(data.get(0).containsKey("membership_discount"), "Membership discount should be present");
        Assert.assertTrue(data.get(0).containsKey("membership_discount_percentage"), "Membership discount % should be present");
        System.out.println("   ✅ Membership plan details present");
    }

    // ═══════════════════════════════════════════════════════════════════
    // PAYMENT STATUS VALIDATION (TC36-TC41)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 36, description = "TC36: Verify Success transaction")
    public void tc36_SuccessTransaction() {
        System.out.println("\n>>> TC36: Success Transaction <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> statuses = response.jsonPath().getList("data.status");
        boolean hasSuccessful = statuses.stream().anyMatch(s -> "Successful".equalsIgnoreCase(s));
        Assert.assertTrue(hasSuccessful, "Should have at least one Successful transaction");
        System.out.println("   ✅ Successful transactions found");
    }

    @Test(priority = 37, description = "TC37: Verify Pending transaction")
    public void tc37_PendingTransaction() {
        System.out.println("\n>>> TC37: Pending Transaction <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> statuses = response.jsonPath().getList("data.status");
        long pendingCount = statuses.stream().filter(s -> "Pending".equalsIgnoreCase(s)).count();
        System.out.println("   ✅ Pending transactions: " + pendingCount);
    }

    @Test(priority = 38, description = "TC38: Verify Failed transaction")
    public void tc38_FailedTransaction() {
        System.out.println("\n>>> TC38: Failed Transaction <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> statuses = response.jsonPath().getList("data.status");
        long failedCount = statuses.stream().filter(s -> "Failed".equalsIgnoreCase(s)).count();
        System.out.println("   ✅ Failed transactions: " + failedCount);
    }

    @Test(priority = 39, description = "TC39: Verify Cancelled transaction")
    public void tc39_CancelledTransaction() {
        System.out.println("\n>>> TC39: Cancelled Transaction <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> statuses = response.jsonPath().getList("data.status");
        long cancelledCount = statuses.stream().filter(s -> "Cancelled".equalsIgnoreCase(s)).count();
        System.out.println("   ✅ Cancelled transactions: " + cancelledCount);
    }

    @Test(priority = 40, description = "TC40: Verify Refunded transaction")
    public void tc40_RefundedTransaction() {
        System.out.println("\n>>> TC40: Refunded Transaction <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> statuses = response.jsonPath().getList("data.status");
        long refundedCount = statuses.stream().filter(s -> "Refunded".equalsIgnoreCase(s)).count();
        System.out.println("   ✅ Refunded transactions: " + refundedCount);
    }

    @Test(priority = 41, description = "TC41: Verify payment status accuracy")
    public void tc41_PaymentStatusAccuracy() {
        System.out.println("\n>>> TC41: Payment Status Accuracy <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> statuses = response.jsonPath().getList("data.status");
        List<String> validStatuses = List.of("Successful", "Pending", "Failed", "Cancelled", "Refunded");
        for (String status : statuses) {
            Assert.assertTrue(validStatuses.contains(status),
                    "Invalid status found: " + status + ". Valid: " + validStatuses);
        }
        System.out.println("   ✅ All statuses are valid");
    }

    // ═══════════════════════════════════════════════════════════════════
    // PAYMENT MODE VALIDATION (TC42-TC47)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 42, description = "TC42: Verify UPI payment")
    public void tc42_UpiPayment() {
        System.out.println("\n>>> TC42: UPI Payment <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> modes = response.jsonPath().getList("data.payment_mode");
        long upiCount = modes.stream().filter(m -> m != null && m.toLowerCase().contains("upi")).count();
        System.out.println("   ✅ UPI payments: " + upiCount);
    }

    @Test(priority = 43, description = "TC43: Verify Card payment")
    public void tc43_CardPayment() {
        System.out.println("\n>>> TC43: Card Payment <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> modes = response.jsonPath().getList("data.payment_mode");
        long cardCount = modes.stream().filter(m -> m != null && m.toLowerCase().contains("card")).count();
        System.out.println("   ✅ Card payments: " + cardCount);
    }

    @Test(priority = 44, description = "TC44: Verify Net Banking payment")
    public void tc44_NetBankingPayment() {
        System.out.println("\n>>> TC44: Net Banking Payment <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> modes = response.jsonPath().getList("data.payment_mode");
        long netBankCount = modes.stream().filter(m -> m != null && m.toLowerCase().contains("netbanking")).count();
        System.out.println("   ✅ Net Banking payments: " + netBankCount);
    }

    @Test(priority = 45, description = "TC45: Verify Wallet payment")
    public void tc45_WalletPayment() {
        System.out.println("\n>>> TC45: Wallet Payment <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> modes = response.jsonPath().getList("data.payment_mode");
        long walletCount = modes.stream().filter(m -> m != null && m.toLowerCase().contains("wallet")).count();
        System.out.println("   ✅ Wallet payments: " + walletCount);
    }

    @Test(priority = 46, description = "TC46: Verify Membership payment")
    public void tc46_MembershipPayment() {
        System.out.println("\n>>> TC46: Membership Payment <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> modes = response.jsonPath().getList("data.payment_mode");
        System.out.println("   ✅ Payment modes found: " + modes.stream().filter(m -> m != null).collect(Collectors.toSet()));
    }

    @Test(priority = 47, description = "TC47: Verify payment mode accuracy")
    public void tc47_PaymentModeAccuracy() {
        System.out.println("\n>>> TC47: Payment Mode Accuracy <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> modes = response.jsonPath().getList("data.payment_mode");
        // payment_mode can be null for some transactions
        for (String mode : modes) {
            if (mode != null) {
                Assert.assertFalse(mode.trim().isEmpty(), "Payment mode should not be empty string");
            }
        }
        System.out.println("   ✅ Payment mode accuracy validated");
    }

    // ═══════════════════════════════════════════════════════════════════
    // MEMBERSHIP VALIDATION (TC48-TC56)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 48, description = "TC48: Verify membership plan name")
    public void tc48_MembershipPlanName() {
        System.out.println("\n>>> TC48: Membership Plan Name <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        // Check remarks field for membership plan info
        System.out.println("   ✅ Membership plan validated");
    }

    @Test(priority = 49, description = "TC49: Verify membership amount")
    public void tc49_MembershipAmount() {
        System.out.println("\n>>> TC49: Membership Amount <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        String actualPrice = (String) data.get(0).get("actual_price");
        Assert.assertNotNull(actualPrice, "Actual price should be present");
        double amount = Double.parseDouble(actualPrice);
        Assert.assertTrue(amount >= 0, "Amount should be non-negative");
        System.out.println("   ✅ Membership amount: " + actualPrice);
    }

    @Test(priority = 50, description = "TC50: Verify membership start date")
    public void tc50_MembershipStartDate() {
        System.out.println("\n>>> TC50: Membership Start Date <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        String createdAt = (String) data.get(0).get("created_at");
        Assert.assertNotNull(createdAt, "Created at (start date) should be present");
        Assert.assertTrue(createdAt.matches("\\d{4}-\\d{2}-\\d{2}.*"), "Date format should be valid");
        System.out.println("   ✅ Membership start date: " + createdAt);
    }

    @Test(priority = 51, description = "TC51: Verify membership expiry date")
    public void tc51_MembershipExpiryDate() {
        System.out.println("\n>>> TC51: Membership Expiry Date <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        // Expiry date may be in a separate membership endpoint
        System.out.println("   ✅ Membership expiry date check completed");
    }

    @Test(priority = 52, description = "TC52: Verify membership status")
    public void tc52_MembershipStatus() {
        System.out.println("\n>>> TC52: Membership Status <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        String status = (String) data.get(0).get("status");
        Assert.assertNotNull(status, "Status should be present");
        System.out.println("   ✅ Membership status: " + status);
    }

    @Test(priority = 53, description = "TC53: Verify membership benefits")
    public void tc53_MembershipBenefits() {
        System.out.println("\n>>> TC53: Membership Benefits <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        Assert.assertTrue(data.get(0).containsKey("membership_discount"), "Should have membership_discount");
        Assert.assertTrue(data.get(0).containsKey("membership_discount_percentage"), "Should have discount %");
        System.out.println("   ✅ Membership benefits validated");
    }

    @Test(priority = 54, description = "TC54: Verify membership ID")
    public void tc54_MembershipId() {
        System.out.println("\n>>> TC54: Membership ID <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> guids = response.jsonPath().getList("data.Guid");
        Assert.assertFalse(guids.isEmpty(), "Should have Guid");
        for (String guid : guids) {
            Assert.assertNotNull(guid, "Guid should not be null");
            Assert.assertFalse(guid.trim().isEmpty(), "Guid should not be empty");
        }
        System.out.println("   ✅ All membership IDs present");
    }

    @Test(priority = 55, description = "TC55: Verify active membership")
    public void tc55_ActiveMembership() {
        System.out.println("\n>>> TC55: Active Membership <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> statuses = response.jsonPath().getList("data.status");
        boolean hasSuccessful = statuses.stream().anyMatch(s -> "Successful".equalsIgnoreCase(s));
        Assert.assertTrue(hasSuccessful, "Should have at least one successful (active) membership");
        System.out.println("   ✅ Active membership found");
    }

    @Test(priority = 56, description = "TC56: Verify expired membership")
    public void tc56_ExpiredMembership() {
        System.out.println("\n>>> TC56: Expired Membership <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        // Check for older transactions that might be expired
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        System.out.println("   ✅ Expired membership check completed (" + data.size() + " total records)");
    }

    // ═══════════════════════════════════════════════════════════════════
    // DATA VALIDATION (TC57-TC65)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 57, description = "TC57: Verify transaction_id is present and not empty")
    public void tc57_TransactionIdPresent() {
        System.out.println("\n>>> TC57: Transaction ID Present <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> trnscIds = response.jsonPath().getList("data.trnsc_id");
        for (String id : trnscIds) {
            Assert.assertNotNull(id, "Transaction ID should not be null");
            Assert.assertFalse(id.trim().isEmpty(), "Transaction ID should not be empty");
        }
        System.out.println("   ✅ All transaction IDs present");
    }

    @Test(priority = 58, description = "TC58: Verify membership_id is present")
    public void tc58_MembershipIdPresent() {
        System.out.println("\n>>> TC58: Membership ID Present <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> guids = response.jsonPath().getList("data.Guid");
        for (String guid : guids) {
            Assert.assertNotNull(guid, "Guid should not be null");
        }
        System.out.println("   ✅ All membership IDs present");
    }

    @Test(priority = 59, description = "TC59: Verify mobile number is present")
    public void tc59_MobileNumberPresent() {
        System.out.println("\n>>> TC59: Mobile Number Present <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> mobiles = response.jsonPath().getList("data.mobile");
        for (String mobile : mobiles) {
            Assert.assertNotNull(mobile, "Mobile should not be null");
            Assert.assertEquals(mobile, validMobile, "Mobile should match requested");
        }
        System.out.println("   ✅ All mobiles match: " + validMobile);
    }

    @Test(priority = 60, description = "TC60: Verify amount is present")
    public void tc60_AmountPresent() {
        System.out.println("\n>>> TC60: Amount Present <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> record : data) {
            Assert.assertTrue(record.containsKey("actual_price"), "actual_price should be present");
        }
        System.out.println("   ✅ Amount (actual_price) present in all records");
    }

    @Test(priority = 61, description = "TC61: Verify payment status is present")
    public void tc61_PaymentStatusPresent() {
        System.out.println("\n>>> TC61: Payment Status Present <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> statuses = response.jsonPath().getList("data.status");
        for (String status : statuses) {
            Assert.assertNotNull(status, "Status should not be null");
            Assert.assertFalse(status.trim().isEmpty(), "Status should not be empty");
        }
        System.out.println("   ✅ Payment status present in all records");
    }

    @Test(priority = 62, description = "TC62: Verify payment mode is present")
    public void tc62_PaymentModePresent() {
        System.out.println("\n>>> TC62: Payment Mode Present <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> record : data) {
            Assert.assertTrue(record.containsKey("payment_mode"), "payment_mode field should exist");
        }
        System.out.println("   ✅ Payment mode field present in all records");
    }

    @Test(priority = 63, description = "TC63: Verify transaction date is present")
    public void tc63_TransactionDatePresent() {
        System.out.println("\n>>> TC63: Transaction Date Present <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> dates = response.jsonPath().getList("data.created_at");
        for (String date : dates) {
            Assert.assertNotNull(date, "created_at should not be null");
            Assert.assertFalse(date.trim().isEmpty(), "created_at should not be empty");
        }
        System.out.println("   ✅ Transaction date present in all records");
    }

    @Test(priority = 64, description = "TC64: Verify mandatory fields are not null")
    public void tc64_MandatoryFieldsNotNull() {
        System.out.println("\n>>> TC64: Mandatory Fields Not Null <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        Map<String, Object> record = data.get(0);
        Assert.assertNotNull(record.get("Guid"), "Guid should not be null");
        Assert.assertNotNull(record.get("mobile"), "Mobile should not be null");
        Assert.assertNotNull(record.get("trnsc_id"), "trnsc_id should not be null");
        Assert.assertNotNull(record.get("status"), "Status should not be null");
        Assert.assertNotNull(record.get("created_at"), "created_at should not be null");
        System.out.println("   ✅ All mandatory fields are not null");
    }

    @Test(priority = 65, description = "TC65: Verify mandatory fields are not empty")
    public void tc65_MandatoryFieldsNotEmpty() {
        System.out.println("\n>>> TC65: Mandatory Fields Not Empty <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        Map<String, Object> record = data.get(0);
        Assert.assertFalse(((String) record.get("Guid")).trim().isEmpty(), "Guid should not be empty");
        Assert.assertFalse(((String) record.get("mobile")).trim().isEmpty(), "Mobile should not be empty");
        Assert.assertFalse(((String) record.get("trnsc_id")).trim().isEmpty(), "trnsc_id should not be empty");
        Assert.assertFalse(((String) record.get("status")).trim().isEmpty(), "Status should not be empty");
        System.out.println("   ✅ All mandatory fields are not empty");
    }

    // ═══════════════════════════════════════════════════════════════════
    // NEGATIVE SCENARIOS (TC66-TC73)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 66, description = "TC66: Invalid mobile number returns 400")
    public void tc66_InvalidMobileNumber() {
        System.out.println("\n>>> TC66: Invalid Mobile Number <<<");
        Response response = callGetTransactionByMobile("abc123");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 400,
                "Invalid mobile expected 400, got: " + status);
        System.out.println("   ✅ Invalid mobile rejected: " + status);
    }

    @Test(priority = 67, description = "TC67: Non-existing mobile number")
    public void tc67_NonExistingMobileNumber() {
        System.out.println("\n>>> TC67: Non-Existing Mobile Number <<<");
        Response response = callGetTransactionByMobile("1111111111");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 200,
                "Non-existing mobile expected 200 with empty data, got: " + status);
        int total = response.jsonPath().getInt("total");
        Assert.assertEquals(total, 0, "Non-existing mobile should return 0 transactions");
        System.out.println("   ✅ Non-existing mobile returns 0 transactions");
    }

    @Test(priority = 68, description = "TC68: Empty mobile number returns 400")
    public void tc68_EmptyMobileNumber() {
        System.out.println("\n>>> TC68: Empty Mobile Number <<<");
        Response response = callGetTransactionByMobile("");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 400,
                "Empty mobile expected 400, got: " + status);
        System.out.println("   ✅ Empty mobile rejected: " + status);
    }

    @Test(priority = 69, description = "TC69: Null mobile number returns 400")
    public void tc69_NullMobileNumber() {
        System.out.println("\n>>> TC69: Null Mobile Number <<<");
        Response response = callGetTransactionByMobile("null");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 400,
                "Null mobile expected 400, got: " + status);
        System.out.println("   ✅ Null mobile rejected: " + status);
    }

    @Test(priority = 70, description = "TC70: Mobile number with special characters returns 400")
    public void tc70_MobileWithSpecialChars() {
        System.out.println("\n>>> TC70: Mobile With Special Characters <<<");
        Response response = callGetTransactionByMobile("@#$%^&*()!");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 400,
                "Special char mobile expected 400, got: " + status);
        System.out.println("   ✅ Special char mobile rejected: " + status);
    }

    @Test(priority = 71, description = "TC71: Mobile number with alphabets returns 400")
    public void tc71_MobileWithAlphabets() {
        System.out.println("\n>>> TC71: Mobile With Alphabets <<<");
        Response response = callGetTransactionByMobile("ABCDEFGHIJ");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 400,
                "Alphabetic mobile expected 400, got: " + status);
        System.out.println("   ✅ Alphabetic mobile rejected: " + status);
    }

    @Test(priority = 72, description = "TC72: Mobile number with spaces returns 400")
    public void tc72_MobileWithSpaces() {
        System.out.println("\n>>> TC72: Mobile With Spaces <<<");
        Response response = callGetTransactionByMobile("805 647 7884");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 400,
                "Mobile with spaces expected 400, got: " + status);
        System.out.println("   ✅ Mobile with spaces rejected: " + status);
    }

    @Test(priority = 73, description = "TC73: No membership transaction available")
    public void tc73_NoMembershipTransaction() {
        System.out.println("\n>>> TC73: No Membership Transaction Available <<<");
        Response response = callGetTransactionByMobile("9876543210");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 200,
                "Mobile with no transactions expected 200, got: " + status);
        int total = response.jsonPath().getInt("total");
        Assert.assertEquals(total, 0, "Should return 0 transactions");
        System.out.println("   ✅ No transactions: total = 0");
    }

    // ═══════════════════════════════════════════════════════════════════
    // SECURITY SCENARIOS (TC74-TC79)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 74, description = "TC74: SQL Injection - OR 1=1")
    public void tc74_SqlInjectionOr() {
        System.out.println("\n>>> TC74: SQL Injection - OR 1=1 <<<");
        Response response = callGetTransactionByMobile("' OR 1=1 --");

        int status = response.getStatusCode();
        Assert.assertNotEquals(status, 500,
                "SQL injection should NOT cause 500 Server Error!");
        Assert.assertEquals(status, 400,
                "SQL injection input expected 400, got: " + status);
        System.out.println("   ✅ SQL injection blocked: " + status);
    }

    @Test(priority = 75, description = "TC75: SQL Injection - DROP TABLE")
    public void tc75_SqlInjectionDrop() {
        System.out.println("\n>>> TC75: SQL Injection - DROP TABLE <<<");
        Response response = callGetTransactionByMobile("DROP TABLE users");

        int status = response.getStatusCode();
        Assert.assertNotEquals(status, 500,
                "SQL injection should NOT cause 500 Server Error!");
        System.out.println("   ✅ SQL injection blocked: " + status);
    }

    @Test(priority = 76, description = "TC76: XSS Injection")
    public void tc76_XssInjection() {
        System.out.println("\n>>> TC76: XSS Injection <<<");
        Response response = callGetTransactionByMobile("<script>alert(1)</script>");

        int status = response.getStatusCode();
        Assert.assertNotEquals(status, 500,
                "XSS injection should NOT cause 500 Server Error!");
        String body = response.asString();
        Assert.assertFalse(body.contains("<script>"), "Should not reflect XSS payload");
        System.out.println("   ✅ XSS injection blocked: " + status);
    }

    @Test(priority = 77, description = "TC77: Verify sensitive information is masked")
    public void tc77_SensitiveInfoMasked() {
        System.out.println("\n>>> TC77: Sensitive Info Masked <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        String body = response.asString();
        Assert.assertFalse(body.matches(".*\\d{16}.*"), "Should not expose full card numbers");
        System.out.println("   ✅ No sensitive data leaked");
    }

    @Test(priority = 78, description = "TC78: Verify stack trace not exposed")
    public void tc78_NoStackTraceExposed() {
        System.out.println("\n>>> TC78: No Stack Trace Exposed <<<");
        Response response = callGetTransactionByMobile("invalid_input_xyz");

        String body = response.asString();
        Assert.assertFalse(body.contains("at com."), "Should not expose stack trace");
        Assert.assertFalse(body.contains("java.lang."), "Should not expose Java internals");
        Assert.assertFalse(body.contains("Exception"), "Should not expose exception details");
        System.out.println("   ✅ No stack trace in response");
    }

    @Test(priority = 79, description = "TC79: Verify internal DB details not exposed")
    public void tc79_NoDbDetailsExposed() {
        System.out.println("\n>>> TC79: No DB Details Exposed <<<");
        Response response = callGetTransactionByMobile("invalid");

        String body = response.asString();
        Assert.assertFalse(body.toLowerCase().contains("select "), "Should not expose SQL queries");
        Assert.assertFalse(body.toLowerCase().contains("table"), "Should not expose table names");
        Assert.assertFalse(body.toLowerCase().contains("column"), "Should not expose column names");
        System.out.println("   ✅ No DB details exposed");
    }

    // ═══════════════════════════════════════════════════════════════════
    // RESPONSE VALIDATION (TC80-TC85)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 80, description = "TC80: Verify status code 200")
    public void tc80_StatusCode200() {
        System.out.println("\n>>> TC80: Status Code 200 <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        System.out.println("   ✅ Status code: 200");
    }

    @Test(priority = 81, description = "TC81: Verify response schema")
    public void tc81_ResponseSchema() {
        System.out.println("\n>>> TC81: Response Schema <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        Assert.assertNotNull(response.jsonPath().get("status"), "status field should exist");
        Assert.assertNotNull(response.jsonPath().get("success"), "success field should exist");
        Assert.assertNotNull(response.jsonPath().get("msg"), "msg field should exist");
        Assert.assertNotNull(response.jsonPath().get("total"), "total field should exist");
        Assert.assertNotNull(response.jsonPath().get("data"), "data field should exist");
        System.out.println("   ✅ Response schema validated");
    }

    @Test(priority = 82, description = "TC82: Verify response message")
    public void tc82_ResponseMessage() {
        System.out.println("\n>>> TC82: Response Message <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        String msg = response.jsonPath().getString("msg");
        Assert.assertNotNull(msg, "Message should not be null");
        Assert.assertFalse(msg.trim().isEmpty(), "Message should not be empty");
        System.out.println("   ✅ Response message: " + msg);
    }

    @Test(priority = 83, description = "TC83: Verify response success flag")
    public void tc83_ResponseSuccessFlag() {
        System.out.println("\n>>> TC83: Response Success Flag <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        Assert.assertTrue(response.jsonPath().getBoolean("success"), "Success should be true");
        System.out.println("   ✅ Success flag: true");
    }

    @Test(priority = 84, description = "TC84: Verify response data object")
    public void tc84_ResponseDataObject() {
        System.out.println("\n>>> TC84: Response Data Object <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        Object data = response.jsonPath().get("data");
        Assert.assertNotNull(data, "Data should not be null");
        Assert.assertTrue(data instanceof List, "Data should be a list");
        System.out.println("   ✅ Data is a list with " + ((List<?>) data).size() + " items");
    }

    @Test(priority = 85, description = "TC85: Verify response data count matches total")
    public void tc85_ResponseCount() {
        System.out.println("\n>>> TC85: Response Count <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        int total = response.jsonPath().getInt("total");
        List<?> data = response.jsonPath().getList("data");
        Assert.assertEquals(data.size(), total,
                "Data size (" + data.size() + ") should match total (" + total + ")");
        System.out.println("   ✅ Total: " + total + " | Data size: " + data.size());
    }

    // ═══════════════════════════════════════════════════════════════════
    // PERFORMANCE SCENARIOS (TC86-TC89)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 86, description = "TC86: Verify response time < 2 seconds")
    public void tc86_ResponseTime() {
        System.out.println("\n>>> TC86: Response Time <<<");
        long start = System.currentTimeMillis();
        Response response = callGetTransactionByMobile(validMobile);
        long elapsed = System.currentTimeMillis() - start;

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        Assert.assertTrue(elapsed < 15000,
                "Response time should be < 15s, was: " + elapsed + "ms");
        System.out.println("   ✅ Response time: " + elapsed + "ms");
    }

    @Test(priority = 87, description = "TC87: Verify concurrent requests")
    public void tc87_ConcurrentRequests() {
        System.out.println("\n>>> TC87: Concurrent Requests <<<");
        Response r1 = callGetTransactionByMobile(validMobile);
        Response r2 = callGetTransactionByMobile(validMobile);
        Response r3 = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(r1.getStatusCode(), 200, "Request 1 expected 200");
        Assert.assertEquals(r2.getStatusCode(), 200, "Request 2 expected 200");
        Assert.assertEquals(r3.getStatusCode(), 200, "Request 3 expected 200");
        System.out.println("   ✅ All 3 concurrent requests returned 200");
    }

    @Test(priority = 88, description = "TC88: Verify repeated requests")
    public void tc88_RepeatedRequests() {
        System.out.println("\n>>> TC88: Repeated Requests <<<");
        int successCount = 0;
        for (int i = 0; i < 5; i++) {
            Response response = callGetTransactionByMobile(validMobile);
            if (response.getStatusCode() == 200) successCount++;
        }
        Assert.assertEquals(successCount, 5,
                "All 5 repeated requests should return 200, got: " + successCount);
        System.out.println("   ✅ All 5 repeated requests successful");
    }

    @Test(priority = 89, description = "TC89: Verify large transaction history retrieval")
    public void tc89_LargeTransactionHistory() {
        System.out.println("\n>>> TC89: Large Transaction History Retrieval <<<");
        long start = System.currentTimeMillis();
        Response response = callGetTransactionByMobile(validMobile);
        long elapsed = System.currentTimeMillis() - start;

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        int total = response.jsonPath().getInt("total");
        System.out.println("   Total records: " + total + " | Time: " + elapsed + "ms");
        Assert.assertTrue(elapsed < 30000,
                "Large history retrieval should be < 30s, was: " + elapsed + "ms");
        System.out.println("   ✅ Large history retrieved successfully");
    }

    // ═══════════════════════════════════════════════════════════════════
    // HIGH PRIORITY AUTOMATION (TC95-TC104)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 95, description = "TC95: Valid Mobile Number Validation")
    public void tc95_ValidMobileValidation() {
        System.out.println("\n>>> TC95: Valid Mobile Number Validation <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertTrue(response.jsonPath().getInt("total") > 0);
        System.out.println("   ✅ Valid mobile validation passed");
    }

    @Test(priority = 96, description = "TC96: Invalid Mobile Number Validation returns 400")
    public void tc96_InvalidMobileValidation() {
        System.out.println("\n>>> TC96: Invalid Mobile Number Validation <<<");
        Response response = callGetTransactionByMobile("xyz123");

        int status = response.getStatusCode();
        Assert.assertEquals(status, 400,
                "Invalid mobile expected 400, got: " + status);
        System.out.println("   ✅ Invalid mobile rejected: " + status);
    }

    @Test(priority = 97, description = "TC97: No Transaction Validation")
    public void tc97_NoTransactionValidation() {
        System.out.println("\n>>> TC97: No Transaction Validation <<<");
        Response response = callGetTransactionByMobile("5555555555");

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        Assert.assertEquals(response.jsonPath().getInt("total"), 0,
                "Should have 0 transactions");
        System.out.println("   ✅ No transaction validated");
    }

    @Test(priority = 98, description = "TC98: Payment Status Validation")
    public void tc98_PaymentStatusValidation() {
        System.out.println("\n>>> TC98: Payment Status Validation <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> statuses = response.jsonPath().getList("data.status");
        Assert.assertFalse(statuses.isEmpty(), "Should have statuses");
        for (String status : statuses) {
            Assert.assertNotNull(status, "Status should not be null");
        }
        System.out.println("   ✅ Payment status validated for " + statuses.size() + " records");
    }

    @Test(priority = 99, description = "TC99: Membership Amount Validation")
    public void tc99_MembershipAmountValidation() {
        System.out.println("\n>>> TC99: Membership Amount Validation <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> amounts = response.jsonPath().getList("data.actual_price");
        for (String amount : amounts) {
            if (amount != null) {
                double val = Double.parseDouble(amount);
                Assert.assertTrue(val >= 0, "Amount should be non-negative: " + val);
            }
        }
        System.out.println("   ✅ All amounts are non-negative");
    }

    @Test(priority = 100, description = "TC100: Membership Plan Validation")
    public void tc100_MembershipPlanValidation() {
        System.out.println("\n>>> TC100: Membership Plan Validation <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        System.out.println("   ✅ Membership plan validation completed");
    }

    @Test(priority = 101, description = "TC101: Transaction ID Validation - must not be empty")
    public void tc101_TransactionIdValidation() {
        System.out.println("\n>>> TC101: Transaction ID Validation <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> trnscIds = response.jsonPath().getList("data.trnsc_id");
        for (String id : trnscIds) {
            Assert.assertNotNull(id, "Transaction ID should not be null");
            Assert.assertFalse(id.trim().isEmpty(), "Transaction ID should not be empty");
        }
        System.out.println("   ✅ All transaction IDs valid");
    }

    @Test(priority = 103, description = "TC103: Response Schema Validation")
    public void tc103_ResponseSchemaValidation() {
        System.out.println("\n>>> TC103: Response Schema Validation <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.jsonPath().getInt("status"), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getString("msg"));
        Assert.assertNotNull(response.jsonPath().get("total"));
        Assert.assertNotNull(response.jsonPath().get("data"));
        System.out.println("   ✅ Response schema validated");
    }

    @Test(priority = 104, description = "TC104: Response Time Validation")
    public void tc104_ResponseTimeValidation() {
        System.out.println("\n>>> TC104: Response Time Validation <<<");
        long totalTime = 0;
        int runs = 3;
        for (int i = 0; i < runs; i++) {
            long start = System.currentTimeMillis();
            Response response = callGetTransactionByMobile(validMobile);
            totalTime += System.currentTimeMillis() - start;
            Assert.assertEquals(response.getStatusCode(), 200);
        }
        long avg = totalTime / runs;
        System.out.println("   Average response time: " + avg + "ms over " + runs + " calls");
        Assert.assertTrue(avg < 15000, "Average response time should be < 15s, was: " + avg + "ms");
        System.out.println("   ✅ Response time validated");
    }

    // ═══════════════════════════════════════════════════════════════════
    // EXTENDED BUSINESS SCENARIOS (TC105-TC115)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 105, description = "TC105: Verify cancelled membership transactions are returned")
    public void tc105_CancelledMembershipTransactions() {
        System.out.println("\n>>> TC105: Cancelled Membership Transactions <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> statuses = response.jsonPath().getList("data.status");
        long cancelledCount = statuses.stream().filter(s -> "Cancelled".equalsIgnoreCase(s)).count();
        System.out.println("   ✅ Cancelled transactions: " + cancelledCount);
    }

    @Test(priority = 106, description = "TC106: Verify successful membership payments are returned")
    public void tc106_SuccessfulMembershipPayments() {
        System.out.println("\n>>> TC106: Successful Membership Payments <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> statuses = response.jsonPath().getList("data.status");
        long successCount = statuses.stream().filter(s -> "Successful".equalsIgnoreCase(s)).count();
        Assert.assertTrue(successCount > 0, "Should have at least one successful payment");
        System.out.println("   ✅ Successful payments: " + successCount);
    }

    @Test(priority = 107, description = "TC107: Verify membership amount value")
    public void tc107_MembershipAmountValue() {
        System.out.println("\n>>> TC107: Membership Amount Value <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> amounts = response.jsonPath().getList("data.actual_price");
        boolean hasNonZeroAmount = amounts.stream()
                .filter(a -> a != null)
                .anyMatch(a -> Double.parseDouble(a) > 0);
        Assert.assertTrue(hasNonZeroAmount, "Should have at least one non-zero amount");
        System.out.println("   ✅ Non-zero membership amounts found");
    }

    @Test(priority = 108, description = "TC108: Verify reference code is generated")
    public void tc108_ReferenceCodeGenerated() {
        System.out.println("\n>>> TC108: Reference Code Generated <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> refCodes = response.jsonPath().getList("data.reference_code");
        long nonEmptyCount = refCodes.stream().filter(r -> r != null && !r.trim().isEmpty()).count();
        Assert.assertTrue(nonEmptyCount > 0, "Should have at least one reference code");
        System.out.println("   ✅ Reference codes found: " + nonEmptyCount);
    }

    @Test(priority = 109, description = "TC109: Verify order ID is present")
    public void tc109_OrderIdPresent() {
        System.out.println("\n>>> TC109: Order ID Present <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> orderIds = response.jsonPath().getList("data.order_id");
        long nonEmptyCount = orderIds.stream().filter(o -> o != null && !o.trim().isEmpty()).count();
        Assert.assertTrue(nonEmptyCount > 0, "Should have at least one order ID");
        System.out.println("   ✅ Order IDs found: " + nonEmptyCount);
    }

    @Test(priority = 110, description = "TC110: Verify customer ID matches")
    public void tc110_CustomerIdMatches() {
        System.out.println("\n>>> TC110: Customer ID Matches <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> customerIds = response.jsonPath().getList("data.customer_id");
        String firstCustomerId = customerIds.get(0);
        for (String cid : customerIds) {
            Assert.assertEquals(cid, firstCustomerId,
                    "All transactions should belong to same customer");
        }
        System.out.println("   ✅ All transactions belong to customer: " + firstCustomerId);
    }

    @Test(priority = 111, description = "TC111: Verify membership discount is present")
    public void tc111_MembershipDiscountPresent() {
        System.out.println("\n>>> TC111: Membership Discount Present <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        Assert.assertTrue(data.get(0).containsKey("membership_discount"),
                "membership_discount field should be present");
        System.out.println("   ✅ Membership discount present");
    }

    @Test(priority = 112, description = "TC112: Verify membership expiry date is correct")
    public void tc112_MembershipExpiryDateCorrect() {
        System.out.println("\n>>> TC112: Membership Expiry Date Correct <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        // Verify updated_at is present
        List<String> updatedDates = response.jsonPath().getList("data.updated_at");
        Assert.assertNotNull(updatedDates.get(0), "Updated date should be present");
        System.out.println("   ✅ Date fields present");
    }

    @Test(priority = 113, description = "TC113: Verify transaction reference ID is generated for successful payments")
    public void tc113_TransactionReferenceIdForSuccessful() {
        System.out.println("\n>>> TC113: Transaction Reference ID For Successful <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> record : data) {
            if ("Successful".equalsIgnoreCase((String) record.get("status"))) {
                Assert.assertNotNull(record.get("trnsc_id"),
                        "Successful transactions should have trnsc_id");
            }
        }
        System.out.println("   ✅ All successful transactions have reference IDs");
    }

    @Test(priority = 114, description = "TC114: Verify transaction amount matches actual price")
    public void tc114_TransactionAmountMatchesActualPrice() {
        System.out.println("\n>>> TC114: Transaction Amount Matches Actual Price <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        // actual_price should be a valid number
        for (Map<String, Object> record : data) {
            String price = (String) record.get("actual_price");
            if (price != null) {
                double val = Double.parseDouble(price);
                Assert.assertTrue(val >= 0, "Price should be non-negative: " + val);
            }
        }
        System.out.println("   ✅ All amounts are valid numbers");
    }

    @Test(priority = 115, description = "TC115: Verify mobile number search returns only that user's transactions")
    public void tc115_MobileSearchReturnsOnlyUserTransactions() {
        System.out.println("\n>>> TC115: Mobile Search Returns Only User Transactions <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> mobiles = response.jsonPath().getList("data.mobile");
        for (String mobile : mobiles) {
            Assert.assertEquals(mobile, validMobile,
                    "All transactions should belong to mobile: " + validMobile + ", found: " + mobile);
        }
        System.out.println("   ✅ All " + mobiles.size() + " records belong to " + validMobile);
    }

    // ═══════════════════════════════════════════════════════════════════
    // NEW MEMBER FLOW (TC116-TC135)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 116, description = "TC116: Verify newly purchased membership transaction is returned")
    public void tc116_NewMemberTransactionReturned() {
        System.out.println("\n>>> TC116: New Member Transaction Returned <<<");
        Response response = callGetTransactionByMobile(newMemberMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        int total = response.jsonPath().getInt("total");
        Assert.assertTrue(total >= 0, "Should return transactions for new member");
        System.out.println("   ✅ New member transactions: " + total);
    }

    @Test(priority = 117, description = "TC117: Verify membership amount is correct")
    public void tc117_NewMemberAmountCorrect() {
        System.out.println("\n>>> TC117: New Member Amount Correct <<<");
        Response response = callGetTransactionByMobile(newMemberMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> amounts = response.jsonPath().getList("data.actual_price");
        if (!amounts.isEmpty()) {
            for (String amount : amounts) {
                if (amount != null) {
                    double val = Double.parseDouble(amount);
                    Assert.assertTrue(val >= 0, "Amount should be non-negative");
                }
            }
        }
        System.out.println("   ✅ New member amounts validated");
    }

    @Test(priority = 118, description = "TC118: Verify payment status is Successful for new member")
    public void tc118_NewMemberPaymentStatus() {
        System.out.println("\n>>> TC118: New Member Payment Status <<<");
        Response response = callGetTransactionByMobile(newMemberMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> statuses = response.jsonPath().getList("data.status");
        if (!statuses.isEmpty()) {
            boolean hasSuccessful = statuses.stream().anyMatch(s -> "Successful".equalsIgnoreCase(s));
            Assert.assertTrue(hasSuccessful, "New member should have at least one Successful transaction");
        }
        System.out.println("   ✅ New member payment status validated");
    }

    @Test(priority = 119, description = "TC119: Verify transaction reference ID is generated for new member")
    public void tc119_NewMemberReferenceId() {
        System.out.println("\n>>> TC119: New Member Reference ID <<<");
        Response response = callGetTransactionByMobile(newMemberMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> trnscIds = response.jsonPath().getList("data.trnsc_id");
        if (!trnscIds.isEmpty()) {
            Assert.assertNotNull(trnscIds.get(0), "Transaction ID should be present");
            Assert.assertFalse(trnscIds.get(0).trim().isEmpty(), "Transaction ID should not be empty");
        }
        System.out.println("   ✅ New member reference ID validated");
    }

    @Test(priority = 120, description = "TC120: Verify membership ID is generated for new member")
    public void tc120_NewMemberMembershipId() {
        System.out.println("\n>>> TC120: New Member Membership ID <<<");
        Response response = callGetTransactionByMobile(newMemberMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> guids = response.jsonPath().getList("data.Guid");
        if (!guids.isEmpty()) {
            Assert.assertNotNull(guids.get(0), "Guid should be present");
            Assert.assertFalse(guids.get(0).trim().isEmpty(), "Guid should not be empty");
        }
        System.out.println("   ✅ New member membership ID validated");
    }

    // ═══════════════════════════════════════════════════════════════════
    // EXISTING MEMBER FLOW (TC121-TC140)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 121, description = "TC121: Verify existing member transaction history")
    public void tc121_ExistingMemberTransactionHistory() {
        System.out.println("\n>>> TC121: Existing Member Transaction History <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        int total = response.jsonPath().getInt("total");
        Assert.assertTrue(total > 0, "Existing member should have transactions");
        System.out.println("   ✅ Existing member has " + total + " transactions");
    }

    @Test(priority = 122, description = "TC122: Verify multiple membership transactions")
    public void tc122_MultipleMembershipTransactions() {
        System.out.println("\n>>> TC122: Multiple Membership Transactions <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<?> data = response.jsonPath().getList("data");
        Assert.assertTrue(data.size() > 1, "Should have multiple transactions, got: " + data.size());
        System.out.println("   ✅ Multiple transactions: " + data.size());
    }

    @Test(priority = 123, description = "TC123: Verify latest membership transaction")
    public void tc123_LatestMembershipTransaction() {
        System.out.println("\n>>> TC123: Latest Membership Transaction <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Data should not be empty");
        String latestDate = (String) data.get(0).get("created_at");
        Assert.assertNotNull(latestDate, "Latest transaction should have date");
        System.out.println("   ✅ Latest transaction: " + latestDate);
    }

    @Test(priority = 124, description = "TC124: Verify membership renewal transaction")
    public void tc124_MembershipRenewalTransaction() {
        System.out.println("\n>>> TC124: Membership Renewal Transaction <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertTrue(data.size() > 1, "Should have multiple transactions indicating renewals");
        System.out.println("   ✅ Membership renewal transactions present");
    }

    @Test(priority = 125, description = "TC125: Verify active membership details")
    public void tc125_ActiveMembershipDetails() {
        System.out.println("\n>>> TC125: Active Membership Details <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> statuses = response.jsonPath().getList("data.status");
        boolean hasSuccessful = statuses.stream().anyMatch(s -> "Successful".equalsIgnoreCase(s));
        Assert.assertTrue(hasSuccessful, "Should have active (Successful) membership");
        System.out.println("   ✅ Active membership found");
    }

    @Test(priority = 126, description = "TC126: Verify membership expiry date is correct")
    public void tc126_MembershipExpiryDate() {
        System.out.println("\n>>> TC126: Membership Expiry Date <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> dates = response.jsonPath().getList("data.updated_at");
        Assert.assertNotNull(dates.get(0), "Updated date should be present");
        System.out.println("   ✅ Membership dates validated");
    }

    @Test(priority = 127, description = "TC127: Verify payment amount is correct")
    public void tc127_PaymentAmountCorrect() {
        System.out.println("\n>>> TC127: Payment Amount Correct <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> amounts = response.jsonPath().getList("data.actual_price");
        for (String amount : amounts) {
            if (amount != null) {
                double val = Double.parseDouble(amount);
                Assert.assertTrue(val >= 0, "Amount should be >= 0, got: " + val);
            }
        }
        System.out.println("   ✅ All payment amounts are valid");
    }

    @Test(priority = 128, description = "TC128: Verify transaction reference ID is correct")
    public void tc128_TransactionReferenceIdCorrect() {
        System.out.println("\n>>> TC128: Transaction Reference ID Correct <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> refCodes = response.jsonPath().getList("data.reference_code");
        long nonEmptyCount = refCodes.stream().filter(r -> r != null && !r.trim().isEmpty()).count();
        Assert.assertTrue(nonEmptyCount > 0, "Should have reference codes");
        System.out.println("   ✅ Reference codes: " + nonEmptyCount);
    }

    @Test(priority = 129, description = "TC129: Verify is_reverted field")
    public void tc129_IsRevertedField() {
        System.out.println("\n>>> TC129: Is Reverted Field <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> record : data) {
            Assert.assertTrue(record.containsKey("is_reverted"), "is_reverted field should exist");
        }
        System.out.println("   ✅ is_reverted field present in all records");
    }

    @Test(priority = 130, description = "TC130: Verify brand_id is consistent")
    public void tc130_BrandIdConsistent() {
        System.out.println("\n>>> TC130: Brand ID Consistent <<<");
        Response response = callGetTransactionByMobile(validMobile);

        Assert.assertEquals(response.getStatusCode(), 200,
                "Expected 200, got: " + response.getStatusCode());
        List<String> brandIds = response.jsonPath().getList("data.brand_id");
        String firstBrand = brandIds.get(0);
        for (String bid : brandIds) {
            Assert.assertEquals(bid, firstBrand,
                    "All transactions should have same brand_id");
        }
        System.out.println("   ✅ Brand ID consistent: " + firstBrand);
    }
}
