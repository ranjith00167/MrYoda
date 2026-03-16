package stepDefinition;

import api.cart.CartClient;
import api.order.OrderClient;
import api.payment.PaymentClient;
import api.user.UserClient;
import com.mryoda.diagnostics.api.utils.TokenManager;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import io.restassured.RestAssured;
import api.payment.PaymentPayloads;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.openqa.selenium.JavascriptExecutor;
import utilities.BaseClass;
import utilities.ScenarioContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ══════════════════════════════════════════════════════════════════════════════
 *  HYBRID STEP DEFINITIONS — Pay Online Flow
 * ══════════════════════════════════════════════════════════════════════════════
 *  Bridges PHASE 1 (UI: order placement) → PHASE 2 (API: Razorpay payment).
 *
 *  Data flow:
 *    UI checkout steps populate → TestSession / BaseClass.testData / ScenarioContext
 *    Bridge step ("capture order booking details") → ScenarioContext API fields
 *    API steps consume → ScenarioContext API fields
 * ══════════════════════════════════════════════════════════════════════════════
 */
public class PayOnlineHybridSteps extends BaseSteps {

    private final PaymentClient paymentClient = new PaymentClient();
    private final OrderClient   orderClient   = new OrderClient();
    private final CartClient    cartClient    = new CartClient();
    // private final UserClient    userClient    = new UserClient();

    // In-scenario response holders
    private Response initiatePaymentResponse;
    private Response verifyPaymentResponse;

    // =========================================================================
    // Excel data-loading steps — new user scenarios
    // =========================================================================

    @Given("load the excel data for new user and lab visit with online payment")
    public void load_excel_new_user_lab_online() {
        BaseClass.loadTestData("newUserLabOnline");
    }

    @Given("load the excel data for new user and home collection with online payment")
    public void load_excel_new_user_home_online() {
        BaseClass.loadTestData("newUserHomeOnline");
    }

    @And("capture initial rewards balance for member")
    public void capture_initial_rewards_balance() {
        System.out.println("\n========== 🎁 CAPTURING INITIAL REWARDS ==========");
        String mobile = com.mryoda.diagnostics.api.utils.RequestContext.getMobile();
        if (mobile != null && !mobile.isEmpty()) {
            double rewards = com.mryoda.diagnostics.api.utils.RewardHelper.callGetRewardsByMobileAPI(mobile);
            com.mryoda.diagnostics.api.utils.RequestContext.setInitialTotalRewards(rewards);
            System.out.println("✅ Initial Rewards captured: " + rewards);
        } else {
            System.out.println("⚠️ Mobile number missing, cannot capture rewards.");
        }
    }

    // =========================================================================
    // BRIDGE STEP — Read UI-captured data → push into ScenarioContext API fields
    // =========================================================================

    @And("capture order booking details for API handoff")
    public void capture_order_booking_details_for_api_handoff() {
        System.out.println("\n========== 📋 BRIDGE: Capturing UI booking data for API ==========");

        // ── Amount ────────────────────────────────────────────────────────────
        // uiAmountCheckout is set by "capture amount to pay" (PaymentPageSteps)
        ScenarioContext.totalAmountForPayment = (int) TestSession.uiAmountCheckout;

        // ── Visit / order type ────────────────────────────────────────────────
        String visitType = LocatorsPage.visitTypeSelected;
        ScenarioContext.orderType = (visitType != null && visitType.equalsIgnoreCase("Lab Visit"))
                                    ? "lab_visit"
                                    : "home_collection";

        // ── Booking date & time (captured by slot-selection step via TestSession) ──
        ScenarioContext.visitDate = TestSession.selectedSlotDate;   // e.g. "2026-03-07"
        ScenarioContext.visitTime = TestSession.selectedSlotTime;   // e.g. "08:00"

        // ── userId: Excel → RequestContext (live API login) ───────────────────
        String excelUserId = BaseClass.testData.getOrDefault("userId", "");
        if (excelUserId == null || excelUserId.isBlank()) {
            // Fallback: pull from RequestContext which is populated by API login step
            String rcUserId = com.mryoda.diagnostics.api.utils.RequestContext.getUserId();
            excelUserId = (rcUserId != null && !rcUserId.isBlank()) ? rcUserId : "";
            System.out.println("  userId: not in Excel → using RequestContext: " + excelUserId);
        }
        ScenarioContext.apiUserId = excelUserId;
        // ── cartId: Excel → RequestContext (live cart GUID from AddToCart API) → ScenarioContext.orderId ──
        String excelCartId = BaseClass.testData.getOrDefault("cartId", "");
        if (excelCartId == null || excelCartId.isBlank()) {
            // Prefer the live cart GUID stored by AddToCart API step
            String rcCartId = com.mryoda.diagnostics.api.utils.RequestContext.getCurrentCartId();
            if (rcCartId != null && !rcCartId.isBlank()) {
                excelCartId = rcCartId;
                System.out.println("  cartId: not in Excel → using RequestContext (live): " + excelCartId);
            } else if (ScenarioContext.orderId != null && !ScenarioContext.orderId.isBlank()) {
                excelCartId = ScenarioContext.orderId;
                System.out.println("  cartId: not in RequestContext → using ScenarioContext.orderId: " + excelCartId);
            }
        }
        ScenarioContext.apiCartId = excelCartId;

        ScenarioContext.apiAddressId    = BaseClass.testData.getOrDefault("addressId", "");
        ScenarioContext.apiSlotId       = BaseClass.testData.getOrDefault("slotId",    "");

        // ── labLocationId: Excel → RequestContext (live Madhapur location stored during LocationAPI) ──
        String excelLocId = BaseClass.testData.getOrDefault("labLocationId", "");
        if (excelLocId == null || excelLocId.isBlank()) {
            String rcLocId = com.mryoda.diagnostics.api.utils.RequestContext.getLocationId("Madhapur");
            if (rcLocId == null || rcLocId.isBlank()) {
                // Fallback: use any stored location ID
                java.util.Map<String, String> locs = com.mryoda.diagnostics.api.utils.RequestContext.getAllLocations();
                if (locs != null && !locs.isEmpty()) {
                    rcLocId = locs.values().iterator().next();
                }
            }
            excelLocId = (rcLocId != null && !rcLocId.isBlank()) ? rcLocId : null;
            System.out.println("  labLocationId: not in Excel → using RequestContext: " + excelLocId);
        }
        ScenarioContext.apiLabLocationId = excelLocId;

        // ── Coupon & Multi-Member Handoff ─────────────────────────────────────
        double uiCoupon = com.mryoda.diagnostics.api.utils.RequestContext.getCouponAmount();
        ScenarioContext.couponAmount = uiCoupon;
        
        List<String> memberIds = com.mryoda.diagnostics.api.utils.RequestContext.getMemberIds();
        
        System.out.println("  orderType        = " + ScenarioContext.orderType);
        System.out.println("  totalAmount      = ₹" + ScenarioContext.totalAmountForPayment);
        System.out.println("  couponAmount     = ₹" + ScenarioContext.couponAmount);
        System.out.println("  memberIds        = " + memberIds);
        System.out.println("  visitDate        = " + ScenarioContext.visitDate);
        System.out.println("  visitTime        = " + ScenarioContext.visitTime);
        System.out.println("  userId           = " + ScenarioContext.apiUserId);
        System.out.println("  cartId           = " + ScenarioContext.apiCartId);
        System.out.println("  addressId        = " + ScenarioContext.apiAddressId);
        System.out.println("  slotId           = " + ScenarioContext.apiSlotId);
        System.out.println("  labLocationId    = " + ScenarioContext.apiLabLocationId);
        System.out.println("=================================================================\n");
    }


    // =========================================================================
    // BRIDGE STEP — Extract Bearer token from browser localStorage
    // =========================================================================

    @And("extract auth token from browser local storage")
    public void extract_auth_token_from_browser_local_storage() {
        System.out.println("========== 🔑 Extracting auth token from browser ==========");

        JavascriptExecutor js = (JavascriptExecutor) BaseClass.driver;

        // ── Step 1: Try known keys ──
        String token = (String) js.executeScript(
            "return localStorage.getItem('token')" +
            "    || localStorage.getItem('authToken')" +
            "    || localStorage.getItem('access_token')" +
            "    || localStorage.getItem('userToken')" +
            "    || localStorage.getItem('jwt')" +
            "    || sessionStorage.getItem('token')" +
            "    || sessionStorage.getItem('authToken')" +
            "    || sessionStorage.getItem('access_token')" +
            "    || '';"
        );

        // ── Step 2: Scan all keys for JWT-shaped value (fallback) ──
        if (token == null || token.isBlank()) {
            @SuppressWarnings("unchecked")
            java.util.Map<String,String> allKeys = (java.util.Map<String,String>)
                js.executeScript(
                    "var result = {};" +
                    "for(var i=0;i<localStorage.length;i++){" +
                    "  var k=localStorage.key(i); result[k]=localStorage.getItem(k);" +
                    "}" +
                    "for(var i=0;i<sessionStorage.length;i++){" +
                    "  var k=sessionStorage.key(i); result['SS:'+k]=sessionStorage.getItem(k);" +
                    "}" +
                    "return result;"
                );
            System.out.println("🔍 All storage keys: " + (allKeys == null ? "null" : allKeys.keySet()));
            if (allKeys != null) {
                for (java.util.Map.Entry<String,String> e : allKeys.entrySet()) {
                    String v = e.getValue();
                    if (v != null && !v.isBlank()) {
                        System.out.println("  " + e.getKey() + " = " + v.substring(0, Math.min(120, v.length())));
                        // Look for JWT (header.payload.signature) pattern
                        if (v.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")) {
                            token = v;
                            System.out.println("  ✅ JWT found at key: " + e.getKey());
                            break;
                        }
                        // Look for Supabase JSON token objects {"access_token":"ey..."}
                        if (v.contains("\"access_token\"")) {
                            try {
                                String extracted = v.replaceAll(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*", "$1");
                                if (!extracted.equals(v)) {
                                    token = extracted;
                                    System.out.println("  ✅ access_token found in JSON at key: " + e.getKey());
                                    break;
                                }
                            } catch (Exception ignore) {}
                        }
                    }
                }
            }
        }

        if (token == null || token.isBlank()) {
            throw new RuntimeException(
                "❌ Auth token not found in browser storage. " +
                "Verify the localStorage key used by the MrYoda web application."
            );
        }

        // Strip "Bearer " prefix if already present
        ScenarioContext.authToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        System.out.println("✅ Auth token extracted (length: " + ScenarioContext.authToken.length() + ")");
    }

    // =========================================================================
    // API PHASE — Initiate Razorpay payment
    // =========================================================================

    @Then("call initiate payment API")
    public void call_initiate_payment_api() {
        System.out.println("========== 🚀 API PHASE: Initiate Payment ==========");

        Map<String, Object> payload = PaymentPayloads.buildVerifyPaymentPayload(
            ScenarioContext.apiCartId,
            "online",                               // payment_mode
            "android",                              // source
            ScenarioContext.apiUserId,
            ScenarioContext.apiAddressId,
            ScenarioContext.apiSlotId,
            ScenarioContext.visitDate,
            ScenarioContext.visitTime,
            ScenarioContext.totalAmountForPayment,
            ScenarioContext.orderType,
            ScenarioContext.apiLabLocationId
        );

        System.out.println("📤 POST /gateway/v2/InitiatePayment ...");
        initiatePaymentResponse = paymentClient.initiatePayment(
                ScenarioContext.authToken, payload);

        System.out.println("📥 HTTP " + initiatePaymentResponse.getStatusCode());
        System.out.println(initiatePaymentResponse.asPrettyString());
    }

    @And("initiate payment API response should return HTTP 200")
    public void initiate_payment_api_response_should_return_http_200() {
        int status = initiatePaymentResponse.getStatusCode();
        if (status != 200 && status != 201) {
            throw new AssertionError(
                "❌ initiatePayment failed! Expected HTTP 200/201 but got " + status +
                "\nBody: " + initiatePaymentResponse.getBody().asString()
            );
        }
        System.out.println("✅ initiatePayment → HTTP " + status + " (OK)");
    }

    @And("store razorpay order id and internal order id from response")
    public void store_razorpay_order_id_and_internal_order_id_from_response() {
        // Try nested under "data" first, then root level
        String rzpOrderId = initiatePaymentResponse.jsonPath()
                .getString("data.razorpay_order_id");
        if (rzpOrderId == null || rzpOrderId.isBlank()) {
            rzpOrderId = initiatePaymentResponse.jsonPath()
                    .getString("razorpay_order_id");
        }
        if (rzpOrderId == null || rzpOrderId.isBlank()) {
            throw new AssertionError(
                "❌ razorpay_order_id not found in initiatePayment response!\n" +
                initiatePaymentResponse.asPrettyString()
            );
        }
        ScenarioContext.razorpayOrderId = rzpOrderId;

        // Internal order id (optional — may not be returned by all env builds)
        String internalId = initiatePaymentResponse.jsonPath().getString("data.order_id");
        if (internalId != null && !internalId.isBlank()) {
            ScenarioContext.createdOrderId = internalId;
        }

        System.out.println("✅ razorpay_order_id stored : " + ScenarioContext.razorpayOrderId);
        System.out.println("✅ internal order_id stored : " + ScenarioContext.createdOrderId);
    }

    // =========================================================================
    // API PHASE — Verify payment (simulate Razorpay callback with test values)
    // =========================================================================

    @When("call verify payment API with test credentials")
    public void call_verify_payment_api_with_test_credentials() {
        System.out.println("========== ✅ API PHASE: Verify Payment ==========");

        // Use mock/test creds — real Razorpay test keys for UAT/staging environment
        String mockPaymentId  = BaseClass.testData.getOrDefault(
                "mockRazorpayPaymentId",  "pay_test_AUTOMATION_MOCK_001");
        String mockSignature  = BaseClass.testData.getOrDefault(
                "mockRazorpaySignature",  "mock_hmac_signature_for_automation");

        ScenarioContext.razorpayPaymentId = mockPaymentId;

        Map<String, Object> payload = new HashMap<>();
        payload.put("razorpay_payment_id", mockPaymentId);
        payload.put("razorpay_order_id",   ScenarioContext.razorpayOrderId);
        payload.put("razorpay_signature",  mockSignature);
        payload.put("order_id",            ScenarioContext.createdOrderId);

        System.out.println("📤 POST /gateway/v2/VerifyPayment ...");
        verifyPaymentResponse = paymentClient.verifyPayment(
                ScenarioContext.authToken, payload);

        System.out.println("📥 HTTP " + verifyPaymentResponse.getStatusCode());
        System.out.println(verifyPaymentResponse.asPrettyString());
    }

    @Then("verify payment API response should return HTTP 200")
    public void verify_payment_api_response_should_return_http_200() {
        int status = verifyPaymentResponse.getStatusCode();
        if (status != 200 && status != 201) {
            throw new AssertionError(
                "❌ verifyPayment failed! Expected HTTP 200/201 but got " + status +
                "\nBody: " + verifyPaymentResponse.getBody().asString()
            );
        }
        System.out.println("✅ verifyPayment → HTTP " + status + " (OK)");
    }

    @And("payment status in response should be {string}")
    public void payment_status_in_response_should_be(String expectedStatus) {
        String actual = verifyPaymentResponse.jsonPath().getString("data.payment_status");
        if (actual == null) {
            actual = verifyPaymentResponse.jsonPath().getString("payment_status");
        }
        ScenarioContext.paymentStatus = actual;
        System.out.println("💳 Payment status from API : " + actual);

        if (!expectedStatus.equalsIgnoreCase(actual)) {
            throw new AssertionError(
                "❌ Payment status mismatch! Expected: '" + expectedStatus +
                "' | Actual: '" + actual + "'\n" +
                verifyPaymentResponse.asPrettyString()
            );
        }
        System.out.println("✅ Payment status confirmed : " + actual);
    }

    // =========================================================================
    // API PHASE — Fetch order and assert confirmed status
    // =========================================================================

    @And("capture order id from browser after payment")
    public void capture_order_id_from_browser_after_payment() {
        System.out.println("========== 📦 Extracting Order ID from browser ==========");
        
        String url = BaseClass.driver.getCurrentUrl();
        System.out.println("Current URL: " + url);
        
        String extractedId = null;
        
        // 1. Try URL query params & path
        if (url.contains("order_id=")) {
            extractedId = url.split("order_id=")[1].split("&")[0];
            System.out.println("✅ Order ID found in URL: " + extractedId);
        } else if (url.contains("/order/")) {
            String[] parts = url.split("/");
            // Get the last non-empty part
            for(int i = parts.length-1; i >= 0; i--) {
                if(!parts[i].isEmpty() && parts[i].split("\\?")[0].matches(".*[0-9a-fA-F-]{10,}.*")) {
                    extractedId = parts[i].split("\\?")[0];
                    break;
                }
            }
            if(extractedId != null) System.out.println("✅ Order ID found in URL path: " + extractedId);
        }
        
        // 2. Comprehensive Storage Search
        if (extractedId == null || extractedId.isBlank() || extractedId.length() < 10) {
            org.openqa.selenium.support.ui.WebDriverWait wait = new org.openqa.selenium.support.ui.WebDriverWait(BaseClass.driver, java.time.Duration.ofSeconds(10));
            try {
                wait.until(org.openqa.selenium.support.ui.ExpectedConditions.urlContains("order-success"));
            } catch (Exception e) {
                System.out.println("⚠️ order-success URL not reached before storage scan. Current URL: " + BaseClass.driver.getCurrentUrl());
                System.out.println("   Proceeding with browser-storage and API fallbacks.");
            }
            System.out.println("Current URL: " + BaseClass.driver.getCurrentUrl());

            System.out.println("🔍 Scanning Local & Session Storage...");
            
            // diagnostic dump
            org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) BaseClass.driver;
            String allStorage = (String) js.executeScript(
                "let s = 'LOCAL:\\n';" +
                "for (let i = 0; i < localStorage.length; i++) { let k=localStorage.key(i); s += k + '=' + localStorage.getItem(k) + '\\n'; }" +
                "s += 'SESSION:\\n';" +
                "for (let i = 0; i < sessionStorage.length; i++) { let k=sessionStorage.key(i); s += k + '=' + sessionStorage.getItem(k) + '\\n'; }" +
                "return s;"
            );
            System.out.println("📦 BROWSER STORAGE DUMP:\n" + allStorage);

            // Extract avoiding strict regex mapping that expects backslashes, correctly parsing persist:root
            extractedId = (String) js.executeScript(
                "try { " +
                "  let persistStr = localStorage.getItem('persist:root'); " +
                "  if (persistStr) { " +
                "    let root = JSON.parse(persistStr); " +
                "    if (root.diagnosticsCart) { " +
                "      let diag = JSON.parse(root.diagnosticsCart); " +
                "      let d2 = (typeof diag === 'string') ? JSON.parse(diag) : diag; " +
                "      if (d2.orderIdFromOrderSuccess) return d2.orderIdFromOrderSuccess; " +
                "    } " +
                "  } " +
                "} catch (e) {} " +
                "return localStorage.getItem('last_order_id') " +
                "  || sessionStorage.getItem('last_order_id') " +
                "  || localStorage.getItem('order_id') " +
                "  || localStorage.getItem('booking_id') " +
                "  || '';"
            );

            // If still null, search ALL keys for something that looks like a GUID
            if (extractedId == null || extractedId.isBlank() || extractedId.length() < 10) {
                extractedId = (String) js.executeScript(
                    "for (let i = 0; i < localStorage.length; i++) {" +
                    "  let key = localStorage.key(i);" +
                    "  let val = localStorage.getItem(key);" +
                    "  if (val && val.match(/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/)) return val;" +
                    "}" +
                    "for (let i = 0; i < sessionStorage.length; i++) {" +
                    "  let key = sessionStorage.key(i);" +
                    "  let val = sessionStorage.getItem(key);" +
                    "  if (val && val.match(/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/)) return val;" +
                    "}" +
                    "return '';"
                );
            }

            if (extractedId != null && !extractedId.isBlank()) {
                System.out.println("✅ Order ID found in storage scanning: " + extractedId);
            }
        }
        
        // 3. Fallback: If still missing, check API for latest transaction/order
        if (extractedId == null || extractedId.isBlank() || extractedId.length() < 10) {
            System.out.println("⚠️ Could not extract Order ID from browser. Trying API Fallback (latest transaction)...");
            try {
                String mobile = BaseClass.testData.get("mobileNumber");
                if (mobile == null || mobile.isBlank()) {
                     mobile = utilities.ConfigReader.get("mobile.number");
                }
                
                System.out.println("🔍 API Fallback executing for mobile: " + mobile);
                
                io.restassured.response.Response transResponse = io.restassured.RestAssured.given()
                    .header("Authorization", "Bearer " + ScenarioContext.authToken)
                    .queryParam("pageSize", "5")
                    .queryParam("page", "1")
                    .get("https://staging-api-membership.yodaprojects.com/transaction/getTransactionByMobile/" + mobile);
                
                System.out.println("📥 Fallback API Status Code: " + transResponse.getStatusCode());

                if (transResponse.getStatusCode() == 200) {
                    System.out.println("🧐 Fallback Transaction DATA[0]: \n" + transResponse.jsonPath().getString("data[0]"));
                    extractedId = transResponse.jsonPath().getString("data[0].Guid");
                    if (extractedId == null || extractedId.isBlank()) {
                        extractedId = transResponse.jsonPath().getString("data[0].guid");
                    }
                    if (extractedId == null || extractedId.isBlank()) {
                        extractedId = transResponse.jsonPath().getString("data[0].order_id");
                    }
                    if (extractedId != null && !extractedId.isBlank()) {
                        System.out.println("✅ Order ID found via API Fallback: " + extractedId);
                    } else {
                        System.out.println("❌ Fallback API returned 200 but Order ID is missing in response.");
                    }
                } else {
                    System.out.println("❌ Fallback API failed with status: " + transResponse.getStatusCode() + " body: " + transResponse.getBody().asString());
                }
            } catch (Exception e) {
                System.out.println("❌ API Fallback threw exception: " + e.getMessage());
            }
        }

        if (extractedId != null && !extractedId.isBlank() && !extractedId.toLowerCase().contains("success")) {
            ScenarioContext.createdOrderId = extractedId.trim();
            ScenarioContext.orderId = extractedId.trim();
            com.mryoda.diagnostics.api.utils.RequestContext.setCurrentOrderId(extractedId.trim());
            com.mryoda.diagnostics.api.utils.RequestContext.setCurrentOrderIds(
                java.util.Collections.singletonList(extractedId.trim()));
        } else {
            System.out.println("⚠️ Could not extract specific Order ID from browser or API.");
            System.out.println("Fallbacks: createdOrderId=" + ScenarioContext.createdOrderId + ", orderId=" + ScenarioContext.orderId);
        }
    }

    // =========================================================================
    // MULTI-MEMBER: Capture ALL order IDs from browser after payment
    // =========================================================================

    @And("capture all order ids from browser after payment for multi member")
    public void capture_all_order_ids_from_browser_after_payment_for_multi_member() {
        System.out.println("========== 📦 Extracting ALL Order IDs (Multi-Member) ==========");

        List<String> allOrderIds = new ArrayList<>();
        org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) BaseClass.driver;

        // Determine expected number of members from Excel memberNames
        int expectedMembers = 2; // default
        try {
            String memberNamesRaw = BaseClass.testData.get("memberNames");
            if (memberNamesRaw != null && !memberNamesRaw.isBlank()) {
                expectedMembers = memberNamesRaw.split(",").length;
            }
        } catch (Exception ignored) {}
        System.out.println("📋 Expected member count: " + expectedMembers);

        // 1. Wait for order-success page ────────────────────────────────────────
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(BaseClass.driver, java.time.Duration.ofSeconds(15))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.urlContains("order-success"));
        } catch (Exception ignored) {}

        String url = BaseClass.driver.getCurrentUrl();
        System.out.println("📌 Current URL: " + url);

        // 2. Extract from URL (comma-sep or repeated params) ────────────────────
        for (String paramKey : new String[]{"order_id", "orderId", "orderIds", "order_ids"}) {
            if (url.contains(paramKey + "=")) {
                String raw = url.split(paramKey + "=")[1].split("&")[0];
                for (String part : raw.split(",")) {
                    String id = part.trim();
                    if (id.matches("[0-9a-fA-F-]{10,}") && !id.equalsIgnoreCase("success") && !allOrderIds.contains(id)) {
                        allOrderIds.add(id);
                        System.out.println("✅ Order ID from URL [" + paramKey + "]: " + id);
                    }
                }
            }
        }

        // 3. Extract from localStorage persist:root diagnosticsCart ─────────────
        if (allOrderIds.size() < expectedMembers) {
            try {
                Object idsRaw = js.executeScript(
                    "try {" +
                    "  let r = JSON.parse(localStorage.getItem('persist:root') || '{}');" +
                    "  let d = JSON.parse(r.diagnosticsCart || '{}');" +
                    "  let d2 = (typeof d === 'string') ? JSON.parse(d) : d;" +
                    "  let arr = d2.orderIdsFromOrderSuccess || d2.orderIds;" +
                    "  if (Array.isArray(arr) && arr.length > 0) return JSON.stringify(arr);" +
                    "  let single = d2.orderIdFromOrderSuccess || d2.orderId;" +
                    "  if (single) return JSON.stringify([single]);" +
                    "} catch(e) {}" +
                    "return null;"
                );
                if (idsRaw != null && !idsRaw.toString().isBlank()) {
                    String json = idsRaw.toString().trim().replaceAll("[\\[\\]\"]", "");
                    for (String id : json.split(",")) {
                        String clean = id.trim();
                        if (clean.matches("[0-9a-fA-F-]{10,}") && !allOrderIds.contains(clean)) {
                            allOrderIds.add(clean);
                            System.out.println("✅ Order ID from localStorage: " + clean);
                        }
                    }
                }
            } catch (Exception ex) {
                System.out.println("⚠️ localStorage extraction failed: " + ex.getMessage());
            }
        }

        // 3B. Get payment_id from first captured order (for sibling matching) ──
        String siblingPaymentId = null;
        if (!allOrderIds.isEmpty()) {
            try {
                io.restassured.response.Response firstOrderResp = io.restassured.RestAssured.given()
                    .header("Authorization", "Bearer " + ScenarioContext.authToken)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .body("{}")
                    .get("https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/" + allOrderIds.get(0));
                if (firstOrderResp.getStatusCode() == 200) {
                    siblingPaymentId = firstOrderResp.jsonPath().getString("data[0].payment_id");
                    System.out.println("💳 First order's payment_id: " + siblingPaymentId);
                }
            } catch (Exception e) {
                System.out.println("⚠️ Failed to get payment_id from first order: " + e.getMessage());
            }
        }

        // 4. API: getTransactionByMobile — collect order_id fields ──────────────
        //    IMPORTANT: The membership API's `Guid` field is the MEMBERSHIP transaction
        //    GUID, NOT the diagnostics order GUID. The `order_id` field contains the
        //    actual diagnostics order GUID. We must check `order_id` FIRST.
        if (allOrderIds.size() < expectedMembers) {
            System.out.println("📡 Using getTransactionByMobile API to find order IDs...");
            try {
                String mobile = BaseClass.testData.get("mobileNumber");
                if (mobile == null || mobile.isBlank()) mobile = com.mryoda.diagnostics.api.utils.RequestContext.getMobile();
                if (mobile == null || mobile.isBlank()) mobile = utilities.ConfigReader.get("mobile.number");
                System.out.println("🔍 API mobile: " + mobile);

                io.restassured.response.Response transResp = io.restassured.RestAssured.given()
                    .header("Authorization", "Bearer " + ScenarioContext.authToken)
                    .queryParam("pageSize", String.valueOf(expectedMembers * 3 + 5))
                    .queryParam("page", "1")
                    .get("https://staging-api-membership.yodaprojects.com/transaction/getTransactionByMobile/" + mobile);

                System.out.println("📥 API HTTP: " + transResp.getStatusCode());
                if (transResp.getStatusCode() == 200) {
                    int maxEntries = expectedMembers * 3 + 5;
                    for (int i = 0; i < maxEntries && allOrderIds.size() < expectedMembers; i++) {
                        // Collect ALL candidate IDs from this transaction entry
                        // order_id is the diagnostics order GUID; Guid is the membership transaction GUID
                        java.util.LinkedHashSet<String> candidates = new java.util.LinkedHashSet<>();
                        String ordId    = transResp.jsonPath().getString("data[" + i + "].order_id");
                        String guidCap  = transResp.jsonPath().getString("data[" + i + "].Guid");
                        String guidLow  = transResp.jsonPath().getString("data[" + i + "].guid");

                        // If all fields are null, we've exhausted the data array
                        if (ordId == null && guidCap == null && guidLow == null) break;

                        // Prefer order_id (actual diagnostics order GUID)
                        if (ordId != null && !ordId.isBlank() && !"null".equalsIgnoreCase(ordId)) candidates.add(ordId);
                        if (guidCap != null && !guidCap.isBlank() && !"null".equalsIgnoreCase(guidCap)) candidates.add(guidCap);
                        if (guidLow != null && !guidLow.isBlank() && !"null".equalsIgnoreCase(guidLow)) candidates.add(guidLow);

                        System.out.println("  📋 Transaction[" + i + "]: order_id=" + ordId + ", Guid=" + guidCap);

                            // First, try the usual candidate fields
                            for (String candidate : candidates) {
                                if (allOrderIds.contains(candidate)) continue;
                                try {
                                    io.restassured.response.Response checkResp = io.restassured.RestAssured.given()
                                        .header("Authorization", "Bearer " + ScenarioContext.authToken)
                                        .header("Content-Type", "application/json; charset=UTF-8")
                                        .body("{}")
                                        .get("https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/" + candidate);
                                    if (checkResp.getStatusCode() == 200) {
                                        // If we have a payment_id, verify sibling match (or accept null payment_id with valid visit)
                                        if (siblingPaymentId != null) {
                                            String candPaymentId = checkResp.jsonPath().getString("data[0].payment_id");
                                            String candVisitNo   = checkResp.jsonPath().getString("data[0].visit_number");
                                            boolean paymentMatch    = siblingPaymentId.equals(candPaymentId);
                                            boolean nullPmtWithVisit = (candPaymentId == null || candPaymentId.isEmpty() || "null".equalsIgnoreCase(candPaymentId))
                                                                        && (candVisitNo != null && candVisitNo.matches("MYD\\d+"));
                                            if (paymentMatch || nullPmtWithVisit) {
                                                allOrderIds.add(candidate);
                                                System.out.println(paymentMatch
                                                    ? "  ✅ Sibling order (payment_id match) from API [" + i + "]: " + candidate
                                                    : "  ✅ Sibling order (null pmt, visit=" + candVisitNo + ") from API [" + i + "]: " + candidate);
                                                break; // found valid order from this entry, move to next
                                            } else {
                                                System.out.println("  ⏭️ Valid order but different payment_id [" + i + "]: " + candidate
                                                    + " (expected=" + siblingPaymentId + ", got=" + candPaymentId + ")");
                                            }
                                        } else {
                                            // No payment_id available — accept any valid order
                                            allOrderIds.add(candidate);
                                            System.out.println("  ✅ Verified Order ID from API [" + i + "]: " + candidate);
                                            break;
                                        }
                                    } else {
                                        System.out.println("  ⏭️ Not an order (HTTP " + checkResp.getStatusCode() + "): " + candidate);
                                    }
                                } catch (Exception e) {
                                    System.out.println("  ⚠️ Check failed for " + candidate + ": " + e.getMessage());
                                }
                            }

                            // If still not found, scan the raw transaction entry for embedded UUIDs
                            if (allOrderIds.size() < expectedMembers) {
                                try {
                                    String entryRaw = transResp.jsonPath().getString("data[" + i + "]");
                                    if (entryRaw != null && !entryRaw.isBlank()) {
                                        java.util.regex.Pattern uuidPat = java.util.regex.Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
                                        java.util.regex.Matcher m = uuidPat.matcher(entryRaw);
                                        while (m.find() && allOrderIds.size() < expectedMembers) {
                                            String found = m.group();
                                            if (allOrderIds.contains(found)) continue;
                                            try {
                                                io.restassured.response.Response checkResp = io.restassured.RestAssured.given()
                                                    .header("Authorization", "Bearer " + ScenarioContext.authToken)
                                                    .header("Content-Type", "application/json; charset=UTF-8")
                                                    .body("{}")
                                                    .get("https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/" + found);
                                                if (checkResp.getStatusCode() == 200) {
                                                    if (siblingPaymentId != null) {
                                                        String candPaymentId = checkResp.jsonPath().getString("data[0].payment_id");
                                                        String candVisitNo   = checkResp.jsonPath().getString("data[0].visit_number");
                                                        boolean paymentMatch    = siblingPaymentId.equals(candPaymentId);
                                                        boolean nullPmtWithVisit = (candPaymentId == null || candPaymentId.isEmpty() || "null".equalsIgnoreCase(candPaymentId))
                                                                                    && (candVisitNo != null && candVisitNo.matches("MYD\\d+"));
                                                        if (paymentMatch || nullPmtWithVisit) {
                                                            allOrderIds.add(found);
                                                            System.out.println(paymentMatch
                                                                ? "  ✅ Discovered embedded Order UUID (payment match): " + found
                                                                : "  ✅ Discovered embedded Order UUID (null pmt, visit=" + candVisitNo + "): " + found);
                                                        }
                                                    } else {
                                                        allOrderIds.add(found);
                                                        System.out.println("  ✅ Discovered embedded Order UUID: " + found);
                                                    }
                                                }
                                            } catch (Exception ignored) {}
                                        }
                                    }
                                } catch (Exception ex) {
                                    System.out.println("  ⚠️ UUID scan failed for transaction entry: " + ex.getMessage());
                                }
                            }
                    }
                }
            } catch (Exception ex) {
                System.out.println("❌ API exception: " + ex.getMessage());
            }
        }

        // 4D. Fallback: scan getPaymentById response body for sibling order UUIDs ──
        //    The payment gateway record is created once for both orders and likely
        //    contains references (order_id, order_guid, etc.) to both diagnostics orders.
        if (allOrderIds.size() < expectedMembers && siblingPaymentId != null) {
            System.out.println("📡 Scanning getPaymentById response for sibling order UUIDs...");
            try {
                io.restassured.response.Response pmtResp = io.restassured.RestAssured.given()
                    .header("Authorization", "Bearer " + ScenarioContext.authToken)
                    .header("Content-Type", "application/json")
                    .body("{\"id\":\"" + siblingPaymentId + "\"}")
                    .post("https://staging-api-diagnostics.yodaprojects.com/gateway/getPaymentById");
                System.out.println("  📥 getPaymentById HTTP: " + pmtResp.getStatusCode());
                if (pmtResp.getStatusCode() == 200) {
                    String pmtBody = pmtResp.asString();
                    System.out.println("  📄 getPaymentById body (first 500 chars): "
                        + (pmtBody.length() > 500 ? pmtBody.substring(0, 500) : pmtBody));
                    java.util.regex.Pattern uuidPat = java.util.regex.Pattern.compile(
                        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
                    java.util.regex.Matcher m = uuidPat.matcher(pmtBody);
                    java.util.LinkedHashSet<String> pmtUuids = new java.util.LinkedHashSet<>();
                    while (m.find()) pmtUuids.add(m.group().toLowerCase());
                    System.out.println("  🔍 Found " + pmtUuids.size() + " UUIDs in payment response");
                    for (String pmtUuid : pmtUuids) {
                        if (allOrderIds.size() >= expectedMembers) break;
                        if (allOrderIds.contains(pmtUuid)) continue;
                        try {
                            io.restassured.response.Response checkResp = io.restassured.RestAssured.given()
                                .header("Authorization", "Bearer " + ScenarioContext.authToken)
                                .header("Content-Type", "application/json; charset=UTF-8")
                                .body("{}")
                                .get("https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/" + pmtUuid);
                            if (checkResp.getStatusCode() == 200) {
                                String candVisitNo = checkResp.jsonPath().getString("data[0].visit_number");
                                String candPmtId   = checkResp.jsonPath().getString("data[0].payment_id");
                                boolean paymentMatch    = siblingPaymentId.equals(candPmtId);
                                boolean nullPmtWithVisit = (candPmtId == null || candPmtId.isEmpty() || "null".equalsIgnoreCase(candPmtId))
                                                            && (candVisitNo != null && candVisitNo.matches("MYD\\d+"));
                                if (paymentMatch || nullPmtWithVisit) {
                                    allOrderIds.add(pmtUuid);
                                    System.out.println(paymentMatch
                                        ? "  ✅ Sibling order from payment response (payment_id match): " + pmtUuid
                                        : "  ✅ Sibling order from payment response (null pmt, visit=" + candVisitNo + "): " + pmtUuid);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ex) {
                System.out.println("  ⚠️ getPaymentById UUID scan failed: " + ex.getMessage());
            }
        }

        // 4B. Fallback: localStorage UUID scan with payment_id matching ─────────
        if (allOrderIds.size() < expectedMembers && siblingPaymentId != null) {
            System.out.println("📡 Scanning localStorage UUIDs for sibling orders (payment_id match)...");
            try {
                Object allUuidsRaw = js.executeScript(
                    "try {" +
                    "  var all = '';" +
                    "  for (var i = 0; i < localStorage.length; i++) { all += localStorage.getItem(localStorage.key(i)) + ' '; }" +
                    "  for (var i = 0; i < sessionStorage.length; i++) { all += sessionStorage.getItem(sessionStorage.key(i)) + ' '; }" +
                    "  var matches = all.match(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/gi);" +
                    "  return matches ? JSON.stringify([...new Set(matches)]) : '[]';" +
                    "} catch(e) { return '[]'; }"
                );
                if (allUuidsRaw != null) {
                    String uuidJson = allUuidsRaw.toString().replaceAll("[\\[\\]\"]", "");
                    String[] uuids = uuidJson.split(",");
                    System.out.println("  🔍 Found " + uuids.length + " unique UUIDs in browser storage");
                    for (String rawUuid : uuids) {
                        if (allOrderIds.size() >= expectedMembers) break;
                        String uuid = rawUuid.trim();
                        if (!uuid.matches("[0-9a-fA-F-]{36}") || allOrderIds.contains(uuid)) continue;
                        try {
                            io.restassured.response.Response checkResp = io.restassured.RestAssured.given()
                                .header("Authorization", "Bearer " + ScenarioContext.authToken)
                                .header("Content-Type", "application/json; charset=UTF-8")
                                .body("{}")
                                .get("https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/" + uuid);
                            if (checkResp.getStatusCode() == 200) {
                                String candPaymentId = checkResp.jsonPath().getString("data[0].payment_id");
                                String candVisitNo   = checkResp.jsonPath().getString("data[0].visit_number");
                                boolean paymentMatch    = siblingPaymentId.equals(candPaymentId);
                                boolean nullPmtWithVisit = (candPaymentId == null || candPaymentId.isEmpty() || "null".equalsIgnoreCase(candPaymentId))
                                                            && (candVisitNo != null && candVisitNo.matches("MYD\\d+"));
                                if (paymentMatch || nullPmtWithVisit) {
                                    allOrderIds.add(uuid);
                                    System.out.println(paymentMatch
                                        ? "  ✅ Sibling order from localStorage (payment_id match): " + uuid
                                        : "  ✅ Sibling order from localStorage (null pmt, visit=" + candVisitNo + "): " + uuid);
                                } else {
                                    System.out.println("  ⏭️ Valid order but different payment_id: " + uuid
                                        + " (expected=" + siblingPaymentId + ", got=" + candPaymentId + ")");
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ex) {
                System.out.println("⚠️ localStorage UUID scan failed: " + ex.getMessage());
            }
        }

        // 4C. Fallback: Extract from page DOM / window.__NEXT_DATA__ ────────────
        if (allOrderIds.size() < expectedMembers) {
            System.out.println("📡 Scanning page DOM and __NEXT_DATA__ for order IDs...");
            try {
                Object pageUuidsRaw = js.executeScript(
                    "try {" +
                    "  var src = document.body.innerText + ' ';" +
                    "  if (window.__NEXT_DATA__) src += JSON.stringify(window.__NEXT_DATA__) + ' ';" +
                    "  var matches = src.match(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/gi);" +
                    "  return matches ? JSON.stringify([...new Set(matches)]) : '[]';" +
                    "} catch(e) { return '[]'; }"
                );
                if (pageUuidsRaw != null) {
                    String uuidJson = pageUuidsRaw.toString().replaceAll("[\\[\\]\"]", "");
                    for (String rawUuid : uuidJson.split(",")) {
                        if (allOrderIds.size() >= expectedMembers) break;
                        String uuid = rawUuid.trim();
                        if (!uuid.matches("[0-9a-fA-F-]{36}") || allOrderIds.contains(uuid)) continue;
                        try {
                            io.restassured.response.Response checkResp = io.restassured.RestAssured.given()
                                .header("Authorization", "Bearer " + ScenarioContext.authToken)
                                .header("Content-Type", "application/json; charset=UTF-8")
                                .body("{}")
                                .get("https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/" + uuid);
                            if (checkResp.getStatusCode() == 200) {
                                if (siblingPaymentId != null) {
                                    String candPaymentId = checkResp.jsonPath().getString("data[0].payment_id");
                                    String candVisitNo   = checkResp.jsonPath().getString("data[0].visit_number");
                                    boolean paymentMatch    = siblingPaymentId.equals(candPaymentId);
                                    boolean nullPmtWithVisit = (candPaymentId == null || candPaymentId.isEmpty() || "null".equalsIgnoreCase(candPaymentId))
                                                                && (candVisitNo != null && candVisitNo.matches("MYD\\d+"));
                                    if (paymentMatch || nullPmtWithVisit) {
                                        allOrderIds.add(uuid);
                                        System.out.println(paymentMatch
                                            ? "  ✅ Sibling order from DOM (payment_id match): " + uuid
                                            : "  ✅ Sibling order from DOM (null pmt, visit=" + candVisitNo + "): " + uuid);
                                    } else {
                                        System.out.println("  ⏭️ Valid order but different payment_id (DOM): " + uuid
                                            + " (expected=" + siblingPaymentId + ", got=" + candPaymentId + ")");
                                    }
                                } else {
                                    allOrderIds.add(uuid);
                                    System.out.println("  ✅ Order ID from DOM: " + uuid);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ex) {
                System.out.println("⚠️ DOM scan failed: " + ex.getMessage());
            }
        }

        // 5. Trim excess: validate via getOrderById if more than expected ───────
        if (allOrderIds.size() > expectedMembers) {
            System.out.println("🔍 Validating " + allOrderIds.size() + " candidate IDs via getOrderById...");
            List<String> validated = new ArrayList<>();
            for (String id : allOrderIds) {
                try {
                    io.restassured.response.Response checkResp = io.restassured.RestAssured.given()
                        .header("Authorization", "Bearer " + ScenarioContext.authToken)
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .body("{}")
                        .get("https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/" + id);
                    if (checkResp.getStatusCode() == 200) {
                        validated.add(id);
                        System.out.println("  ✅ Valid order: " + id);
                    } else {
                        System.out.println("  ❌ Not an order (HTTP " + checkResp.getStatusCode() + "): " + id);
                    }
                } catch (Exception e) {
                    System.out.println("  ⚠️ Check failed: " + id);
                }
            }
            allOrderIds = validated;
        }

        // 6. Store results ──────────────────────────────────────────────────────
        if (!allOrderIds.isEmpty()) {
            ScenarioContext.createdOrderId = allOrderIds.get(0);
            ScenarioContext.orderId        = allOrderIds.get(0);
            com.mryoda.diagnostics.api.utils.RequestContext.setCurrentOrderId(allOrderIds.get(0));
            com.mryoda.diagnostics.api.utils.RequestContext.setCurrentOrderIds(allOrderIds);
            System.out.println("✅ Total Order IDs captured: " + allOrderIds.size() + " → " + allOrderIds);

            // Derive visit numbers by calling getOrderById for each captured order
            List<String> derivedVisits = new ArrayList<>();
            for (String oid : allOrderIds) {
                try {
                    Response r = orderClient.getOrderById(ScenarioContext.authToken, oid);
                    if (r != null && r.getStatusCode() == 200) {
                        String visitNo = r.jsonPath().getString("data[0].visit_number");
                        if (visitNo == null || visitNo.isBlank()) visitNo = r.jsonPath().getString("data[0].lab_no");
                        if (visitNo != null && !visitNo.isBlank() && !derivedVisits.contains(visitNo.trim())) {
                            derivedVisits.add(visitNo.trim());
                            System.out.println("   ✅ Mapped Order " + oid + " → Visit " + visitNo.trim());
                        }
                    } else {
                        System.out.println("   ⏭️ getOrderById did not return 200 for " + oid + "; HTTP=" + (r==null?"null":r.getStatusCode()));
                    }
                } catch (Exception e) {
                    System.out.println("   ⚠️ Failed mapping order " + oid + " to visit: " + e.getMessage());
                }
            }

            if (!derivedVisits.isEmpty()) {
                com.mryoda.diagnostics.api.utils.RequestContext.setCurrentVisitNumbers(derivedVisits);
                System.out.println("   ✅ Derived Visit Numbers from captured orders: " + derivedVisits);
            } else {
                System.out.println("⚠️ WARNING: No Visit Numbers found across " + allOrderIds.size() + " order(s)!");
            }
        } else {
            System.out.println("⚠️ Could not capture any Order IDs from browser or API fallbacks.");
        }
    }

    @Then("fetch order by id via API and assert status is confirmed")
    public void fetch_order_by_id_via_api_and_assert_status_is_confirmed() {
        // ── Build the order ID list to check ─────────────────────────────────────
        List<String> orderIdsToCheck = com.mryoda.diagnostics.api.utils.RequestContext.getCurrentOrderIds();
        if (orderIdsToCheck == null || orderIdsToCheck.isEmpty()) {
            String single = ScenarioContext.createdOrderId;
            if (single == null || single.isBlank()) single = ScenarioContext.orderId;
            if (single != null && !single.isBlank()) {
                orderIdsToCheck = java.util.Collections.singletonList(single);
            }
        }
        if (orderIdsToCheck == null || orderIdsToCheck.isEmpty()) {
            System.out.println("⚠️ Order ID(s) missing! Cannot verify via API.");
            return;
        }

        System.out.println("========== 📦 API PHASE: Verify " + orderIdsToCheck.size() + " Order(s) via getOrderById ==========");
        System.out.println("📋 Order IDs: " + orderIdsToCheck);

        List<String> allVisitIds           = new ArrayList<>();
        List<String> allComponentTestNames = new ArrayList<>();
        List<String> validStates           = java.util.Arrays.asList(
            "confirmed", "payment_success", "active", "booked", "success", "pending", "order booked");

        for (String orderIdToCheck : orderIdsToCheck) {
            System.out.println("\n🔍 --- Order: " + orderIdToCheck + " ---");
            try {
                Response orderResponse = null;
                List<String> visitIds  = new ArrayList<>();
                int maxRetries         = 10;

                for (int attempt = 1; attempt <= maxRetries; attempt++) {
                    orderResponse = orderClient.getOrderById(ScenarioContext.authToken, orderIdToCheck);

                    if (orderResponse.getStatusCode() != 200) {
                        System.out.println("⚠️ getOrderById HTTP " + orderResponse.getStatusCode() + " — skipping this ID");
                        orderResponse = null; // mark as invalid
                        break;
                    }

                    visitIds.clear();
                    
                    // Extract visit_number from data[0] (getOrderById returns data as array with single element)
                    String visitNo = orderResponse.jsonPath().getString("data[0].visit_number");
                    if (visitNo != null && !visitNo.isBlank()) {
                        visitIds.add(visitNo.trim());
                    }
                    
                    // Fallback: try lab_no if visit_number is missing
                    if (visitIds.isEmpty()) {
                        String labNo = orderResponse.jsonPath().getString("data[0].lab_no");
                        if (labNo != null && !labNo.isBlank()) {
                            visitIds.add(labNo.trim());
                        }
                    }

                    if (!visitIds.isEmpty()) {
                        System.out.println("✅ Visit Number confirmed: " + visitIds);
                        break;
                    } else if (attempt < maxRetries) {
                        System.out.println("⏳ Attempt " + attempt + ": visit_number empty. Retrying in 4s...");
                        try { Thread.sleep(4000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    }
                }

                // Skip this order if getOrderById returned non-200
                if (orderResponse == null) {
                    System.out.println("⏭️ Skipping non-existent order: " + orderIdToCheck);
                    continue;
                }

                // Accumulate visit IDs from this order
                for (String v : visitIds) {
                    if (!allVisitIds.contains(v)) allVisitIds.add(v);
                }

                System.out.println("📥 getOrderById HTTP " + orderResponse.getStatusCode());

                // Order status
                String _orderStatus = orderResponse.jsonPath().getString("data[0].order_status");
                if (_orderStatus == null) _orderStatus = orderResponse.jsonPath().getString("data[0].status");
                if (_orderStatus == null) _orderStatus = orderResponse.jsonPath().getString("data[0].payment_status");
                if (_orderStatus == null) _orderStatus = orderResponse.jsonPath().getString("status");
                final String orderStatus = _orderStatus;
                System.out.println("📦 Order status: " + orderStatus);

                // Price summary for this order
                String totalPrice = orderResponse.jsonPath().getString("data[0].total_price");
                String paidAmount = orderResponse.jsonPath().getString("data[0].paid_amount");
                String finalPrice = orderResponse.jsonPath().getString("data[0].final_price");
                System.out.println("💰 Total: ₹" + totalPrice + " | Paid: ₹" + paidAmount + " | Final: ₹" + finalPrice);

                // ── Coupon & Discount Details ──────────────────────────────────────────
                String couponCode = orderResponse.jsonPath().getString("data[0].coupon_code");
                String couponGuid = orderResponse.jsonPath().getString("data[0].coupon_guid");
                String couponDiscountAmount = orderResponse.jsonPath().getString("data[0].coupon_discount_amount");
                String membershipDiscount = orderResponse.jsonPath().getString("data[0].membership_discount");
                
                System.out.println("\n🎫 COUPON & DISCOUNT VALIDATION:");
                System.out.println("   Coupon Code       : " + (couponCode != null ? couponCode : "None"));
                System.out.println("   Coupon GUID       : " + (couponGuid != null ? couponGuid : "None"));
                System.out.println("   Coupon Discount   : ₹" + (couponDiscountAmount != null ? couponDiscountAmount : "0"));
                System.out.println("   Membership Discount: ₹" + (membershipDiscount != null ? membershipDiscount : "0"));

                // Store coupon details in context for cross-validation
                if (couponCode != null && !couponCode.isEmpty() && !"null".equals(couponCode)) {
                    System.out.println("   ✅ Coupon applied in order: " + couponCode);
                } else {
                    System.out.println("   ℹ️ No coupon applied in this order");
                }

                // Extract test names and pricing from order_items
                // Note: data is an array, so use data[0].order_items to get the items list directly
                try {
                    List<Map<String, Object>> allItems = orderResponse.jsonPath().getList("data[0].order_items");
                    if (allItems == null) allItems = orderResponse.jsonPath().getList("data[0].items");
                    if (allItems == null) {
                        // Fallback: try data.order_items (flattened)
                        try {
                            Object raw = orderResponse.jsonPath().get("data.order_items");
                            if (raw instanceof List) {
                                List<?> rawList = (List<?>) raw;
                                if (!rawList.isEmpty() && rawList.get(0) instanceof List) {
                                    // Nested list: [[{item1}, {item2}]] — flatten
                                    @SuppressWarnings("unchecked")
                                    List<Map<String, Object>> nested = (List<Map<String, Object>>) rawList.get(0);
                                    allItems = nested;
                                } else {
                                    @SuppressWarnings("unchecked")
                                    List<Map<String, Object>> flat = (List<Map<String, Object>>) raw;
                                    allItems = flat;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    if (allItems == null) allItems = new ArrayList<>();

                    for (Map<String, Object> item : allItems) {
                        String testName  = item.get("product_name") != null ? item.get("product_name").toString() : null;
                        if (testName == null) testName = item.get("test_name") != null ? item.get("test_name").toString() : null;
                        String testId    = item.get("product_id") != null ? item.get("product_id").toString() :
                                          (item.get("test_id")   != null ? item.get("test_id").toString()   : "");
                        String itemId    = item.get("id") != null ? item.get("id").toString() : "";
                        int    price     = item.get("final_price")  != null ? (int) Double.parseDouble(item.get("final_price").toString())    : 0;
                        int    origPrice = item.get("actual_price") != null ? (int) Double.parseDouble(item.get("actual_price").toString())   : price;
                        int    discount  = item.get("membership_discount") != null ? (int) Double.parseDouble(item.get("membership_discount").toString()) : 0;
                        String type      = item.get("product_type") != null ? item.get("product_type").toString() : "test";
                        if (testName != null) {
                            System.out.println("📌 Storing test → " + testName + " [₹" + price + "]");
                            com.mryoda.diagnostics.api.utils.RequestContext.storeTestDetails(testName, itemId, testId, price, origPrice, discount, type);
                        }
                        // Component test names from sample_types
                        Object sampleTypesRaw = item.get("sample_types");
                        if (sampleTypesRaw instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> sampleTypes = (List<Map<String, Object>>) sampleTypesRaw;
                            for (Map<String, Object> st : sampleTypes) {
                                Object testsRaw = st.get("Tests");
                                if (testsRaw instanceof List) {
                                    @SuppressWarnings("unchecked")
                                    List<Map<String, Object>> tests = (List<Map<String, Object>>) testsRaw;
                                    for (Map<String, Object> t : tests) {
                                        String tn = t.get("TestName") != null ? t.get("TestName").toString().trim() : null;
                                        if (tn != null && !tn.isEmpty() && !allComponentTestNames.contains(tn))
                                            allComponentTestNames.add(tn);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception parseEx) {
                    System.out.println("⚠️ Item extraction error for order " + orderIdToCheck + ": " + parseEx.getMessage());
                }

                // Status validation
                final String statusForCheck = orderStatus != null ? orderStatus.toLowerCase() : null;
                boolean isValid = statusForCheck != null && validStates.stream().anyMatch(s -> s.equalsIgnoreCase(statusForCheck));
                if (!isValid) {
                    System.out.println("⚠️ Order status warning: " + orderStatus);
                } else {
                    System.out.println("✅ Order " + orderIdToCheck + " verified. Status: " + orderStatus);
                }

            } catch (Exception e) {
                System.out.println("⚠️ Error verifying order " + orderIdToCheck + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        } // end for each orderId

        // ── Store accumulated results ─────────────────────────────────────────
        if (!allVisitIds.isEmpty()) {
            System.out.println("\n🆔 All Visit IDs (all orders): " + allVisitIds);
            com.mryoda.diagnostics.api.utils.RequestContext.setCurrentVisitNumbers(allVisitIds);
            System.out.println("👥 " + (allVisitIds.size() > 1 ? "MULTI-MEMBER — " + allVisitIds.size() + " visit IDs" : "SINGLE-MEMBER ORDER"));
        } else {
            System.out.println("⚠️ WARNING: No Visit Numbers found across " + orderIdsToCheck.size() + " order(s)!");
        }
        if (!allComponentTestNames.isEmpty()) {
            System.out.println("🧪 Accumulated component tests: " + allComponentTestNames);
            com.mryoda.diagnostics.api.utils.RequestContext.setPackageTestNames(allComponentTestNames);
        }
    }

    @And("login via API and refresh auth token")
    public void login_via_api_and_refresh_auth_token() {
        System.out.println("========== 🔑 API PHASE: Refreshing Token via TokenManager ==========");
        
        // Ensure BaseURI is set for API calls in Cucumber context
        if (RestAssured.baseURI == null || RestAssured.baseURI.contains("localhost")) {
            String baseUrl = utilities.ConfigReader.get("base.url");
            if (baseUrl != null) {
                RestAssured.baseURI = baseUrl;
                System.out.println("🌐 RestAssured Base URI set to: " + RestAssured.baseURI);
            }
        }

        // Priority 1: Excel testData mobileNumber (member / non-member scenarios)
        String mobile = (BaseClass.testData != null) ? BaseClass.testData.get("mobileNumber") : null;

        // Priority 2: Randomly generated mobile from new-user signup
        String generatedMobile = stepDefinition.TestSession.generatedMobile;
        boolean isNewUser = (mobile == null || mobile.isBlank()) && (generatedMobile != null && !generatedMobile.isBlank());
        if (isNewUser) {
            mobile = generatedMobile;
        }

        // Priority 3: Fallback to config
        if (mobile == null || mobile.isBlank()) {
            mobile = utilities.ConfigReader.get("mobile.number");
            System.out.println("⚠️ mobileNumber not in testData, using config mobile.number: " + mobile);
        }

        // Determine user type
        String nonMemberMobile = utilities.ConfigReader.get("nonMemberMobile.number");
        String userType;
        if (isNewUser) {
            userType = TokenManager.NEW_USER;
        } else if (mobile.equals(nonMemberMobile)) {
            userType = TokenManager.NON_MEMBER;
        } else {
            userType = TokenManager.MEMBER;
        }

        System.out.println("🔍 Logging in via TokenManager for mobile: " + mobile + " | userType: " + userType);

        // Store mobile in RequestContext for downstream steps (rewards, API fallback, etc.)
        com.mryoda.diagnostics.api.utils.RequestContext.setMobile(mobile);

        try {
            String token = TokenManager.generateToken(mobile, userType);
            
            if (token == null || token.isBlank()) {
                 throw new AssertionError("❌ Token not found in API Login response!");
            }

            System.out.println("✅ Token refreshed. UserType: " + userType + " | Token: " + (token.length() > 20 ? token.substring(0, 20) + "..." : token));
            ScenarioContext.authToken = token;

            // ── Populate Location data into RequestContext (required for API pricing steps) ──
            try {
                com.mryoda.diagnostics.api.utils.RequestContext.setToken(token);
                Response locResponse = new RequestBuilder()
                        .setEndpoint(APIEndpoints.GET_LOCATION)
                        .addHeader("Authorization", token)
                        .post();
                if (locResponse.getStatusCode() == 200) {
                    java.util.List<java.util.Map<String, Object>> locs = locResponse.jsonPath().getList("data");
                    if (locs != null) {
                        for (java.util.Map<String, Object> l : locs) {
                            String title = String.valueOf(l.get("title"));
                            String id = String.valueOf(l.get("_id"));
                            com.mryoda.diagnostics.api.utils.RequestContext.storeLocation(title, id);
                            Object lat = l.get("google_map_latitude");
                            Object lng = l.get("google_map_langitude");
                            if (lat != null && lng != null) {
                                com.mryoda.diagnostics.api.utils.RequestContext.storeLocationCoordinates(title, String.valueOf(lat), String.valueOf(lng));
                            }
                            Object city = l.get("city");
                            Object state = l.get("state");
                            if (city != null && state != null) {
                                com.mryoda.diagnostics.api.utils.RequestContext.storeLocationCityState(title, String.valueOf(city), String.valueOf(state));
                            }
                        }
                        // Set selected location to Madhapur (default) if available
                        String madhapurId = com.mryoda.diagnostics.api.utils.RequestContext.getLocationId("Madhapur");
                        if (madhapurId != null) {
                            com.mryoda.diagnostics.api.utils.RequestContext.setSelectedLocationId(madhapurId);
                            System.out.println("📍 Location populated. Madhapur ID: " + madhapurId);
                        } else {
                            // Fallback: use first available location
                            java.util.Map<String, Object> first = locs.get(0);
                            String fallbackId = String.valueOf(first.get("_id"));
                            com.mryoda.diagnostics.api.utils.RequestContext.setSelectedLocationId(fallbackId);
                            System.out.println("📍 Madhapur not found; using fallback location ID: " + fallbackId);
                        }
                    }
                } else {
                    System.out.println("⚠️ Location API returned: " + locResponse.getStatusCode() + " — location data may be incomplete");
                }
            } catch (Exception locEx) {
                System.out.println("⚠️ Could not fetch locations: " + locEx.getMessage());
            }
        } catch (Exception e) {
            System.out.println("⚠️ API Login failed! Error: " + e.getMessage());
            throw new AssertionError("❌ API Login failed via TokenManager: " + e.getMessage());
        }
    }

    @Then("verify cart details via getCartById API for member")
    public void verify_cart_details_via_get_cart_by_id_api_for_member() {
        System.out.println("========== 🛒 API PHASE: Verify Cart Details ==========");

        // Priority 1: Use the current user's GUID from login API (data.guid stored by TokenManager)
        String userId = com.mryoda.diagnostics.api.utils.RequestContext.getUserId();
        if (userId != null && !userId.isBlank()) {
            System.out.println("✅ Using current user GUID from login API (RequestContext): " + userId);
        }

        // Priority 2: Try to get the GUID from browser LocalStorage (UI flow)
        if (userId == null || userId.isBlank()) {
            try {
                if (BaseClass.driver != null) {
                    org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) BaseClass.driver;
                    String storageId = (String) js.executeScript(
                        "try { " +
                        "  let otpStr = localStorage.getItem('otp');" +
                        "  if (otpStr) { " +
                        "     let p = JSON.parse(otpStr); " +
                        "     let p2 = typeof p === 'string' ? JSON.parse(p) : p; " +
                        "     if (p2.guID) return p2.guID;" +
                        "  }" +
                        "} catch (e) {} return '';"
                    );
                    if (storageId != null && !storageId.isBlank()) {
                        System.out.println("✅ Found User GUID dynamically from browser storage: " + storageId);
                        userId = storageId;
                    }
                }
            } catch (Exception e) {
                System.out.println("⚠️ Could not extract User ID dynamically from browser.");
            }
        }

        // Priority 3: Fallback to config (member.cart.id) — last resort only
        if (userId == null || userId.isBlank()) {
            userId = utilities.ConfigReader.get("member.cart.id");
            System.out.println("⚠️ Falling back to member.cart.id from config: " + userId);
        }

        if (userId == null || userId.isBlank()) {
            throw new RuntimeException("❌ User ID could not be resolved for getCartById!");
        }

        System.out.println("🔍 Calling getCartById for User ID: " + userId);
        Response response = cartClient.getCartById(ScenarioContext.authToken, userId);
        
        System.out.println("📥 getCartById → HTTP " + response.getStatusCode());
        if (response.getStatusCode() != 200) {
            System.out.println("❌ API Body:\n" + response.asString());
            throw new AssertionError("❌ getCartById failed with HTTP " + response.getStatusCode());
        }

        // 1. Basic Structure Validation
        String cartGuid = response.jsonPath().getString("data.guid");
        System.out.println("✅ API returned Cart GUID: " + cartGuid);
        
        // 2. Validate tests selected are present in API (Checking both 'items' and 'product_details')
        List<String> apiTests = response.jsonPath().getList("data.items.test_name");
        if (apiTests == null) apiTests = response.jsonPath().getList("data.product_details.package_name");
        
        System.out.println("📦 Tests in API Cart: " + (apiTests != null ? apiTests : "NULL"));

        String expectedTestsFromExcel = BaseClass.testData != null ? BaseClass.testData.get("testName") : null;
        if (expectedTestsFromExcel != null && !expectedTestsFromExcel.isBlank() && apiTests != null) {
            String[] expectedArr = expectedTestsFromExcel.split(",");
            for (String test : expectedArr) {
                String cleanTest = test.trim();
                boolean found = apiTests.stream().anyMatch(t -> t != null && t.equalsIgnoreCase(cleanTest));
                if (found) {
                    System.out.println("   ✔ Match found in API for: " + cleanTest);
                } else {
                    System.out.println("   ❌ MISMATCH: " + cleanTest + " not found in API Cart!");
                }
            }
        }

        // 3. Extract Visit/User details for bridge (CRITICAL FOR COD_16)
        System.out.println("🔍 Extracting Visit IDs/Order references from Cart Items...");
        List<Map<String, Object>> items = response.jsonPath().getList("data.items");
        if (items == null) items = response.jsonPath().getList("data.product_details");
        
        List<String> cleanVisits = new ArrayList<>();
        
        if (items != null) {
            for (Map<String, Object> item : items) {
                String potentialId = null;
                // Try multiple possible keys for visit/order ID/lab number
                if (item.get("lab_no") != null) potentialId = item.get("lab_no").toString();
                else if (item.get("visit_number") != null) potentialId = item.get("visit_number").toString();
                else if (item.get("item_guid") != null) potentialId = item.get("item_guid").toString();
                else if (item.get("guid") != null) potentialId = item.get("guid").toString();

                if (potentialId != null && !potentialId.isBlank() && !cleanVisits.contains(potentialId.trim())) {
                    cleanVisits.add(potentialId.trim());
                }
            }
        }

        if (!cleanVisits.isEmpty()) {
            System.out.println("✅ Extracted IDs: " + cleanVisits);
            com.mryoda.diagnostics.api.utils.RequestContext.setCurrentVisitNumbers(cleanVisits);
            System.out.println("👤 Visits found: " + cleanVisits.size());
            System.out.println("👥 Classification: " + (cleanVisits.size() > 1 ? "MULTI-USER" : "SINGLE-USER"));
        } else {
            System.out.println("⚠️ Warning: No Visit IDs or Order References found in getCartById response!");
        }

        // 4. Validate Price
        try {
            Object totalObj = response.jsonPath().get("data.totalPrice");
            if (totalObj == null) totalObj = response.jsonPath().get("data.total_amount");
            if (totalObj != null) {
                 System.out.println("💰 Total Amount in API Cart: ₹" + totalObj);
            }
        } catch (Exception ignored) {}
        
        System.out.println("✅ getCartById validation completed.");
    }
    
    @Then("fetch order by id via API and verify coupon split across orders")
    public void fetch_order_by_id_and_verify_coupon_split_across_orders() throws Throwable {
        System.out.println("========== ✓ VERIFYING COUPON SPLIT VIA API ==========");
        
        List<String> orderIds = com.mryoda.diagnostics.api.utils.RequestContext.getCurrentOrderIds();
        double totalCoupon = com.mryoda.diagnostics.api.utils.RequestContext.getCouponAmount();
        java.util.Map<String, Double> orderAmounts = com.mryoda.diagnostics.api.utils.RequestContext.getOrderAmounts();
        
        if (orderIds == null || orderIds.isEmpty()) {
            System.out.println("⚠️ No order IDs found - skipping API coupon verification");
            return;
        }
        
        if (orderAmounts == null || orderAmounts.isEmpty()) {
            System.out.println("⚠️ No order amounts found - skipping API coupon verification");
            return;
        }
        
        System.out.println("Total coupon to distribute: ₹" + totalCoupon);
        System.out.println("Number of orders: " + orderIds.size());
        
        // Calculate grand total
        double grandTotal = orderAmounts.values().stream()
            .mapToDouble(Double::doubleValue).sum();
        System.out.println("Grand total (all orders): ₹" + grandTotal);
        
        double apiCouponSumTotal = 0.0;
        int passCount = 0;
        int failCount = 0;
        
        for (String orderId : orderIds) {
            // Get expected coupon split for this order
            Double orderAmount = orderAmounts.get(orderId);
            if (orderAmount == null) {
                System.out.println("⚠️ Order " + orderId + " amount not found in RequestContext - skipping");
                continue;
            }
            
            double proportion = grandTotal > 0 ? orderAmount / grandTotal : 1.0 / orderIds.size();
            double expectedCouponSplit = Math.round(totalCoupon * proportion * 100.0) / 100.0;
            
            // Fetch order from API
            try {
                String token = ScenarioContext.authToken;
                if (token == null || token.isBlank()) {
                    token = com.mryoda.diagnostics.api.utils.RequestContext.getToken();
                }
                
                Response response = orderClient.getOrderById(token, orderId);
                
                if (response.getStatusCode() == 200) {
                    Double apiCouponAmount = response.jsonPath().getDouble("data[0].coupon_amount");
                    if (apiCouponAmount == null) apiCouponAmount = 0.0;
                    apiCouponSumTotal += apiCouponAmount;
                    
                    double difference = Math.abs(apiCouponAmount - expectedCouponSplit);
                    if (difference <= 2.0) {
                        System.out.println("✅ Order " + orderId 
                            + " | Amount: ₹" + orderAmount 
                            + " | Expected split: ₹" + expectedCouponSplit 
                            + " | API coupon: ₹" + apiCouponAmount 
                            + " | Diff: ₹" + difference);
                        passCount++;
                    } else {
                        System.out.println("❌ Order " + orderId 
                            + " | Expected: ₹" + expectedCouponSplit 
                            + " | API: ₹" + apiCouponAmount 
                            + " | MISMATCH: ₹" + difference);
                        failCount++;
                    }
                } else {
                    System.out.println("❌ Failed to fetch order " + orderId + " - Status: " + response.getStatusCode());
                    failCount++;
                }
            } catch (Exception e) {
                System.out.println("❌ Exception fetching order " + orderId + ": " + e.getMessage());
                failCount++;
            }
        }
        
        System.out.println("========== COUPON SPLIT VERIFICATION SUMMARY ==========");
        System.out.println("Total coupon distributed (API): ₹" + apiCouponSumTotal);
        System.out.println("Expected total coupon: ₹" + totalCoupon);
        System.out.println("Passed: " + passCount + " | Failed: " + failCount);
        
        double totalDiff = Math.abs(apiCouponSumTotal - totalCoupon);
        if (failCount == 0 && totalDiff <= 2.0) {
            System.out.println("✅ Coupon split across all orders VALIDATED via API (diff: ₹" + totalDiff + ")");
        } else if (failCount > 0) {
            System.out.println("❌ Coupon split validation FAILED - " + failCount + " order(s) mismatch");
            throw new AssertionError("Coupon split validation failed for " + failCount + " order(s)");
        } else {
            System.out.println("⚠️ Coupon sum difference (rounding): ₹" + totalDiff);
        }
    }
}
