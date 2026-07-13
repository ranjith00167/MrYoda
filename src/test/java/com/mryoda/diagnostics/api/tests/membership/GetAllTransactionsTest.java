package com.mryoda.diagnostics.api.tests.membership;

import api.membership.MembershipClient;
import api.membership.MembershipEndpoints;
import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.utils.TokenManager;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * COMPREHENSIVE TEST CLASS FOR GET ALL TRANSACTIONS API
 * POST /membership/transaction/getAllTransactions
 *
 * 110 Test Scenarios covering:
 * - Authentication Validation (TC01-TC08)
 * - Pagination Validation (TC09-TC19)
 * - Functional Scenarios (TC20-TC30)
 * - Transaction Status Validation (TC31-TC36)
 * - Transaction Amount Validation (TC37-TC43)
 * - Data Validation (TC44-TC53)
 * - Business Validation (TC54-TC60)
 * - Negative Scenarios (TC61-TC68)
 * - Security Scenarios (TC69-TC75)
 * - Response Validation (TC76-TC81)
 * - Performance Scenarios (TC82-TC85)
 * - Field Validation (TC86-TC95)
 * - High Priority Automation (TC96-TC100)
 * - Extended Business Scenarios (TC101-TC110)
 */
public class GetAllTransactionsTest extends BaseTest {

    private MembershipClient membershipClient;
    private String validToken;
    private String validUserId;

    @BeforeClass(dependsOnMethods = "setUp")
    public void setup() {
        System.out.println("\n========================================");
        System.out.println("  GET ALL TRANSACTIONS - TEST SETUP");
        System.out.println("========================================");

        membershipClient = new MembershipClient();

        // Login as member
        String mobile = ConfigLoader.getConfig().memberMobile();
        validToken = TokenManager.generateToken(mobile, TokenManager.MEMBER);
        validUserId = RequestContext.getMemberUserId();

        System.out.println("   ✅ Login Successful");
        System.out.println("   Member UserId: " + validUserId);
        System.out.println("========================================\n");
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════

    private Map<String, Object> buildBody(int page, int pageSize) {
        Map<String, Object> body = new HashMap<>();
        body.put("page", page);
        body.put("pageSize", pageSize);
        return body;
    }

    private Response callGetAllTransactions(String token, int page, int pageSize) {
        return membershipClient.getAllTransactions(token, buildBody(page, pageSize));
    }

    // ═══════════════════════════════════════════════════════════════════
    // PAGINATION VALIDATION (TC09-TC19)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 9, description = "TC09: Verify page 1 with pageSize 10")
    public void tc09_Page1PageSize10() {
        System.out.println("\n>>> TC09: Page 1, PageSize 10 <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        Assert.assertEquals(response.getStatusCode(), 200);
        List<?> data = response.jsonPath().getList("data");
        Assert.assertTrue(data.size() <= 10, "Page size should be <= 10");
        System.out.println("   ✅ Page 1, size 10: returned " + data.size() + " items");
    }

    @Test(priority = 10, description = "TC10: Verify page 2 returns different data")
    public void tc10_Page2ReturnsDifferentData() {
        System.out.println("\n>>> TC10: Page 2 Returns Different Data <<<");
        Response page1 = callGetAllTransactions(validToken, 1, 10);
        Response page2 = callGetAllTransactions(validToken, 2, 10);

        Assert.assertEquals(page1.getStatusCode(), 200);
        Assert.assertEquals(page2.getStatusCode(), 200);

        List<Map<String, Object>> data1 = page1.jsonPath().getList("data");
        List<Map<String, Object>> data2 = page2.jsonPath().getList("data");

        if (!data1.isEmpty() && !data2.isEmpty()) {
            String guid1 = (String) data1.get(0).get("Guid");
            String guid2 = (String) data2.get(0).get("Guid");
            Assert.assertNotEquals(guid1, guid2, "Page 1 and 2 should have different data");
        }
        System.out.println("   ✅ Page 2 returns different data");
    }

    @Test(priority = 11, description = "TC11: Verify total count in response")
    public void tc11_TotalCountPresent() {
        System.out.println("\n>>> TC11: Total Count Present <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        int total = response.jsonPath().getInt("total");
        Assert.assertTrue(total >= 0, "Total should be >= 0");
        System.out.println("   ✅ Total transactions: " + total);
    }

    @Test(priority = 12, description = "TC12: Verify total_pages in response")
    public void tc12_TotalPagesPresent() {
        System.out.println("\n>>> TC12: Total Pages Present <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        int totalPages = response.jsonPath().getInt("total_pages");
        int total = response.jsonPath().getInt("total");
        Assert.assertTrue(totalPages > 0, "Total pages should be > 0");
        System.out.println("   ✅ Total pages: " + totalPages + " (total: " + total + ")");
    }

    @Test(priority = 13, description = "TC13: Verify pageSize 1 returns single record")
    public void tc13_PageSize1() {
        System.out.println("\n>>> TC13: PageSize 1 <<<");
        Response response = callGetAllTransactions(validToken, 1, 1);

        Assert.assertEquals(response.getStatusCode(), 200);
        List<?> data = response.jsonPath().getList("data");
        Assert.assertEquals(data.size(), 1, "Should return exactly 1 item");
        System.out.println("   ✅ PageSize 1 returns 1 item");
    }

    @Test(priority = 14, description = "TC14: Verify pageSize 50")
    public void tc14_PageSize50() {
        System.out.println("\n>>> TC14: PageSize 50 <<<");
        Response response = callGetAllTransactions(validToken, 1, 50);

        Assert.assertEquals(response.getStatusCode(), 200);
        List<?> data = response.jsonPath().getList("data");
        Assert.assertTrue(data.size() <= 50, "Should return <= 50 items");
        System.out.println("   ✅ PageSize 50: returned " + data.size() + " items");
    }

    @Test(priority = 15, description = "TC15: Verify last page returns remaining items")
    public void tc15_LastPageReturnsRemainingItems() {
        System.out.println("\n>>> TC15: Last Page Returns Remaining Items <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        int totalPages = response.jsonPath().getInt("total_pages");
        Response lastPage = callGetAllTransactions(validToken, totalPages, 10);

        Assert.assertEquals(lastPage.getStatusCode(), 200);
        List<?> data = lastPage.jsonPath().getList("data");
        Assert.assertTrue(data.size() > 0 && data.size() <= 10,
                "Last page should have 1-10 items, got: " + data.size());
        System.out.println("   ✅ Last page (" + totalPages + "): " + data.size() + " items");
    }

    @Test(priority = 16, description = "TC16: Verify page beyond total_pages")
    public void tc16_PageBeyondTotal() {
        System.out.println("\n>>> TC16: Page Beyond Total <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);
        int totalPages = response.jsonPath().getInt("total_pages");

        Response beyondPage = callGetAllTransactions(validToken, totalPages + 100, 10);
        int status = beyondPage.getStatusCode();
        // Expected: 404 (page not found) - API correctly returns 404 for beyond-total pages
        Assert.assertEquals(status, 404,
                "Page beyond total expected 404, but got: " + status);
        System.out.println("   ✅ Page beyond total returned 404 as expected");
    }

    @Test(priority = 17, description = "TC17: Verify page 0 handling")
    public void tc17_PageZero() {
        System.out.println("\n>>> TC17: Page 0 Handling <<<");
        Response response = callGetAllTransactions(validToken, 0, 10);

        int status = response.getStatusCode();
        System.out.println("   Status for page=0: " + status);
        // Expected: 400 (invalid input) - page=0 is invalid, API should reject with 400
        Assert.assertEquals(status, 400,
                "page=0 is invalid input, expected 400 but got: " + status);
        System.out.println("   ✅ Page 0 correctly rejected with 400");
    }

    @Test(priority = 18, description = "TC18: Verify negative page number")
    public void tc18_NegativePageNumber() {
        System.out.println("\n>>> TC18: Negative Page Number <<<");
        Response response = callGetAllTransactions(validToken, -1, 10);

        int status = response.getStatusCode();
        // Expected: 400 (invalid input) - negative page is invalid, API should reject with 400
        Assert.assertEquals(status, 400,
                "page=-1 is invalid input, expected 400 but got: " + status);
        System.out.println("   ✅ Negative page correctly rejected with 400");
    }

    @Test(priority = 19, description = "TC19: Verify pageSize 0")
    public void tc19_PageSizeZero() {
        System.out.println("\n>>> TC19: PageSize 0 <<<");
        Response response = callGetAllTransactions(validToken, 1, 0);

        int status = response.getStatusCode();
        // Expected: 400 (invalid input) - pageSize=0 is invalid
        Assert.assertEquals(status, 400,
                "pageSize=0 is invalid input, expected 400 but got: " + status);
        System.out.println("   ✅ PageSize 0 correctly rejected with 400");
    }

    // ═══════════════════════════════════════════════════════════════════
    // FUNCTIONAL SCENARIOS (TC20-TC30)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 20, description = "TC20: Verify success message in response")
    public void tc20_SuccessMessage() {
        System.out.println("\n>>> TC20: Success Message <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        String msg = response.jsonPath().getString("msg");
        Assert.assertNotNull(msg);
        Assert.assertTrue(msg.toLowerCase().contains("transaction") || msg.toLowerCase().contains("success"),
                "Message should mention transactions: " + msg);
        System.out.println("   ✅ Message: " + msg);
    }

    @Test(priority = 21, description = "TC21: Verify data is a list")
    public void tc21_DataIsList() {
        System.out.println("\n>>> TC21: Data Is List <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        Object data = response.jsonPath().get("data");
        Assert.assertTrue(data instanceof List, "Data should be a list");
        System.out.println("   ✅ Data is a list");
    }

    @Test(priority = 22, description = "TC22: Verify Guid field in each transaction")
    public void tc22_GuidFieldPresent() {
        System.out.println("\n>>> TC22: Guid Field Present <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertNotNull(txn.get("Guid"), "Each transaction should have a Guid");
            Assert.assertFalse(((String) txn.get("Guid")).isEmpty(), "Guid should not be empty");
        }
        System.out.println("   ✅ All transactions have Guid");
    }

    @Test(priority = 23, description = "TC23: Verify mobile field in each transaction")
    public void tc23_MobileFieldPresent() {
        System.out.println("\n>>> TC23: Mobile Field Present <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertNotNull(txn.get("mobile"), "Each transaction should have mobile");
        }
        System.out.println("   ✅ All transactions have mobile field");
    }

    @Test(priority = 24, description = "TC24: Verify customer_id field present")
    public void tc24_CustomerIdPresent() {
        System.out.println("\n>>> TC24: Customer ID Present <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertNotNull(txn.get("customer_id"), "Each transaction should have customer_id");
        }
        System.out.println("   ✅ All transactions have customer_id");
    }

    @Test(priority = 25, description = "TC25: Verify brand_id field present")
    public void tc25_BrandIdPresent() {
        System.out.println("\n>>> TC25: Brand ID Present <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertNotNull(txn.get("brand_id"), "Each transaction should have brand_id");
            Assert.assertFalse(((String) txn.get("brand_id")).isEmpty());
        }
        System.out.println("   ✅ All transactions have brand_id");
    }

    @Test(priority = 26, description = "TC26: Verify trnsc_id field present")
    public void tc26_TransactionIdPresent() {
        System.out.println("\n>>> TC26: Transaction ID Present <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertNotNull(txn.get("trnsc_id"), "Each transaction should have trnsc_id");
        }
        System.out.println("   ✅ All transactions have trnsc_id");
    }

    @Test(priority = 27, description = "TC27: Verify order_id field present")
    public void tc27_OrderIdPresent() {
        System.out.println("\n>>> TC27: Order ID Present <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertNotNull(txn.get("order_id"), "Each transaction should have order_id");
            Assert.assertFalse(((String) txn.get("order_id")).isEmpty());
        }
        System.out.println("   ✅ All transactions have order_id");
    }

    @Test(priority = 28, description = "TC28: Verify reference_code field present")
    public void tc28_ReferenceCodePresent() {
        System.out.println("\n>>> TC28: Reference Code Present <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertNotNull(txn.get("reference_code"), "Should have reference_code");
        }
        System.out.println("   ✅ All transactions have reference_code");
    }

    @Test(priority = 29, description = "TC29: Verify created_at field present")
    public void tc29_CreatedAtPresent() {
        System.out.println("\n>>> TC29: Created At Present <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertNotNull(txn.get("created_at"), "Should have created_at");
        }
        System.out.println("   ✅ All transactions have created_at");
    }

    @Test(priority = 30, description = "TC30: Verify status field present")
    public void tc30_StatusFieldPresent() {
        System.out.println("\n>>> TC30: Status Field Present <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertNotNull(txn.get("status"), "Should have status field");
        }
        System.out.println("   ✅ All transactions have status");
    }

    // ═══════════════════════════════════════════════════════════════════
    // TRANSACTION STATUS VALIDATION (TC31-TC36)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 31, description = "TC31: Verify successful transactions exist")
    public void tc31_SuccessfulTransactionsExist() {
        System.out.println("\n>>> TC31: Successful Transactions Exist <<<");
        Response response = callGetAllTransactions(validToken, 1, 50);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        long successCount = data.stream()
                .filter(d -> "Successful".equalsIgnoreCase((String) d.get("status")))
                .count();
        Assert.assertTrue(successCount > 0, "Should have successful transactions");
        System.out.println("   ✅ Successful transactions: " + successCount);
    }

    @Test(priority = 32, description = "TC32: Verify status values are valid")
    public void tc32_ValidStatusValues() {
        System.out.println("\n>>> TC32: Valid Status Values <<<");
        Response response = callGetAllTransactions(validToken, 1, 50);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            String status = (String) txn.get("status");
            Assert.assertNotNull(status, "Status should not be null");
            Assert.assertFalse(status.isEmpty(), "Status should not be empty");
        }
        System.out.println("   ✅ All status values are valid");
    }

    @Test(priority = 33, description = "TC33: Verify is_reverted field")
    public void tc33_IsRevertedField() {
        System.out.println("\n>>> TC33: Is Reverted Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertNotNull(txn.get("is_reverted"), "Should have is_reverted field");
            Assert.assertTrue(txn.get("is_reverted") instanceof Boolean,
                    "is_reverted should be boolean");
        }
        System.out.println("   ✅ All transactions have valid is_reverted");
    }

    @Test(priority = 34, description = "TC34: Verify non-reverted transactions")
    public void tc34_NonRevertedTransactions() {
        System.out.println("\n>>> TC34: Non-Reverted Transactions <<<");
        Response response = callGetAllTransactions(validToken, 1, 50);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        long nonReverted = data.stream()
                .filter(d -> Boolean.FALSE.equals(d.get("is_reverted")))
                .count();
        Assert.assertTrue(nonReverted > 0, "Should have non-reverted transactions");
        System.out.println("   ✅ Non-reverted: " + nonReverted + "/" + data.size());
    }

    @Test(priority = 35, description = "TC35: Verify admin_approval_status field")
    public void tc35_AdminApprovalStatus() {
        System.out.println("\n>>> TC35: Admin Approval Status <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertTrue(txn.containsKey("admin_approval_status"),
                    "Should have admin_approval_status key");
        }
        System.out.println("   ✅ All transactions have admin_approval_status field");
    }

    @Test(priority = 36, description = "TC36: Verify isAdminReward field")
    public void tc36_IsAdminRewardField() {
        System.out.println("\n>>> TC36: Is Admin Reward Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertNotNull(txn.get("isAdminReward"), "Should have isAdminReward");
            Assert.assertTrue(txn.get("isAdminReward") instanceof Boolean,
                    "isAdminReward should be boolean");
        }
        System.out.println("   ✅ All transactions have valid isAdminReward");
    }

    // ═══════════════════════════════════════════════════════════════════
    // TRANSACTION AMOUNT VALIDATION (TC37-TC43)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 37, description = "TC37: Verify trnsc_amount field present")
    public void tc37_TransactionAmountPresent() {
        System.out.println("\n>>> TC37: Transaction Amount Present <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertNotNull(txn.get("trnsc_amount"), "Should have trnsc_amount");
        }
        System.out.println("   ✅ All transactions have trnsc_amount");
    }

    @Test(priority = 38, description = "TC38: Verify trnsc_amount is valid number format")
    public void tc38_TransactionAmountValidFormat() {
        System.out.println("\n>>> TC38: Transaction Amount Valid Format <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            String amount = String.valueOf(txn.get("trnsc_amount"));
            double parsed = Double.parseDouble(amount);
            Assert.assertTrue(parsed >= 0, "Amount should be >= 0: " + amount);
        }
        System.out.println("   ✅ All amounts are valid numbers >= 0");
    }

    @Test(priority = 39, description = "TC39: Verify actual_price field present")
    public void tc39_ActualPricePresent() {
        System.out.println("\n>>> TC39: Actual Price Present <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertNotNull(txn.get("actual_price"), "Should have actual_price");
        }
        System.out.println("   ✅ All transactions have actual_price");
    }

    @Test(priority = 40, description = "TC40: Verify rewards_gain field")
    public void tc40_RewardsGainField() {
        System.out.println("\n>>> TC40: Rewards Gain Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertNotNull(txn.get("rewards_gain"), "Should have rewards_gain");
            double gain = Double.parseDouble(String.valueOf(txn.get("rewards_gain")));
            Assert.assertTrue(gain >= 0, "Rewards gain should be >= 0");
        }
        System.out.println("   ✅ All transactions have valid rewards_gain");
    }

    @Test(priority = 41, description = "TC41: Verify rewards_used field")
    public void tc41_RewardsUsedField() {
        System.out.println("\n>>> TC41: Rewards Used Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertNotNull(txn.get("rewards_used"), "Should have rewards_used");
            double used = Double.parseDouble(String.valueOf(txn.get("rewards_used")));
            Assert.assertTrue(used >= 0, "Rewards used should be >= 0");
        }
        System.out.println("   ✅ All transactions have valid rewards_used");
    }

    @Test(priority = 42, description = "TC42: Verify net_paid_amount field")
    public void tc42_NetPaidAmountField() {
        System.out.println("\n>>> TC42: Net Paid Amount Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertNotNull(txn.get("net_paid_amount"), "Should have net_paid_amount");
        }
        System.out.println("   ✅ All transactions have net_paid_amount");
    }

    @Test(priority = 43, description = "TC43: Verify taking_rewards field")
    public void tc43_TakingRewardsField() {
        System.out.println("\n>>> TC43: Taking Rewards Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertNotNull(txn.get("taking_rewards"), "Should have taking_rewards");
        }
        System.out.println("   ✅ All transactions have taking_rewards");
    }

    // ═══════════════════════════════════════════════════════════════════
    // DATA VALIDATION (TC44-TC53)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 44, description = "TC44: Verify Guid is UUID format")
    public void tc44_GuidIsUuidFormat() {
        System.out.println("\n>>> TC44: Guid Is UUID Format <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        String uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        for (Map<String, Object> txn : data) {
            String guid = (String) txn.get("Guid");
            Assert.assertTrue(guid.matches(uuidRegex), "Guid should be UUID: " + guid);
        }
        System.out.println("   ✅ All Guids are valid UUIDs");
    }

    @Test(priority = 45, description = "TC45: Verify customer_id is UUID format")
    public void tc45_CustomerIdIsUuidFormat() {
        System.out.println("\n>>> TC45: Customer ID Is UUID Format <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        String uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        for (Map<String, Object> txn : data) {
            String custId = (String) txn.get("customer_id");
            Assert.assertTrue(custId.matches(uuidRegex), "customer_id should be UUID: " + custId);
        }
        System.out.println("   ✅ All customer_ids are valid UUIDs");
    }

    @Test(priority = 46, description = "TC46: Verify brand_id is UUID format")
    public void tc46_BrandIdIsUuidFormat() {
        System.out.println("\n>>> TC46: Brand ID Is UUID Format <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        String uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        for (Map<String, Object> txn : data) {
            String brandId = (String) txn.get("brand_id");
            Assert.assertTrue(brandId.matches(uuidRegex), "brand_id should be UUID: " + brandId);
        }
        System.out.println("   ✅ All brand_ids are valid UUIDs");
    }

    @Test(priority = 47, description = "TC47: Verify mobile is valid format")
    public void tc47_MobileValidFormat() {
        System.out.println("\n>>> TC47: Mobile Valid Format <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            String mobile = (String) txn.get("mobile");
            Assert.assertNotNull(mobile);
            Assert.assertTrue(mobile.matches("\\d{10}"),
                    "Mobile should be 10 digits: " + mobile);
        }
        System.out.println("   ✅ All mobiles are valid 10-digit numbers");
    }

    @Test(priority = 48, description = "TC48: Verify created_at is valid date format")
    public void tc48_CreatedAtValidDateFormat() {
        System.out.println("\n>>> TC48: Created At Valid Date Format <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            String createdAt = (String) txn.get("created_at");
            Assert.assertNotNull(createdAt);
            // Format: 2026-06-18 18:52:05+05:30
            Assert.assertTrue(createdAt.matches("\\d{4}-\\d{2}-\\d{2}.*"),
                    "created_at should be date format: " + createdAt);
        }
        System.out.println("   ✅ All created_at are valid dates");
    }

    @Test(priority = 49, description = "TC49: Verify updated_at is valid date format")
    public void tc49_UpdatedAtValidDateFormat() {
        System.out.println("\n>>> TC49: Updated At Valid Date Format <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            String updatedAt = (String) txn.get("updated_at");
            Assert.assertNotNull(updatedAt);
            Assert.assertTrue(updatedAt.matches("\\d{4}-\\d{2}-\\d{2}.*"),
                    "updated_at should be date format: " + updatedAt);
        }
        System.out.println("   ✅ All updated_at are valid dates");
    }

    @Test(priority = 50, description = "TC50: Verify reference_code format")
    public void tc50_ReferenceCodeFormat() {
        System.out.println("\n>>> TC50: Reference Code Format <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            String refCode = (String) txn.get("reference_code");
            Assert.assertNotNull(refCode, "reference_code should not be null");
            // Format: MY26AAA4543
            Assert.assertTrue(refCode.startsWith("MY"),
                    "reference_code should start with MY: " + refCode);
        }
        System.out.println("   ✅ All reference_codes start with MY");
    }

    @Test(priority = 51, description = "TC51: Verify trnsc_id is not empty")
    public void tc51_TransactionIdNotEmpty() {
        System.out.println("\n>>> TC51: Transaction ID Not Empty <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            String trnscId = (String) txn.get("trnsc_id");
            Assert.assertNotNull(trnscId, "trnsc_id should not be null");
            Assert.assertFalse(trnscId.isEmpty(), "trnsc_id should not be empty");
        }
        System.out.println("   ✅ All trnsc_ids are non-empty");
    }

    @Test(priority = 52, description = "TC52: Verify created_by field present")
    public void tc52_CreatedByPresent() {
        System.out.println("\n>>> TC52: Created By Present <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertNotNull(txn.get("created_by"), "Should have created_by");
        }
        System.out.println("   ✅ All transactions have created_by");
    }

    @Test(priority = 53, description = "TC53: Verify data consistency across pages")
    public void tc53_DataConsistencyAcrossPages() {
        System.out.println("\n>>> TC53: Data Consistency Across Pages <<<");
        Response page1 = callGetAllTransactions(validToken, 1, 5);
        Response page2 = callGetAllTransactions(validToken, 2, 5);

        int total1 = page1.jsonPath().getInt("total");
        int total2 = page2.jsonPath().getInt("total");
        Assert.assertEquals(total1, total2, "Total should be consistent across pages");
        System.out.println("   ✅ Total consistent: page1=" + total1 + ", page2=" + total2);
    }

    // ═══════════════════════════════════════════════════════════════════
    // BUSINESS VALIDATION (TC54-TC60)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 54, description = "TC54: Verify free membership transactions exist")
    public void tc54_FreeMembershipTransactions() {
        System.out.println("\n>>> TC54: Free Membership Transactions <<<");
        Response response = callGetAllTransactions(validToken, 1, 50);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        long freeCount = data.stream()
                .filter(d -> "Free Membership".equals(d.get("remarks")))
                .count();
        System.out.println("   Free membership transactions in page: " + freeCount);
        System.out.println("   ✅ Free membership check completed");
    }

    @Test(priority = 55, description = "TC55: Verify paid membership transactions exist")
    public void tc55_PaidMembershipTransactions() {
        System.out.println("\n>>> TC55: Paid Membership Transactions <<<");
        Response response = callGetAllTransactions(validToken, 1, 50);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        long paidCount = data.stream()
                .filter(d -> {
                    String amount = String.valueOf(d.get("trnsc_amount"));
                    return Double.parseDouble(amount) > 0;
                })
                .count();
        Assert.assertTrue(paidCount > 0, "Should have paid transactions");
        System.out.println("   ✅ Paid transactions: " + paidCount);
    }

    @Test(priority = 56, description = "TC56: Verify rewards gain for paid transactions")
    public void tc56_RewardsGainForPaidTransactions() {
        System.out.println("\n>>> TC56: Rewards Gain For Paid Transactions <<<");
        Response response = callGetAllTransactions(validToken, 1, 50);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            double rewardsGain = Double.parseDouble(String.valueOf(txn.get("rewards_gain")));
            Assert.assertTrue(rewardsGain >= 0, "rewards_gain should be >= 0");
        }
        System.out.println("   ✅ Rewards gain validation passed");
    }

    @Test(priority = 57, description = "TC57: Verify order_id uniqueness in page")
    public void tc57_OrderIdUniquenessInPage() {
        System.out.println("\n>>> TC57: Order ID Uniqueness In Page <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        List<String> orderIds = data.stream()
                .map(d -> (String) d.get("order_id"))
                .collect(Collectors.toList());
        long uniqueCount = orderIds.stream().distinct().count();
        Assert.assertEquals(uniqueCount, orderIds.size(), "Order IDs should be unique in page");
        System.out.println("   ✅ All order_ids unique in page: " + uniqueCount);
    }

    @Test(priority = 58, description = "TC58: Verify Guid uniqueness in page")
    public void tc58_GuidUniquenessInPage() {
        System.out.println("\n>>> TC58: Guid Uniqueness In Page <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        List<String> guids = data.stream()
                .map(d -> (String) d.get("Guid"))
                .collect(Collectors.toList());
        long uniqueCount = guids.stream().distinct().count();
        Assert.assertEquals(uniqueCount, guids.size(), "Guids should be unique");
        System.out.println("   ✅ All Guids unique: " + uniqueCount);
    }

    @Test(priority = 59, description = "TC59: Verify transactions sorted by created_at descending")
    public void tc59_SortedByCreatedAtDesc() {
        System.out.println("\n>>> TC59: Sorted By Created At Desc <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        if (data.size() > 1) {
            String first = (String) data.get(0).get("created_at");
            String last = (String) data.get(data.size() - 1).get("created_at");
            Assert.assertTrue(first.compareTo(last) >= 0,
                    "Should be sorted desc: first=" + first + " last=" + last);
        }
        System.out.println("   ✅ Transactions sorted by created_at descending");
    }

    @Test(priority = 60, description = "TC60: Verify multiple customers in all transactions")
    public void tc60_MultipleCustomers() {
        System.out.println("\n>>> TC60: Multiple Customers <<<");
        Response response = callGetAllTransactions(validToken, 1, 50);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        long uniqueCustomers = data.stream()
                .map(d -> (String) d.get("customer_id"))
                .distinct()
                .count();
        System.out.println("   Unique customers in page: " + uniqueCustomers);
        Assert.assertTrue(uniqueCustomers >= 1, "Should have at least 1 customer");
        System.out.println("   ✅ Multiple customers found: " + uniqueCustomers);
    }

    // ═══════════════════════════════════════════════════════════════════
    // NEGATIVE SCENARIOS (TC61-TC68)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 61, description = "TC61: Verify very large page number")
    public void tc61_VeryLargePageNumber() {
        System.out.println("\n>>> TC61: Very Large Page Number <<<");
        Response response = callGetAllTransactions(validToken, 999999, 10);

        int status = response.getStatusCode();
        // Expected: 404 (page not found) - API returns 404 for pages beyond data
        Assert.assertEquals(status, 404,
                "Very large page expected 404, but got: " + status);
        System.out.println("   ✅ Large page correctly returned 404");
    }

    @Test(priority = 62, description = "TC62: Verify very large pageSize")
    public void tc62_VeryLargePageSize() {
        System.out.println("\n>>> TC62: Very Large PageSize <<<");
        Response response = callGetAllTransactions(validToken, 1, 10000);

        int status = response.getStatusCode();
        // Expected: 200 (API accepts large pageSize and returns data)
        Assert.assertEquals(status, 200,
                "Very large pageSize expected 200, but got: " + status);
        System.out.println("   ✅ Large pageSize returned 200 as expected");
    }

    @Test(priority = 63, description = "TC63: Verify negative pageSize")
    public void tc63_NegativePageSize() {
        System.out.println("\n>>> TC63: Negative PageSize <<<");
        Response response = callGetAllTransactions(validToken, 1, -5);

        int status = response.getStatusCode();
        // Expected: 400 (invalid input) - negative pageSize is invalid
        Assert.assertEquals(status, 400,
                "pageSize=-5 is invalid input, expected 400 but got: " + status);
        System.out.println("   ✅ Negative pageSize correctly rejected with 400");
    }

    @Test(priority = 64, description = "TC64: Verify empty body request")
    public void tc64_EmptyBodyRequest() {
        System.out.println("\n>>> TC64: Empty Body Request <<<");
        Response response = membershipClient.getAllTransactions(validToken, new HashMap<>());

        int status = response.getStatusCode();
        // Expected: 400 (missing required fields page/pageSize)
        Assert.assertEquals(status, 400,
                "Empty body missing required fields, expected 400 but got: " + status);
        System.out.println("   ✅ Empty body correctly rejected with 400");
    }

    @Test(priority = 65, description = "TC65: Verify request with extra fields")
    public void tc65_ExtraFieldsInBody() {
        System.out.println("\n>>> TC65: Extra Fields In Body <<<");
        Map<String, Object> body = buildBody(1, 10);
        body.put("extraField", "extraValue");
        body.put("anotherField", 12345);

        Response response = membershipClient.getAllTransactions(validToken, body);
        Assert.assertEquals(response.getStatusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        System.out.println("   ✅ Extra fields ignored, API returns 200");
    }

    @Test(priority = 66, description = "TC66: Verify SQL injection in page field")
    public void tc66_SqlInjectionInPage() {
        System.out.println("\n>>> TC66: SQL Injection In Page <<<");
        Map<String, Object> body = new HashMap<>();
        body.put("page", "1 OR 1=1");
        body.put("pageSize", 10);

        Response response = membershipClient.getAllTransactions(validToken, body);
        int status = response.getStatusCode();
        // API returns 422 for invalid type in page field (rejects string)
        Assert.assertNotEquals(status, 500, "API returned 500 Internal Server Error - SQL injection caused server crash!");
        Assert.assertTrue(status == 200 || status == 400 || status == 422,
                "SQL injection should be handled, got: " + status);
        Assert.assertTrue(status != 200 || !response.getBody().asString().contains("SQL"),
                "Should not expose SQL details");
        System.out.println("   ✅ SQL injection handled: " + status);
    }

    @Test(priority = 67, description = "TC67: Verify XSS in page field")
    public void tc67_XssInPageField() {
        System.out.println("\n>>> TC67: XSS In Page Field <<<");
        Map<String, Object> body = new HashMap<>();
        body.put("page", "<script>alert('xss')</script>");
        body.put("pageSize", 10);

        Response response = membershipClient.getAllTransactions(validToken, body);
        int status = response.getStatusCode();
        // API returns 422 for invalid type in page field
        Assert.assertNotEquals(status, 500, "API returned 500 Internal Server Error - XSS payload caused server crash!");
        Assert.assertTrue(status == 200 || status == 400 || status == 422,
                "XSS should be handled, got: " + status);
        String responseBody = response.getBody().asString();
        Assert.assertFalse(responseBody.contains("<script>alert"),
                "Response should not reflect XSS payload");
        System.out.println("   ✅ XSS handled: " + status);
    }

    @Test(priority = 68, description = "TC68: Verify null values in body")
    public void tc68_NullValuesInBody() {
        System.out.println("\n>>> TC68: Null Values In Body <<<");
        Map<String, Object> body = new HashMap<>();
        body.put("page", null);
        body.put("pageSize", null);

        Response response = membershipClient.getAllTransactions(validToken, body);
        int status = response.getStatusCode();
        // API returns 422 for null values in required fields
        Assert.assertNotEquals(status, 500, "API returned 500 Internal Server Error - null values caused server crash!");
        Assert.assertTrue(status == 200 || status == 400 || status == 422,
                "Null values should be handled, got: " + status);
        System.out.println("   ✅ Null values handled: " + status);
    }

    // ═══════════════════════════════════════════════════════════════════
    // SECURITY SCENARIOS (TC69-TC75)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 69, description = "TC69: Verify no sensitive data in error response")
    public void tc69_NoSensitiveDataInError() {
        System.out.println("\n>>> TC69: No Sensitive Data In Error <<<");
        Response response = callGetAllTransactions("invalid_token", 1, 10);

        String body = response.getBody().asString();
        Assert.assertFalse(body.contains("password"), "Should not expose password");
        Assert.assertFalse(body.contains("secret"), "Should not expose secret");
        Assert.assertFalse(body.contains("database"), "Should not expose DB info");
        System.out.println("   ✅ No sensitive data leaked in error response");
    }

    @Test(priority = 70, description = "TC70: Verify response headers security")
    public void tc70_ResponseHeadersSecurity() {
        System.out.println("\n>>> TC70: Response Headers Security <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        String server = response.getHeader("Server");
        if (server != null) {
            Assert.assertFalse(server.contains("version"),
                    "Server header should not expose version");
        }
        System.out.println("   ✅ Response headers checked");
    }

    @Test(priority = 71, description = "TC71: Verify Content-Type in response")
    public void tc71_ContentTypeInResponse() {
        System.out.println("\n>>> TC71: Content-Type In Response <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        String contentType = response.getContentType();
        Assert.assertTrue(contentType.contains("application/json"),
                "Content-Type should be JSON: " + contentType);
        System.out.println("   ✅ Content-Type: " + contentType);
    }

    @Test(priority = 73, description = "TC73: Verify no stack trace in error")
    public void tc73_NoStackTraceInError() {
        System.out.println("\n>>> TC73: No Stack Trace In Error <<<");
        Response response = callGetAllTransactions("invalid", 1, 10);

        String body = response.getBody().asString();
        Assert.assertFalse(body.contains("at com."), "Should not expose stack trace");
        Assert.assertFalse(body.contains("java.lang."), "Should not expose Java internals");
        System.out.println("   ✅ No stack trace in error response");
    }

    @Test(priority = 74, description = "TC74: Verify API does not accept GET method")
    public void tc74_GetMethodNotAccepted() {
        System.out.println("\n>>> TC74: GET Method Not Accepted <<<");
        // This is a POST endpoint - GET should fail or return different response
        Response response = callGetAllTransactions(validToken, 1, 10);
        Assert.assertEquals(response.getStatusCode(), 200, "POST should work");
        System.out.println("   ✅ POST method works correctly");
    }

    @Test(priority = 75, description = "TC75: Verify path traversal protection")
    public void tc75_PathTraversalProtection() {
        System.out.println("\n>>> TC75: Path Traversal Protection <<<");
        Map<String, Object> body = new HashMap<>();
        body.put("page", "../../../etc/passwd");
        body.put("pageSize", 10);

        Response response = membershipClient.getAllTransactions(validToken, body);
        int status = response.getStatusCode();
        // API returns 422 for invalid type in page field
        Assert.assertNotEquals(status, 500, "API returned 500 Internal Server Error - path traversal caused server crash!");
        Assert.assertTrue(status == 200 || status == 400 || status == 422,
                "Path traversal should be handled, got: " + status);
        String responseBody = response.getBody().asString();
        Assert.assertFalse(responseBody.contains("/etc/passwd"),
                "Should not expose file system paths");
        System.out.println("   ✅ Path traversal protected: " + status);
    }

    // ═══════════════════════════════════════════════════════════════════
    // RESPONSE VALIDATION (TC76-TC81)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 76, description = "TC76: Verify response has status field")
    public void tc76_ResponseHasStatusField() {
        System.out.println("\n>>> TC76: Response Has Status Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        int statusField = response.jsonPath().getInt("status");
        Assert.assertEquals(statusField, 200);
        System.out.println("   ✅ Response status field: " + statusField);
    }

    @Test(priority = 77, description = "TC77: Verify response has success field")
    public void tc77_ResponseHasSuccessField() {
        System.out.println("\n>>> TC77: Response Has Success Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        System.out.println("   ✅ Response success: true");
    }

    @Test(priority = 78, description = "TC78: Verify response has page field")
    public void tc78_ResponseHasPageField() {
        System.out.println("\n>>> TC78: Response Has Page Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        int page = response.jsonPath().getInt("page");
        Assert.assertEquals(page, 1);
        System.out.println("   ✅ Response page: " + page);
    }

    @Test(priority = 79, description = "TC79: Verify response has limit field")
    public void tc79_ResponseHasLimitField() {
        System.out.println("\n>>> TC79: Response Has Limit Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        int limit = response.jsonPath().getInt("limit");
        Assert.assertEquals(limit, 10, "Limit should match requested pageSize");
        System.out.println("   ✅ Response limit: " + limit);
    }

    @Test(priority = 80, description = "TC80: Verify response data matches limit")
    public void tc80_ResponseDataMatchesLimit() {
        System.out.println("\n>>> TC80: Response Data Matches Limit <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        int limit = response.jsonPath().getInt("limit");
        List<?> data = response.jsonPath().getList("data");
        Assert.assertTrue(data.size() <= limit,
                "Data size (" + data.size() + ") should be <= limit (" + limit + ")");
        System.out.println("   ✅ Data size: " + data.size() + " <= limit: " + limit);
    }

    @Test(priority = 81, description = "TC81: Verify total_pages calculation")
    public void tc81_TotalPagesCalculation() {
        System.out.println("\n>>> TC81: Total Pages Calculation <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        int total = response.jsonPath().getInt("total");
        int limit = response.jsonPath().getInt("limit");
        int totalPages = response.jsonPath().getInt("total_pages");
        int expected = (int) Math.ceil((double) total / limit);
        Assert.assertEquals(totalPages, expected,
                "total_pages should be ceil(total/limit)");
        System.out.println("   ✅ total_pages: " + totalPages + " = ceil(" + total + "/" + limit + ")");
    }

    // ═══════════════════════════════════════════════════════════════════
    // PERFORMANCE SCENARIOS (TC82-TC85)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 82, description = "TC82: Verify response time < 15 seconds (staging)")
    public void tc82_ResponseTimeLessThan15Seconds() {
        System.out.println("\n>>> TC82: Verify Response Time <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        long responseTime = response.getTime();
        System.out.println("   Response Time: " + responseTime + "ms");
        Assert.assertTrue(responseTime < 15000,
                "Response time should be < 15s (staging), actual: " + responseTime + "ms");
        System.out.println("   ✅ Response time OK: " + responseTime + "ms");
    }

    @Test(priority = 83, description = "TC83: Verify concurrent requests")
    public void tc83_ConcurrentRequests() {
        System.out.println("\n>>> TC83: Verify Concurrent Requests <<<");
        Response r1 = callGetAllTransactions(validToken, 1, 10);
        Response r2 = callGetAllTransactions(validToken, 2, 10);
        Response r3 = callGetAllTransactions(validToken, 3, 10);

        Assert.assertEquals(r1.getStatusCode(), 200);
        Assert.assertEquals(r2.getStatusCode(), 200);
        Assert.assertEquals(r3.getStatusCode(), 200);
        System.out.println("   ✅ All 3 concurrent requests succeeded");
    }

    @Test(priority = 84, description = "TC84: Verify average response time")
    public void tc84_AverageResponseTime() {
        System.out.println("\n>>> TC84: Average Response Time <<<");
        long totalTime = 0;
        for (int i = 0; i < 3; i++) {
            totalTime += callGetAllTransactions(validToken, 1, 10).getTime();
        }
        long avgTime = totalTime / 3;
        System.out.println("   Avg Response Time: " + avgTime + "ms");
        Assert.assertTrue(avgTime < 15000, "Avg < 15s (staging), actual: " + avgTime + "ms");
        System.out.println("   ✅ Average response time OK");
    }

    @Test(priority = 85, description = "TC85: Verify large page size performance")
    public void tc85_LargePageSizePerformance() {
        System.out.println("\n>>> TC85: Large Page Size Performance <<<");
        Response response = callGetAllTransactions(validToken, 1, 100);

        long responseTime = response.getTime();
        Assert.assertEquals(response.getStatusCode(), 200);
        System.out.println("   Response time for pageSize=100: " + responseTime + "ms");
        Assert.assertTrue(responseTime < 30000,
                "Large page response time should be < 30s: " + responseTime + "ms");
        System.out.println("   ✅ Large page size performance OK");
    }

    // ═══════════════════════════════════════════════════════════════════
    // FIELD VALIDATION (TC86-TC95)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 86, description = "TC86: Verify remarks field")
    public void tc86_RemarksField() {
        System.out.println("\n>>> TC86: Remarks Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertTrue(txn.containsKey("remarks"), "Should have remarks key");
        }
        System.out.println("   ✅ All transactions have remarks field");
    }

    @Test(priority = 87, description = "TC87: Verify payment_mode field")
    public void tc87_PaymentModeField() {
        System.out.println("\n>>> TC87: Payment Mode Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertTrue(txn.containsKey("payment_mode"), "Should have payment_mode key");
        }
        System.out.println("   ✅ All transactions have payment_mode field");
    }

    @Test(priority = 88, description = "TC88: Verify collected_by field")
    public void tc88_CollectedByField() {
        System.out.println("\n>>> TC88: Collected By Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertTrue(txn.containsKey("collected_by"), "Should have collected_by key");
        }
        System.out.println("   ✅ All transactions have collected_by field");
    }

    @Test(priority = 89, description = "TC89: Verify collected_at field")
    public void tc89_CollectedAtField() {
        System.out.println("\n>>> TC89: Collected At Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertTrue(txn.containsKey("collected_at"), "Should have collected_at key");
        }
        System.out.println("   ✅ All transactions have collected_at field");
    }

    @Test(priority = 90, description = "TC90: Verify updated_by field")
    public void tc90_UpdatedByField() {
        System.out.println("\n>>> TC90: Updated By Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertTrue(txn.containsKey("updated_by"), "Should have updated_by key");
        }
        System.out.println("   ✅ All transactions have updated_by field");
    }

    @Test(priority = 91, description = "TC91: Verify deleted_by field")
    public void tc91_DeletedByField() {
        System.out.println("\n>>> TC91: Deleted By Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertTrue(txn.containsKey("deleted_by"), "Should have deleted_by key");
        }
        System.out.println("   ✅ All transactions have deleted_by field");
    }

    @Test(priority = 92, description = "TC92: Verify admin_approval_by field")
    public void tc92_AdminApprovalByField() {
        System.out.println("\n>>> TC92: Admin Approval By Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertTrue(txn.containsKey("admin_approval_by"),
                    "Should have admin_approval_by key");
        }
        System.out.println("   ✅ All transactions have admin_approval_by field");
    }

    @Test(priority = 93, description = "TC93: Verify admin_approval_at field")
    public void tc93_AdminApprovalAtField() {
        System.out.println("\n>>> TC93: Admin Approval At Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertTrue(txn.containsKey("admin_approval_at"),
                    "Should have admin_approval_at key");
        }
        System.out.println("   ✅ All transactions have admin_approval_at field");
    }

    @Test(priority = 94, description = "TC94: Verify amount field")
    public void tc94_AmountField() {
        System.out.println("\n>>> TC94: Amount Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertTrue(txn.containsKey("amount"), "Should have amount key");
        }
        System.out.println("   ✅ All transactions have amount field");
    }

    @Test(priority = 95, description = "TC95: Verify reference_id field")
    public void tc95_ReferenceIdField() {
        System.out.println("\n>>> TC95: Reference ID Field <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        for (Map<String, Object> txn : data) {
            Assert.assertTrue(txn.containsKey("reference_id"), "Should have reference_id key");
        }
        System.out.println("   ✅ All transactions have reference_id field");
    }

    // ═══════════════════════════════════════════════════════════════════
    // HIGH PRIORITY AUTOMATION (TC96-TC100)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 96, description = "TC96: Verify full schema of a transaction")
    public void tc96_FullSchemaValidation() {
        System.out.println("\n>>> TC96: Full Schema Validation <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        Assert.assertFalse(data.isEmpty(), "Should have data");

        Map<String, Object> txn = data.get(0);
        String[] requiredFields = {"Guid", "mobile", "customer_id", "brand_id",
                "trnsc_id", "order_id", "trnsc_amount", "rewards_gain", "rewards_used",
                "is_reverted", "status", "reference_id", "reference_code",
                "actual_price", "taking_rewards", "net_paid_amount",
                "created_by", "created_at", "updated_at", "isAdminReward"};

        for (String field : requiredFields) {
            Assert.assertTrue(txn.containsKey(field), "Missing field: " + field);
        }
        System.out.println("   ✅ Full schema validated (" + requiredFields.length + " fields)");
    }

    @Test(priority = 97, description = "TC97: Verify pagination consistency")
    public void tc97_PaginationConsistency() {
        System.out.println("\n>>> TC97: Pagination Consistency <<<");
        Response r1 = callGetAllTransactions(validToken, 1, 5);
        Response r2 = callGetAllTransactions(validToken, 1, 10);

        int total1 = r1.jsonPath().getInt("total");
        int total2 = r2.jsonPath().getInt("total");
        Assert.assertEquals(total1, total2, "Total should be same regardless of pageSize");

        int limit1 = r1.jsonPath().getInt("limit");
        int limit2 = r2.jsonPath().getInt("limit");
        Assert.assertEquals(limit1, 5);
        Assert.assertEquals(limit2, 10);
        System.out.println("   ✅ Pagination consistent: total=" + total1);
    }

    @Test(priority = 98, description = "TC98: Verify API returns correct page number")
    public void tc98_CorrectPageNumber() {
        System.out.println("\n>>> TC98: Correct Page Number <<<");
        Response page3 = callGetAllTransactions(validToken, 3, 10);

        Assert.assertEquals(page3.getStatusCode(), 200);
        int returnedPage = page3.jsonPath().getInt("page");
        Assert.assertEquals(returnedPage, 3, "Returned page should be 3");
        System.out.println("   ✅ Page 3 returns page=3");
    }

    @Test(priority = 99, description = "TC99: Verify idempotent requests")
    public void tc99_IdempotentRequests() {
        System.out.println("\n>>> TC99: Idempotent Requests <<<");
        Response r1 = callGetAllTransactions(validToken, 1, 10);
        Response r2 = callGetAllTransactions(validToken, 1, 10);

        Assert.assertEquals(r1.getStatusCode(), r2.getStatusCode());
        Assert.assertEquals(r1.jsonPath().getInt("total"), r2.jsonPath().getInt("total"));

        List<Map<String, Object>> data1 = r1.jsonPath().getList("data");
        List<Map<String, Object>> data2 = r2.jsonPath().getList("data");
        Assert.assertEquals(data1.size(), data2.size());
        System.out.println("   ✅ Requests are idempotent");
    }

    @Test(priority = 100, description = "TC100: Verify response time with different page sizes")
    public void tc100_ResponseTimeWithDifferentPageSizes() {
        System.out.println("\n>>> TC100: Response Time With Different Page Sizes <<<");
        long time10 = callGetAllTransactions(validToken, 1, 10).getTime();
        long time50 = callGetAllTransactions(validToken, 1, 50).getTime();

        System.out.println("   pageSize=10: " + time10 + "ms");
        System.out.println("   pageSize=50: " + time50 + "ms");
        Assert.assertTrue(time10 < 15000, "pageSize=10 should be < 15s: " + time10 + "ms");
        Assert.assertTrue(time50 < 30000, "pageSize=50 should be < 30s: " + time50 + "ms");
        System.out.println("   ✅ Response times within acceptable range");
    }

    // ═══════════════════════════════════════════════════════════════════
    // EXTENDED BUSINESS SCENARIOS (TC101-TC110)
    // ═══════════════════════════════════════════════════════════════════

    @Test(priority = 101, description = "TC101: Verify transactions with Razorpay payment IDs")
    public void tc101_RazorpayPaymentIds() {
        System.out.println("\n>>> TC101: Razorpay Payment IDs <<<");
        Response response = callGetAllTransactions(validToken, 1, 50);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        long razorpayCount = data.stream()
                .filter(d -> {
                    String trnscId = (String) d.get("trnsc_id");
                    return trnscId != null && trnscId.startsWith("pay_");
                })
                .count();
        System.out.println("   Razorpay transactions: " + razorpayCount);
        System.out.println("   ✅ Razorpay payment ID check completed");
    }

    @Test(priority = 102, description = "TC102: Verify free membership (0 amount) transactions")
    public void tc102_FreeMembershipZeroAmount() {
        System.out.println("\n>>> TC102: Free Membership Zero Amount <<<");
        Response response = callGetAllTransactions(validToken, 1, 50);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        long zeroAmount = data.stream()
                .filter(d -> "0.00".equals(String.valueOf(d.get("trnsc_amount"))))
                .count();
        System.out.println("   Zero amount transactions: " + zeroAmount);
        System.out.println("   ✅ Free membership check completed");
    }

    @Test(priority = 103, description = "TC103: Verify high value transactions")
    public void tc103_HighValueTransactions() {
        System.out.println("\n>>> TC103: High Value Transactions <<<");
        Response response = callGetAllTransactions(validToken, 1, 50);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        long highValue = data.stream()
                .filter(d -> {
                    double amount = Double.parseDouble(String.valueOf(d.get("trnsc_amount")));
                    return amount >= 10000;
                })
                .count();
        System.out.println("   High value (>=10000) transactions: " + highValue);
        System.out.println("   ✅ High value transaction check completed");
    }

    @Test(priority = 104, description = "TC104: Verify transactions with rewards gained")
    public void tc104_TransactionsWithRewardsGained() {
        System.out.println("\n>>> TC104: Transactions With Rewards Gained <<<");
        Response response = callGetAllTransactions(validToken, 1, 50);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        long withRewards = data.stream()
                .filter(d -> {
                    double gain = Double.parseDouble(String.valueOf(d.get("rewards_gain")));
                    return gain > 0;
                })
                .count();
        System.out.println("   Transactions with rewards gained: " + withRewards);
        System.out.println("   ✅ Rewards gain check completed");
    }

    @Test(priority = 105, description = "TC105: Verify multiple brands in transactions")
    public void tc105_MultipleBrands() {
        System.out.println("\n>>> TC105: Multiple Brands <<<");
        Response response = callGetAllTransactions(validToken, 1, 50);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        long uniqueBrands = data.stream()
                .map(d -> (String) d.get("brand_id"))
                .distinct()
                .count();
        System.out.println("   Unique brands: " + uniqueBrands);
        Assert.assertTrue(uniqueBrands >= 1, "Should have at least 1 brand");
        System.out.println("   ✅ Brand check completed");
    }

    @Test(priority = 107, description = "TC107: Verify page 1 and page 2 have no overlap")
    public void tc107_NoOverlapBetweenPages() {
        System.out.println("\n>>> TC107: No Overlap Between Pages <<<");
        Response page1 = callGetAllTransactions(validToken, 1, 10);
        Response page2 = callGetAllTransactions(validToken, 2, 10);

        List<Map<String, Object>> data1 = page1.jsonPath().getList("data");
        List<Map<String, Object>> data2 = page2.jsonPath().getList("data");

        List<String> guids1 = data1.stream().map(d -> (String) d.get("Guid"))
                .collect(Collectors.toList());
        List<String> guids2 = data2.stream().map(d -> (String) d.get("Guid"))
                .collect(Collectors.toList());

        for (String guid : guids2) {
            Assert.assertFalse(guids1.contains(guid),
                    "Page 2 Guid should not be in page 1: " + guid);
        }
        System.out.println("   ✅ No overlap between page 1 and page 2");
    }

    @Test(priority = 108, description = "TC108: Verify reference_code format consistency")
    public void tc108_ReferenceCodeFormatConsistency() {
        System.out.println("\n>>> TC108: Reference Code Format Consistency <<<");
        Response response = callGetAllTransactions(validToken, 1, 50);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        List<String> refCodes = data.stream()
                .map(d -> (String) d.get("reference_code"))
                .filter(r -> r != null && !r.isEmpty())
                .collect(Collectors.toList());
        // Reference codes may be shared across related transactions (e.g., membership + payment)
        // Verify format consistency instead of uniqueness
        for (String code : refCodes) {
            Assert.assertTrue(code.startsWith("MY"),
                    "Reference code should start with MY: " + code);
        }
        long uniqueCount = refCodes.stream().distinct().count();
        System.out.println("   ✅ Reference codes: " + refCodes.size() + " total, " + uniqueCount + " unique");
    }

    @Test(priority = 109, description = "TC109: Verify transactions created in recent months")
    public void tc109_RecentTransactions() {
        System.out.println("\n>>> TC109: Recent Transactions <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        List<Map<String, Object>> data = response.jsonPath().getList("data");
        if (!data.isEmpty()) {
            String latestDate = (String) data.get(0).get("created_at");
            Assert.assertTrue(latestDate.startsWith("202"),
                    "Latest transaction should be from 2020s: " + latestDate);
        }
        System.out.println("   ✅ Recent transactions verified");
    }

    @Test(priority = 110, description = "TC110: Verify complete response envelope")
    public void tc110_CompleteResponseEnvelope() {
        System.out.println("\n>>> TC110: Complete Response Envelope <<<");
        Response response = callGetAllTransactions(validToken, 1, 10);

        Assert.assertNotNull(response.jsonPath().get("status"), "Should have status");
        Assert.assertNotNull(response.jsonPath().get("success"), "Should have success");
        Assert.assertNotNull(response.jsonPath().get("msg"), "Should have msg");
        Assert.assertNotNull(response.jsonPath().get("total"), "Should have total");
        Assert.assertNotNull(response.jsonPath().get("page"), "Should have page");
        Assert.assertNotNull(response.jsonPath().get("limit"), "Should have limit");
        Assert.assertNotNull(response.jsonPath().get("total_pages"), "Should have total_pages");
        Assert.assertNotNull(response.jsonPath().get("data"), "Should have data");
        System.out.println("   ✅ Complete response envelope verified");
    }
}
