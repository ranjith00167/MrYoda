# Complete COD Flow — Full API Validation Reference

**Suite:** `testng_labvisit_order_Refund.xml` (non-coupon) / `testng_labvisit_order_Refund_Coupon.xml` (coupon variant)  
**Base URL (Diagnostics):** `https://staging-api-diagnostics.yodaprojects.com`  
**Base URL (Membership):** `https://staging-api-membership.yodaprojects.com`

---

## Assertion Symbol Legend

| Symbol | Meaning |
|--------|---------|
| ✅ Hard | `AssertionUtil.verifyEquals / verifyNotNull / verifyTrue` — test fails immediately if wrong |
| ⚠️ Soft | `logSoft(...)` — writes `[SOFT]` to `logs/cod_failures.log`; test continues |
| ℹ️ Info | `System.out.println(...)` — logged for debugging; no assertion |
| 🔗 Cross-API | Value compared against a field stored in `RequestContext` from a prior step |

---

## Execution Order Map

```
[01]  auth.LoginAPITest::testLoginWithOTP
[02]  auth.COD_01_LoginTest::step01_LoginAndSetup
[03]  tests_packages.LocationAPITest::testGetLocations_ForMember
[04]  tests_packages.BrandAPITest::testGetBrands_ForMember
[05]  tests_packages.GlobalSearchAPITest::testGlobalSearch_ForMember
[06]  tests_packages.CatalogVerificationAPITest::verifyDiagnosticsCatalog (+ 4 sub-tests)
[07]  cart.AddToCartAPITest::testAddToCart_ForMember
       ── COUPON VARIANT ONLY ──────────────────────────────────────────
[07-C] coupons.CouponComprehensiveTest::TC_CPN_012_ApplyValidCouponSuccessfully
[07-C] coupons.CouponComprehensiveTest::TC_CPN_021_VerifyTotalPriceSubtraction
[07-C] coupons.CouponComprehensiveTest::TC_CPN_017_ValidateRemainingPayableAfterCoupon
[07-C] coupons.CouponComprehensiveTest::TC_CPN_019_CouponPersistsInCart
[07-C] coupons.CouponComprehensiveTest::TC_CPN_018_ValidateCouponAndAdminCashSplit
       ─────────────────────────────────────────────────────────────────
[08]  cart.COD_02_GetCartTest::step02_VerifyCartAndPrice
[09]  slot.COD_03_AddLabSlotTest::step03_AddLabSlot
[10]  payment.COD_04_VerifyPaymentPreCheckTest::step04_VerifyPaymentPreCheck
[11]  order.COD_05_CrossApiValidationTest::step05_CrossApiValidation
[12]  rewards.COD_18_RewardValidationTest::step18_A_VerifyInitialRewards   (pre-payment)
[13]  payment.COD_15_ApprovePaymentTest::step15_ApprovePayment
[14]  rewards.COD_18_RewardValidationTest::step18_B_VerifyRewardsGain      (post-payment)
[15]  rewards.COD_18_RewardValidationTest::step18_C_VerifyFinalRewardsBalance
[16]  order.COD_20_CancellationRefundTest::step20_A_ProcessCashback
[17]  order.COD_20_CancellationRefundTest::step20_B_FinalUpdateOrderCancelled
[18]  order.COD_20_CancellationRefundTest::step20_C_ApproveCancelledOrder
[19]  order.COD_20_CancellationRefundTest::step20_D_VerifyCancelledOrderDetails
[20]  order.COD_20_CancellationRefundTest::step20_E_VerifyPaymentById
[21]  order.COD_20_CancellationRefundTest::step20_F_VerifyRewardsByMobile
[22]  order.COD_20_CancellationRefundTest::step20_G_VerifyTransactionByMobile
```

---

---

## [01] Member Login — `testLoginWithOTP`

**Source:** `auth/LoginAPITest.java::testLoginWithOTP`  
**What it does:** Calls `TokenManager.generateToken(mobile, MEMBER)` to obtain a JWT for the paid-member user. No direct HTTP assertion; token and identity are stored in `RequestContext`.

| Data | Stored In | Value |
|------|-----------|-------|
| Member mobile | `RequestContext.setMobile(mobile)` | Configured via `ConfigLoader.getConfig().memberMobile()` (e.g. `9003730394`) |
| Member JWT token | `RequestContext.getMemberToken()` | Bearer token from OTP-based login |
| Member user_id | `RequestContext.getMemberUserId()` | UUID of the member user |
| Member first/last name | `RequestContext.getMemberFirstName/LastName()` | From `/users/getUser` profile fetch |
| Flow name | `RequestContext.setCurrentFlowName("member_flow")` | Used by rewards validation to skip non-member |

**Assertions:**

| Check | Assertion | Rule |
|-------|-----------|------|
| Token returned | ✅ Hard (implicit) | `TokenManager.generateToken` throws on failure |
| Expected patient stored | ✅ Hard | `RequestContext.storeExpectedPatient(userId, fullName)` called immediately after |

---

---

## [02] COD Login and Setup — `step01_LoginAndSetup`

**Source:** `auth/COD_01_LoginTest.java::step01_LoginAndSetup`  
**What it does:** Picks the correct token/userId from `RequestContext` based on the `userType` XML parameter (member / non-member / new_user), then stores them in the generic `RequestContext.setToken()` / `setUserId()`. Also clears stale order, payment, and ScenarioContext state.

### 2.1 Context Cleanup (on each run)

| Cleared field | Class |
|---------------|-------|
| `RequestContext.setVisitNumber(null)` | Order tracking |
| `RequestContext.setCurrentOrderId(null)` | Payment/order |
| `RequestContext.setCurrentPaymentId(null)` | Payment |
| `ScenarioContext.extractedSinNo` | Playwright |
| `ScenarioContext.extractedEmrId` | Playwright |
| `ScenarioContext.extractedFirstName` | Playwright |
| `ScenarioContext.orderId` | Playwright |

### 2.2 Token Resolution Logic

| XML `userType` param | Token used | UserId used |
|----------------------|-----------|-------------|
| `member` | `getMemberToken()` | `getMemberUserId()` |
| `non_member` / `existing_member` | `getNonMemberToken()` | `getNonMemberUserId()` |
| `new_user` | `getNewUserToken()` → fallback `getToken()` | `getNewUserUserId()` → fallback `getUserId()` |
| null / default | `getToken()` → fallback `getMemberToken()` | `getUserId()` → fallback `getMemberUserId()` |

### 2.3 Assertions

| Check | Assertion | Rule |
|-------|-----------|------|
| Token not null | ✅ Hard | `Assert.assertNotNull(token, "Token should not be null")` |
| UserId not null | ✅ Hard | `Assert.assertNotNull(userId, "UserId should not be null")` |

**After this step:** `RequestContext.getToken()` and `RequestContext.getUserId()` are set for all subsequent steps.

---

---

## [03] Location API — `testGetLocations_ForMember`

**Source:** `tests_packages/LocationAPITest.java::testGetLocations_ForMember`  
**Endpoint:** `POST /tests/getlocations`  
**Header:** `Authorization: <memberToken>`

### 3.1 Response fields

| Field | Assertion | Rule |
|-------|-----------|------|
| HTTP status | ✅ Hard | 200 |
| `success` | ✅ Hard | `true` |
| `data` list | ✅ Hard | `data.size() > 0` (at least one location returned) |

### 3.2 Per-location fields (iterated for all locations)

| Field | Action |
|-------|--------|
| `data[i]._id` | Stored via `RequestContext.storeLocation(title, id)` |
| `data[i].title` | Used as the location key |
| `data[i].status` | Logged; only ACTIVE locations shown prominently |
| `data[i].city` / `state` | Stored via `RequestContext.storeLocationCityState(title, city, state)` |
| `data[i].google_map_latitude` / `google_map_langitude` | Stored via `RequestContext.storeLocationCoordinates(title, lat, long)` when present |

### 3.3 Critical location check

| Check | Assertion | Rule |
|-------|-----------|------|
| `DEFAULT_LOCATION` found | ✅ Hard (via `verifyTrue`) | The configured default location (e.g. `"Madhapur"`) must exist in the response |
| `DEFAULT_LOCATION` set as selected | ✅ Hard | `RequestContext.setSelectedLocation(DEFAULT_LOCATION)` called |

**After this step:** `RequestContext.getLocationId(DEFAULT_LOCATION)` is available for all downstream steps.

---

---

## [04] Brand API — `testGetBrands_ForMember`

**Source:** `tests_packages/BrandAPITest.java::testGetBrands_ForMember`  
**Endpoint:** `POST https://staging-api-membership.yodaprojects.com/brand/getAllBrands`  
**Body:** `{ "page": 1 }`  
**Header:** `Authorization: <memberToken>`

### 4.1 Response fields

| Field | Assertion | Rule |
|-------|-----------|------|
| HTTP status | ✅ Hard | 200 |
| `success` | ✅ Hard | `true` |
| `data` list | ✅ Hard | `data != null && data.size() > 0` |

### 4.2 Per-brand fields

| Field | Action |
|-------|--------|
| `data[i].title` | Brand name stored via `RequestContext.storeBrand(title, guid)` |
| `data[i].Guid` | Brand ID stored |
| `data[i].is_active` | Logged; used as filter before storing |

### 4.3 Critical brand check

| Check | Assertion | Rule |
|-------|-----------|------|
| `"Diagnostics"` brand ID resolved | ✅ Hard (with hardcoded fallback) | Must produce a non-null brand GUID (fallback: `"967a5f02-2e38-47c8-b850-c4aeee8898ed"`) |
| Set as selected brand | ℹ️ Info | `RequestContext.setSelectedBrand("Diagnostics")` |

**After this step:** `RequestContext.getBrandId("Diagnostics")` used in `GET_CART_BY_ID` query param.

---

---

## [05] Global Search API — `testGlobalSearch_ForMember`

**Source:** `tests_packages/GlobalSearchAPITest.java::testGlobalSearch_ForMember`  
**Endpoint:** `POST tests/adminTests` (via `GlobalSearchHelper`)  
**Depends on:** Steps [01] (token) + [03] (location ID)

### 5.1 Tests searched

| Test Name | Home Collection |
|-----------|----------------|
| `Bone Profile -1` | Varies |
| `RANDOM BLOOD GLUCOSE (RBS)` | Varies |
| `CLOTTING TIME` | Varies |
| `Complete Blood Count (CBC)` | Varies |
| `T4 - THYROXINE` | Varies |

> **Note:** CBC is present here for data setup only — the comment "we are NOT searching for CBC" in code is a legacy note; all 5 tests are actually searched and stored.

### 5.2 Validations

| Check | Assertion | Rule |
|-------|-----------|------|
| Location ID from context | ✅ Hard (log warning) | `RequestContext.getLocationId(DEFAULT_LOCATION)` must be non-null |
| Tests stored | ✅ Hard | `GlobalSearchHelper.extractAndStoreTests()` stores each test's `test_id`, `b2b_price`, `rewards_percentage`, `home_collection`, `diseases`, `organ` |
| Unexpected test guard | ⚠️ Soft (warn) | Logs warning if a test not in the searched list ends up in `RequestContext.getAllTests()` |
| `home_collection` analysis | ℹ️ Info | Counts AVAILABLE vs NOT AVAILABLE; no hard assertion on counts |

**After this step:** `RequestContext.getAllTests()` contains test metadata keyed by test name, used in step [08] cart validation.

---

---

## [06] Catalog Verification — `verifyDiagnosticsCatalog` (+ sub-tests)

**Source:** `tests_packages/CatalogVerificationAPITest.java`  
**Depends on:** Step [03] (location ID) + Step [05] (stored test names)

Five independent `@Test` methods run in this class:

| Priority | Test method | Endpoint | Catalog type |
|----------|------------|----------|-------------|
| 1 | `verifyDiagnosticsCatalog` | `POST /tests/getAllTests` | Diagnostics tests |
| 2 | `verifyDnaDecoderCatalog` | `POST /tests/getAllTests` | DNA Decoder (`dnadecoder: true`) |
| 3 | `verifyPgxCatalog` | `POST /tests/getAllTests` | Pharmacogenomics (`pharmacogenomics: true`) |
| 4 | `verifyFetalMedicineCatalog` | `POST /tests/getFetalMedicineTests` | Fetal Medicine |
| 5 | `verifyPackagesCatalog` | `POST /tests/getAllPackages` | Packages |

### 6.1 Per-catalog validations

| Check | Assertion | Rule |
|-------|-----------|------|
| HTTP status | ✅ Hard | 200 |
| Target item found in paginated results | ✅ Hard | Searches up to 5 pages; target name resolved from `RequestContext.getAllTests()` (strategy 1) or dynamically from API (strategy 2) |
| Item price > 0 | ✅ Hard | `verifyPriceAndDetails(item)` |
| Item detail fields present | ✅ Hard | Name + price fields validated |

### 6.2 Target resolution strategy

1. **Strategy 1:** First test name from `RequestContext.getAllTests()` (populated by Global Search step)  
2. **Strategy 2 (fallback):** `fetchFirstItemName()` calls the API and picks the first non-null name  
3. AssertionUtil assertion: `verifyNotNull(targetName, ...)` — fails hard if both strategies return null

---

---

## [07] Add to Cart — `testAddToCart_ForMember`

**Source:** `cart/AddToCartAPITest.java::testAddToCart_ForMember`  
**Endpoint:** `POST /carts/v2/addCart`  
**Depends on:** Steps [01]–[05] (token, userId, test IDs, location ID, brand ID)

### 7.1 Request payload summary

| Field | Source |
|-------|--------|
| `user_id` | `RequestContext.getUserId()` |
| `order_type` | `"home"` or `"lab"` (from XML param / default `"home"`) |
| `lab_location_id` | `RequestContext.getLocationId(DEFAULT_LOCATION)` |
| `product_details[]` | Built from `RequestContext.getAllTests()` — each test mapped to `{product_id, quantity, ...}` |
| `coupon_guid` | Resolved if coupon flow enabled for this user type; else absent/null |

### 7.2 Response validations

| Field | Assertion | Rule |
|-------|-----------|------|
| HTTP status | ✅ Hard | 200 |
| `success` | ✅ Hard | `true` |
| `data.guid` (cart ID) | ✅ Hard | Not null; stored to `RequestContext.setCurrentCartId()` |
| `data.totalPrice` | ✅ Hard | `> 0`; stored to `RequestContext.setCurrentTotalPrice()` |
| `data.totalPrice < 2500` | ✅ Hard | COD limit enforcement; test fails if exceeded |
| `data.order_type` | ✅ Hard | Not null |
| Coupon applied (coupon flow) | ✅ Hard | `coupon_guid` non-null in response when coupon flow is enabled |

### 7.3 Price calculations (coupon flow)

```
subtotal = sum(unitPrice × quantity per product)
coupon_amount = min(subtotal × discount_pct/100, max_redeemable_amount)  [for percentage type]
             = flat discount value                                         [for flat type]
payable = subtotal − coupon_amount
```

**After this step:** `RequestContext.getCurrentCartId()`, `getCurrentTotalPrice()`, `getCouponAmount()` set.

---

---

## [07-C] Coupon Flow (COUPON SUITES ONLY)

**Source:** `coupons/CouponComprehensiveTest.java`  
**Runs:** Only in `testng_labvisit_order_Refund_Coupon.xml` and similar coupon-enabled suites.

| Test ID | What it validates |
|---------|------------------|
| `TC_CPN_012` | Apply a valid coupon to cart → HTTP 200; coupon applied in response |
| `TC_CPN_021` | Verify total price after coupon subtraction; `totalPrice − coupon_amount = payable` |
| `TC_CPN_017` | Validate remaining payable amount after coupon is applied; `payable_amount > 0` |
| `TC_CPN_019` | Reload cart; verify coupon persists in `coupon_guid` and `coupon_amount` fields |
| `TC_CPN_018` | Admin-vs-cash split: coupon discount is subtracted from user's payable; admin bears nothing from coupon |

**After this block:** `RequestContext.getCouponAmount()` contains the validated discount; membership `customer_id` not yet set (done in [21]).

---

---

## [08] Get Cart / Verify Price — `step02_VerifyCartAndPrice`

**Source:** `cart/COD_02_GetCartTest.java::step02_VerifyCartAndPrice`  
**Endpoint:** `GET /carts/v2/getCartById/{user_id}?order_type=&location=&brand=`  
**Depends on:** Steps [01] (token/userId) + [07] (cart must exist)

### 8.1 Response fields

| Field | Assertion | Rule |
|-------|-----------|------|
| HTTP status | ✅ Hard | 200 (or fallback path used if 4xx) |
| `data.guid` (cart ID) | ✅ Hard | Not null; stored to `RequestContext.setCurrentCartId()` |
| `data.totalPrice` | ✅ Hard | Parsed and stored to `RequestContext.setCurrentTotalPrice()` |
| `data.order_type` | ℹ️ Info | Logged for information; overrides XML param if present |
| `data.coupon_amount` | ℹ️ Info | Stored to `RequestContext.setCouponAmount(discount)` |
| `data.due_amount` / `payable_amount` | ℹ️ Info | Stored to `RequestContext.setCurrentDueAmount()` |
| `data.totalPrice < 2500` | ✅ Hard | `Assert.fail` if ≥ 2500 (COD limit enforcement) |

### 8.2 Cart item cross-validation (vs Global Search data)

For each test stored in `RequestContext.getAllStoredTests()`:

| Check | Assertion | Type |
|-------|-----------|------|
| Test found in `data.test_details` by `test_id` | ℹ️ Info (warning if missing) | Warn if not found |
| `rewards_percentage` match | ℹ️ Info | Expected vs actual from Global Search |
| `b2b_price` match (±0.01) | ℹ️ Info | Expected vs actual |
| `diseases` match | ℹ️ Info | Expected vs actual (string comparison) |
| `organ` match | ℹ️ Info | Expected vs actual |

### 8.3 Coupon (coupon flow)

| Check | Assertion | Rule |
|-------|-----------|------|
| `coupon_guid` in cart response | ✅ Hard | Must be non-null when coupon flow is expected (unless cart has already been converted to order) |
| `couponResult.valid` | ✅ Hard / ⚠️ Soft | `true` = coupon valid; `"already redeemed"` = business rule enforced (soft pass) |

**After this step:** Cart ID and total price confirmed and stored for downstream payment/order steps.

---

---

## [09] Add Lab Slot — `step03_AddLabSlot`

**Source:** `slot/COD_03_AddLabSlotTest.java::step03_AddLabSlot`  
**Endpoints used internally:**
- `GET /slot/getSlotCountByTime` — find available slots for the center
- `POST /carts/v2/addCart` (cart update) — set `order_type=lab`, `slot_guid`, `center_id`

**Depends on:** Steps [01] (token/userId) + [03] (location/center ID)

### 9.1 Slot selection

| Check | Assertion | Rule |
|-------|-----------|------|
| Available slot found | ✅ Hard | `Assert.assertNotNull(slotGuid, "Lab Slot GUID required")` |
| `slot.guid` | ✅ Hard | Must be non-null |
| `slot.date` + `slot.time` | ✅ Hard | Stored to `RequestContext.setExpectedSlotTiming(date, time)` |

### 9.2 Center resolution

| Check | Action |
|-------|--------|
| `centerId` | `RequestContext.getSelectedLocationId()` → fallback `getLocationId(DEFAULT_LOCATION)` |
| `locationName` | Reverse-lookup from `RequestContext.getAllLocations()` map |
| `RequestContext.setExpectedAddressName(locationName)` | Stored for later order validation |

### 9.3 Cart update for lab visit

`updateCartWithLabSlot(token, userId, slotGuid, centerId)` is called internally:
- Sets `order_type = "lab"`, `address_id = null`, `slot_guid = slotGuid`, `center_id = centerId`
- Validates HTTP 200 on cart update

**After this step:** `RequestContext.getCurrentSlotGuid()`, `getSlotStartDate()`, `getMemberSlotTime()` set.

---

---

## [10] Verify Payment Pre-Check — `step04_VerifyPaymentPreCheck`

**Source:** `payment/COD_04_VerifyPaymentPreCheckTest.java::step04_VerifyPaymentPreCheck`  
**Endpoint:** `POST /gateway/v2/VerifyPayment`  
**Depends on:** Steps [08] (cartId, totalPrice) + [09] (slotGuid)

### 10.1 Pre-check inputs resolved from context

| Input | Source |
|-------|--------|
| `token` | `RequestContext.getToken()` |
| `userId` | `RequestContext.getUserId()` |
| `cartId` | `RequestContext.getCurrentCartId()` |
| `addressId` | `RequestContext.getCurrentAddressId()` (null for lab orders) |
| `slotGuid` | `RequestContext.getCurrentSlotGuid()` |
| `totalPrice` | `RequestContext.getCurrentTotalPrice()` |
| `orderType` | `"lab"` when `addressId == null`, else `"home"` |
| `labLocationId` | Re-fetched from cart response (`data.lab_location_id`) |
| `date` / `time` | `RequestContext.getSlotStartDate()` / `getMemberSlotTime()` |

### 10.2 Verify Payment API response

| Field | Assertion | Rule |
|-------|-----------|------|
| HTTP status | ✅ Hard | 200 |
| `data.payment_id` | ✅ Hard | Not null; stored to `RequestContext.setCurrentPaymentId()` |
| `data.order_id` | ✅ Hard | Not null; stored to `RequestContext.setCurrentOrderId()` |

**After this step:** `RequestContext.getCurrentPaymentId()` and `getCurrentOrderId()` are the key IDs used by all remaining steps.

---

---

## [11] Cross-API Validation — `step05_CrossApiValidation`

**Source:** `order/COD_05_CrossApiValidationTest.java::step05_CrossApiValidation`  
**Endpoints called internally:**
- `POST /gateway/getPaymentById` — fetch payment record
- `GET /carts/v2/getCartById/{user_id}` — fetch current cart

**Depends on:** Step [10] (paymentId, cartId, totalPrice)

### 11.1 Inputs

| Input | Source |
|-------|--------|
| `paymentId` | `RequestContext.getCurrentPaymentId()` |
| `cartId` | `RequestContext.getCurrentCartId()` |
| `totalPrice` | `RequestContext.getCurrentTotalPrice()` |
| `addressId` | `RequestContext.getCurrentAddressId()` |
| `slotGuid` | `RequestContext.getCurrentSlotGuid()` |
| `expectedProductNames` | Extracted from `cart.product_details[].product_name` |

### 11.2 Cross-API checks performed by `performCrossAPIValidations()`

| Check | Assertion | Rule |
|-------|-----------|------|
| Payment record found | ✅ Hard | `getPaymentById` returns HTTP 200 |
| Payment amount matches cart total | ✅ Hard | `payment.amount ≈ totalPrice` (±1.0) |
| Product names in payment match cart | ✅ Hard | Each product name from cart found in payment's `order_items` |
| Cart order_type | ✅ Hard | Matches expected type (lab/home) |
| `slot_guid` in payment | 🔗 Cross-API | Must equal `RequestContext.getCurrentSlotGuid()` when present |
| `user_id` in payment | 🔗 Cross-API | Must equal `RequestContext.getUserId()` |
| `cart_id` in payment | 🔗 Cross-API | Must equal `RequestContext.getCurrentCartId()` |

---

---

## [12] Initial Rewards Capture — `step18_A_VerifyInitialRewards`

**Source:** `rewards/COD_18_RewardValidationTest.java::step18_A_VerifyInitialRewards`  
**Endpoint:** `GET /reward/getRewardsByMobile/{mobile}` (via `RewardHelper.callGetRewardsByMobileAPI`)  
**Condition:** Only executes when `RequestContext.getCurrentFlowName() == "member_flow"`.

### 12.1 What it does

| Check | Assertion | Rule |
|-------|-----------|------|
| Flow is member | ℹ️ Info (skip otherwise) | Non-member flows skip with log `"⏩ Skipping"` |
| `total_rewards` | ✅ Hard | `>= 0` |
| Stored | — | `RequestContext.setInitialTotalRewards(initialRewards)` |

**After this step:** `RequestContext.getInitialTotalRewards()` = pre-payment rewards balance baseline.

---

---

## [13] Approve Payment — `step15_ApprovePayment`

**Source:** `payment/COD_15_ApprovePaymentTest.java::step15_ApprovePayment`  
**Endpoint:** `POST /order/approvepayment`  
**Depends on:** Step [10] (orderId)

### 13.1 Request

| Field | Value |
|-------|-------|
| `order_guid` | `RequestContext.getCurrentOrderId()` |
| `Authorization` | `RequestContext.getToken()` |

### 13.2 Response validations (performed inside `callApprovePaymentAPI()`)

| Check | Assertion | Rule |
|-------|-----------|------|
| HTTP status | ✅ Hard | 200 |
| `success` | ✅ Hard | `true` |
| `data[0].rewards_gain` | ✅ Hard | Not null; `>= 0` |
| `data[0].order_status` | ✅ Hard | `"paid"` |
| `data[0].payment_status` | ✅ Hard | `"Success"` |

**After this step:** `RequestContext.getRewardsGain()` is set from `data[0].rewards_gain` — used in steps [14], [15], and [16]–[22].

---

---

## [14] Rewards Gain Validation — `step18_B_VerifyRewardsGain`

**Source:** `rewards/COD_18_RewardValidationTest.java::step18_B_VerifyRewardsGain`  
**Condition:** Only for `member_flow`. Depends on step [13].

### 14.1 Validations (via `RewardHelper.validateRewardsGain()`)

| Check | Assertion | Rule |
|-------|-----------|------|
| `actualGain > 0` | ✅ Hard | Rewards must have been earned on payment |
| `actualGain ≈ dueAmount × rewards_pct/100` | ✅ Hard | Rewards proportional to amount paid (business rule) |
| `dueAmount` | Source: `RequestContext.getCurrentDueAmount()` | Set in step [08] from cart payable |

---

---

## [15] Final Rewards Balance — `step18_C_VerifyFinalRewardsBalance`

**Source:** `rewards/COD_18_RewardValidationTest.java::step18_C_VerifyFinalRewardsBalance`  
**Endpoint:** `GET /reward/getRewardsByMobile/{mobile}`  
**Condition:** Only for `member_flow`. Depends on step [14].

### 15.1 Mathematical reward validation

```
initial   = RequestContext.getInitialTotalRewards()   (from step [12])
gain      = RequestContext.getRewardsGain()            (from step [13])
expected  = initial + gain
actual    = RewardHelper.callGetRewardsByMobileAPI(mobile)   (fresh API call)

Pass condition: |actual − expected| < 0.1
```

| Check | Assertion | Rule |
|-------|-----------|------|
| `actual ≈ expected` (±0.1) | ✅ Hard (via `System.err` log + optional throw) | Mismatch currently logged as error but does not hard-fail by default |
| `actual >= 0` | ✅ Hard | Balance cannot go negative |

**After this step:** `RequestContext.setFinalTotalRewards(finalRewards)` stored for use in step [21].

---

---

## [16] Process Cashback — `step20_A_ProcessCashback`

**Source:** `order/COD_20_CancellationRefundTest.java::step20_A_ProcessCashback`  
**Endpoint:** `POST /order/adminReturningCashback`  
**Payload:** `{ "order_guid": "<orderId>" }`

### 16.1 Top-level response fields

| Field | Assertion | Rule |
|-------|-----------|------|
| `status` | ✅ Hard | Not null; equals HTTP status code |
| `success` | ✅ Hard | Not null |
| `msg` | ✅ Hard | Not null |
| `message` | ✅ Hard (200 only) | Not null when HTTP 200 |
| `total_amount` | ℹ️ Info | Always `0` (dummy field); logged only |

### 16.2 HTTP 409 — Business Rule Response

When a phlebotomist has already been assigned:

| Field | Assertion | Rule |
|-------|-----------|------|
| `success` | ✅ Hard | Must be `false` |
| `msg` | ✅ Hard | Contains one of: `"cancel"` / `"phlebotomist"` / `"assigned"` / `"cannot"` |

_Test marks as SOFT-PASS and returns early when HTTP 409 is received._

### 16.3 HTTP 200 — `data.membershipCancelAmount` outer object

| Field | Assertion | Rule / Semantic |
|-------|-----------|-----------------|
| `paid_amount` → `memberDiscountAmt` | ✅ Hard | `>= 0`. **Membership DISCOUNT applied** (NOT cash paid). |
| `orderItemAmount` | ✅ Hard | `>= 0`. Rewards-eligible portion. |
| `actual_price` | ✅ Hard | `>= 0`. Full list price before membership discount. |
| `remaining_rewards` | ✅ Hard | Not null; any integer (can be negative). Current wallet balance. |
| `actual_taking_rewards` | ✅ Hard | Must be exactly `0`. |
| `adjustedRefundAmount` | ✅ Hard | `>= 0`. Must equal `canceled_amount.orderedByCash`. |

### 16.4 String / boolean fields

| Field | Assertion | Rule |
|-------|-----------|------|
| `reference_code` | ✅ Hard when present; ⚠️ Soft when null | Must start with `"MY"`. |
| `is_delivery_charge_added` | ✅ Hard | Not null. If `true` → `actual_price > orderedByCash`. |
| `adjustedMessage` | ✅ Hard | Must contain the numeric value of `orderedByCash`. |
| `walletUpdate` | ℹ️ Info | Expected `""` for single-member. |

### 16.5 `canceled_amount` nested object

| Field | Assertion | Rule / Semantic |
|-------|-----------|-----------------|
| `orderedByCash` | ✅ Hard | `> 0`. **Actual cash paid** by user for this sub-order. |
| `orderItemAmount` | ✅ Hard | Must equal `orderedByCash`. |
| `actual_price` | ✅ Hard | `>= 0`. |
| `reference_code` | ✅ Hard | Must equal outer `membershipCancelAmount.reference_code`. |
| `remaining_rewards` | ✅ Hard | Not null; any integer (can be negative). Historical cumulative figure. |
| `actual_taking_rewards` | ✅ Hard | `>= 0`. |

### 16.6 Consistency / cross-field checks

| Check | Assertion | Formula |
|-------|-----------|---------|
| Cash formula | ✅ Hard | `actual_price − memberDiscountAmt ≥ orderedByCash` (inequality allows coupon split) |
| Refund equals cash paid | ✅ Hard | `adjustedRefundAmount == canceled_amount.orderedByCash` |
| `actual_price ≥ memberDiscountAmt` | ✅ Hard | Price sanity |
| `outer.actual_taking_rewards == 0` | ✅ Hard | API contract |
| `canceled_amount.orderItemAmount == orderedByCash` | ✅ Hard | Cash view  consistency |
| `outer.remaining_rewards ≥ 0` | ✅ Hard | Wallet non-negative |
| `adjustedMessage` contains cash amount | ✅ Hard | Contains `String.valueOf((int)orderedByCash)` |
| `actual_price` discrepancy outer vs nested | ⚠️ Soft | Expected in multi-member orders |

### 16.7 Cross-API checks

| Check | Assertion | Source |
|-------|-----------|--------|
| `canceled_amount.orderedByCash == storedCartTotal` (single order) | ✅ Hard | `RequestContext.getCurrentTotalPrice()` from step [08] |
| `outer.remaining_rewards == rewardsGainForOrder` (when stored > 0) | ✅ Hard | `RequestContext.getRewardsGain()` from step [13] |

---

---

## [17] Update Order to Cancelled — `step20_B_FinalUpdateOrderCancelled`

**Source:** `order/COD_20_CancellationRefundTest.java::step20_B_FinalUpdateOrderCancelled`  
**Endpoint:** `POST /order/v2updateOrder`  
**Payload:** `{ "order_guid": "<guid>", "order_status": "Cancelled", "canceledBy": "<adminGuid>" }`

| Field | Assertion | Rule |
|-------|-----------|------|
| HTTP status | ✅ Hard | 200 |
| `msg` | ✅ Hard | Not null; equals `"Order updated successfully"` |
| **Cross-API GET verification** | ✅ Hard | Calls `getOrderById` after update; asserts `data[0].order_status == "Cancelled"` |

---

---

## [18] Approve Cancellation — `step20_C_ApproveCancelledOrder`

**Source:** `order/COD_20_CancellationRefundTest.java::step20_C_ApproveCancelledOrder`  
**Endpoint:** `POST /order/approveCancelldOrder`  
**Payload:** `{ "order_id": "<orderId>", "remarks": "test", "admin_approval_by": "<adminGuid>", "status": "Approve" }`

| Field | Assertion | Rule |
|-------|-----------|------|
| HTTP status | ✅ Hard | 200 |
| `success` | ✅ Hard (when present) | `true` |
| `msg` or `message` | ✅ Hard (when `success` absent) | At least one non-null |
| `error` | ✅ Hard (when present) | Empty string |

---

---

## [19] Verify Cancelled Order Details — `step20_D_VerifyCancelledOrderDetails`

**Source:** `order/COD_20_CancellationRefundTest.java::step20_D_VerifyCancelledOrderDetails`  
**Endpoint:** `GET /order/getOrderById/{guid}`

### 19.1 Top-level response

| Field | Assertion | Rule |
|-------|-----------|------|
| HTTP status | ✅ Hard | 200 |
| `status` | ✅ Hard | Equals HTTP code |
| `success` | ✅ Hard | `true` |
| `msg` | ✅ Hard | Not null |

### 19.2 Order identity (`data[0]`)

| Field | Assertion | Rule |
|-------|-----------|------|
| `guid` | ✅ Hard + 🔗 Cross-API | Must equal `RequestContext.getCurrentOrderId()` |
| `order_number` | ✅ Hard | Not null; not empty |
| `order_sample_number` | ✅ Hard | Not null; starts with `"MY"` |
| `visit_number` (when present) | ✅ Hard | Starts with `"MYD"` |
| `user_id` | 🔗 Cross-API | Must equal `RequestContext.getUserId()` |
| `cart_id` | ✅ Hard | Not null |

### 19.3 Amount fields

| Field | Assertion | Rule / Semantic |
|-------|-----------|-----------------|
| `paid_amount` (string) | ✅ Hard | `>= 0`. Cash user actually paid (post-membership, post-coupon). |
| `total_price` (string) | ✅ Hard | `>= paid_amount`. Full list price. |
| `final_price` (string) | ✅ Hard | `total_price >= final_price >= paid_amount` |
| `membership_discount` | ✅ Hard (when present) | `>= 0` |
| `actual_discount` | ✅ Hard (when present) | `>= 0`; must equal `membership_discount + coupon_discount` |
| `coupon_discount` | ✅ Hard (when present) | `>= 0` |
| `due_amount` | ✅ Hard `== 0` (single); ⚠️ Soft (multi-member) | |
| `refund_amount` (string) | ✅ Hard | Must exactly equal `paid_amount` |
| `rewards_gain` (string) | ✅ Hard | `>= 0`; 🔗 Cross-API `== RequestContext.getRewardsGain()` when stored > 0 |

**Discount consistency formula (hard-asserted when both fields present):**
```
total_price − membership_discount − coupon_discount = paid_amount
membership_discount + coupon_discount = actual_discount
```

### 19.4 Payment fields

| Field | Assertion | Rule |
|-------|-----------|------|
| `payment_id` | ✅ Hard + 🔗 Cross-API | Equals `RequestContext.getCurrentPaymentId()` |
| `payment.payment_type` | ✅ Hard | Not null (e.g. `"COD"`) |
| `payment.payment_mode` | ✅ Hard | `null` for COD; non-null for online |
| `payment.payment_status` | ✅ Hard | `"Success"` |
| `payment_statuses` | ✅ Hard | `"Success"` |

### 19.5 Slot fields

| Field | Assertion | Rule |
|-------|-----------|------|
| `slot_guid` | ✅ Hard + 🔗 Cross-API | Equals `RequestContext.getCurrentSlotGuid()` |
| `slot_start_time` | ✅ Hard | Not null |
| `slot_end_time` | ✅ Hard | Not null |

### 19.6 Cancellation status fields

| Field | Assertion | Rule |
|-------|-----------|------|
| `order_status` | ✅ Hard | `"Cancelled"` |
| `cancelled_date` | ✅ Hard | Not null; not empty |
| `cancelled_by_user_at` | ✅ Hard | Not null |
| `it_dose_order_status` | ✅ Hard | `"Cancelled"` |
| `cancel_order_remarks` | ℹ️ Info | Logged only |

### 19.7 Admin approval fields

| Field | Assertion | Rule |
|-------|-----------|------|
| `admin_approval_status` | ✅ Hard (when present); ⚠️ Soft (when null) | `"Approved"` |
| `admin_approval_by` | ✅ Hard + 🔗 Cross-API (when present); ⚠️ Soft (null/empty) | Equals `RequestContext.getAdminGuid()` |
| `admin_approval_at` | ✅ Hard (when present); ⚠️ Soft (null/empty) | Non-empty timestamp |

### 19.8 `user_details` object

| Field | Assertion | Rule |
|-------|-----------|------|
| `user_details.guid` | ✅ Hard | Must equal `data[0].user_id` |
| `user_details.first_name` | ✅ Hard | Not null |
| `user_details.mobile` | ✅ Hard | Not null |

### 19.9 Per-item fields in `order_items[]`

| Field | Assertion | Rule |
|-------|-----------|------|
| `order_id` | ✅ Hard | Must equal main order guid |
| `product_name` | ✅ Hard | Not null; not empty |
| `order_item_number` | ✅ Hard | `"{order_sample_number}-{itemIndex}"` |
| `order_status` | ✅ Hard | `"Cancelled"` |
| `it_dose_order_items_status` | ⚠️ Soft | Expected `"Cancelled"` (may lag async) |
| `admin_approval_status` | ✅ Hard / ⚠️ Soft | `"Approved"` when present |
| `cancelled_at` | ✅ Hard | Not null |
| `actual_price` | ✅ Hard | `> 0` |
| `final_price` | ✅ Hard | `> 0`; `<= actual_price` |
| `membership_discount` | ✅ Hard | `>= 0` |
| `sample_types[].Tests[].TestName` | ✅ Hard | Not null; not empty per test |

**Per-item price formula:**
```
actual_price − membership_discount = final_price     (HARD ASSERT per item)
```

### 19.10 Sum assertion

```
sum(order_items[].final_price) − couponDiscThisOrder = paid_amount     (HARD ASSERT)
```

### 19.11 Multi-member sibling order checks

| Check | Assertion | Rule |
|-------|-----------|------|
| Sibling `order_status` | ✅ Hard | Must NOT be `"Cancelled"` |
| Sibling `refund_amount` | ✅ Hard (when present) | Must be `0` |

---

---

## [20] Verify Payment Record — `step20_E_VerifyPaymentById`

**Source:** `order/COD_20_CancellationRefundTest.java::step20_E_VerifyPaymentById`  
**Endpoint:** `POST /gateway/getPaymentById`  
**Payload:** `{ "id": "<paymentId>" }`  
**Retry:** Up to 3 times on HTTP 5xx.

### 20.1 Top-level

| Field | Assertion | Rule |
|-------|-----------|------|
| HTTP status | ✅ Hard | 200 |
| `success` (when present) | ✅ Hard | `true` |
| `msg` | ✅ Hard | Not null |

### 20.2 `data.payments` object

| Field | Assertion | Rule |
|-------|-----------|------|
| `guid` | ✅ Hard + 🔗 Cross-API | Equals `RequestContext.getCurrentPaymentId()` |
| `payment_type` | ✅ Hard | `"COD"` |
| `payment_status` | ✅ Hard (not null); ℹ️ Info (value) | Accepted: `Refunded / Cancelled / cancelled_refund / Approved / Paid / Success` |
| `amount` | ✅ Hard | `>= 0`. 🔗 Soft vs `getCurrentTotalPrice()` |
| `net_payable` | ✅ Hard (when present) | `>= 0` |
| `membership_discount` | ✅ Hard (when present) | `>= 0` |
| `coupon_discount` | ✅ Hard (when present) | `>= 0`. 🔗 Cross-API `≈ getCouponAmount()` (±1.0); ⚠️ Soft |
| `coupon_guid` | 🔗 Cross-API (when stored) | Equals `getMemberCouponGuid()`; ⚠️ Soft |
| `payment_mode` | ✅ Hard (non-COD); ℹ️ Info (COD) | Null for COD |

### 20.3 `data.order_items[]`

| Field | Assertion | Rule |
|-------|-----------|------|
| Array | ✅ Hard | Not null; not empty |
| `product_name` | ✅ Hard | Not null; not empty |
| `final_price` | ✅ Hard | `>= 0` |
| `quantity` | ✅ Hard | `> 0` |
| `order_id` | ✅ Hard + 🔗 Cross-API | Equals `getCurrentOrderId()` (single) or one of `getCurrentOrderIds()` (multi) |

---

---

## [21] Verify Rewards After Cancellation — `step20_F_VerifyRewardsByMobile`

**Source:** `order/COD_20_CancellationRefundTest.java::step20_F_VerifyRewardsByMobile`  
**Endpoint:** `GET /reward/getRewardsByMobile/{mobile}`  
**Purpose:** Confirm rewards were reversed on cancellation; extract `customer_id` for step [22].

### 21.1 Top-level

| Field | Assertion | Rule |
|-------|-----------|------|
| HTTP status | ✅ Hard | 200 |
| `success` (when present) | ✅ Hard | `true` |
| `msg` | ✅ Hard | Not null |

### 21.2 `data` fields

| Field | Assertion | Rule |
|-------|-----------|------|
| `total_rewards` | ✅ Hard | `>= 0`; stored as `postCancelRewards` |
| `customer_id` | ✅ Hard | Not null; stored to `RequestContext.setCurrentMembershipCustomerId()` |
| `mobile` | ✅ Hard + 🔗 Cross-API | Equals `RequestContext.getMobile()` |

### 21.3 Rewards reversal cross-checks

| Check | Assertion | Rule |
|-------|-----------|------|
| `postCancelRewards < finalRewards` | ✅ Hard (when `finalRewards > 0`) | Cancellation reverses earned rewards |
| `postCancelRewards ≈ initialRewards` | ℹ️ Info | Full reversal returns to pre-payment balance |
| `finalRewards − postCancelRewards ≈ rewardsGain` | ✅ Hard (when both > 0) | Reversal amount equals what was earned |
| `postCancelRewards ≥ 0` | ✅ Hard | Wallet cannot go negative |

> **Why `customer_id` matters:** The transaction API maps `customer_id` to the **membership system's user GUID** (e.g. `"08743efb-..."`), which differs from the diagnostics `user_id` (e.g. `"60cdff44-..."`). Stored here for cross-API validation in step [22].

---

---

## [22] Verify Transactions — `step20_G_VerifyTransactionByMobile`

**Source:** `order/COD_20_CancellationRefundTest.java::step20_G_VerifyTransactionByMobile`  
**Endpoint:** `GET /transaction/getTransactionByMobile/{mobile}?page={n}&limit=10`  
**Strategy:** Paginates all pages; collects records where `reference_code == sampleNumber`; validates one `Success` + one `Cancelled` record pair.

### 22.1 Pagination (page 1 assertions)

| Field | Assertion | Rule |
|-------|-----------|------|
| `success` | ✅ Hard | `true` |
| `msg` | ✅ Hard | Not null |
| `total_pages` | ✅ Hard | `>= 1` |
| `total` | ✅ Hard | `>= 1` |
| `limit` | ✅ Hard | Not null |

### 22.2 Transaction pair existence

| Check | Assertion | Rule |
|-------|-----------|------|
| `successTxn` found | ✅ Hard | At least one `"Success"` / `"Successful"` transaction for `reference_code == sampleNumber` |
| `cancelledTxn` found | ✅ Hard | At least one `"Cancelled"` transaction for the same `reference_code` |

### 22.3 Per-transaction identity fields (both records)

| Field | Assertion | Rule |
|-------|-----------|------|
| `Guid` (capital G; fallback: `guid`, `_id`) | ✅ Hard | Not null; not empty |
| `reference_code` | ✅ Hard + 🔗 Cross-API | Equals stored `sampleNumber` (e.g. `"MY26AAA2007"`) |
| `reference_id` | **Cancelled:** ✅ Hard (not null UUID); **Success:** ℹ️ Info | Cancelled must have UUID. Not compared to `reference_code`. |
| `order_id` | ✅ Hard | Not null |
| `mobile` | ✅ Hard + 🔗 Cross-API | Equals `RequestContext.getMobile()` |
| `customer_id` | ✅ Hard + 🔗 Cross-API | Equals membership `customer_id` from step [21] |

### 22.4 Per-transaction state fields

| Field | Assertion | Rule |
|-------|-----------|------|
| `status` | ✅ Hard | One of `"Success"` / `"Successful"` / `"Cancelled"` |
| `is_reverted` | ✅ Hard | `Cancelled` → `true`; `Success/Successful` → `false` |

### 22.5 Per-transaction amount fields (all returned as strings)

| Field | Assertion | Rule |
|-------|-----------|------|
| `trnsc_amount` | ✅ Hard | `>= 0` |
| `actual_price` | ✅ Hard | `>= 0` |
| `net_paid_amount` | ✅ Hard | `>= 0` |
| `membership_discount` | ✅ Hard | `>= 0` |
| `coupon_discount` | ✅ Hard | `>= 0`. 🔗 `≈ getCouponAmount()` (±1.0); ⚠️ Soft |
| `rewards_gain` | ✅ Hard | `>= 0` |
| `rewards_used` | ✅ Hard | `>= 0` |

### 22.6 Percentage fields

| Field | Assertion | Rule |
|-------|-----------|------|
| `membership_discount_percentage` | ✅ Hard | Not null |
| `rewards_discount_percentage` | ✅ Hard | Not null |

### 22.7 Patient + timestamp fields

| Field | Assertion | Rule |
|-------|-----------|------|
| `patient_first_name` | ✅ Hard | Not null |
| `dob` | ✅ Hard | Not null |
| `gender` | ✅ Hard | Not null |
| `created_at` | ✅ Hard | Not null |
| `updated_at` | ✅ Hard | Not null |

### 22.8 `Success` record additional checks

| Field | Assertion | Rule |
|-------|-----------|------|
| `refund_amount` (when present) | ✅ Hard | Must be `0` |
| `rewards_used` | ✅ Hard | `>= 0` |
| `net_paid_amount` | ℹ️ Info | API returns `0` by design — logged |
| `trnsc_amount` comparison | ℹ️ Info | Compared with `getCurrentTotalPrice()` (±1.0) |

### 22.9 `Cancelled` record additional checks

| Field | Assertion | Rule |
|-------|-----------|------|
| `trnsc_amount` | ℹ️ Info | Compared with `getCurrentTotalPrice()` — full refund amount |
| `coupon_discount` | ℹ️ Info | Compared with `storedCouponAmount` — mirrors Success record |
| `refund_amount` (when present) | ℹ️ Info | Compared with `trnsc_amount` — full refund expected |
| `rewards_gain` | ℹ️ Info | Compared with `getRewardsGain()` — should match original earned (reversal) |
| `net_paid_amount` | ℹ️ Info | What was refunded back; compared with `storedCartTotal` |

---

---

## Key Price Formulas (Quick Reference)

```
[Add to Cart level]
subtotal = sum(product unitPrice × qty)
coupon_amount = min(subtotal × pct/100, max_redeemable)   OR flat discount
payable = subtotal − coupon_amount

[Order level (getOrderById)]
actual_price − membership_discount − coupon_discount = paid_amount
membership_discount + coupon_discount = actual_discount
sum(item.final_price) − couponDiscThisOrder = paid_amount
refund_amount = paid_amount   (full refund on cancellation)

[Per item (order_items)]
actual_price − membership_discount = final_price

[adminReturningCashback]
actual_price − memberDiscountAmt ≥ orderedByCash   (≥ because coupon split)
adjustedRefundAmount = canceled_amount.orderedByCash

[Rewards (post-payment)]
finalBalance = initialBalance + rewardsGain   (±0.1)

[Rewards (post-cancellation)]
postCancelBalance < finalBalance
finalBalance − postCancelBalance ≈ rewardsGain
```

---

---

## RequestContext Fields — Lifecycle Map

| Field | Set in Step | Read in Step(s) |
|-------|-------------|-----------------|
| `getMemberToken()` | [01] | [03], [04], [05], [07] |
| `getToken()` (generic) | [02] | [08]–[22] |
| `getUserId()` | [02] | [08], [10], [11], [14], [19] |
| `getMobile()` | [01] | [12], [15], [21], [22] |
| `getCurrentFlowName()` | [01] | [12], [14], [15] |
| `getLocationId(DEFAULT_LOCATION)` | [03] | [04], [05], [07], [08], [09] |
| `getBrandId("Diagnostics")` | [04] | [08] |
| `getAllTests()` | [05] | [06], [07], [08] |
| `getCouponAmount()` | [07], [08] | [16], [19], [20], [22] |
| `getCurrentCartId()` | [07], [08] | [10], [11] |
| `getCurrentTotalPrice()` | [07], [08] | [10], [11], [16], [19], [22] |
| `getCurrentDueAmount()` | [08] | [14] |
| `getCurrentSlotGuid()` | [09] | [10], [11], [19] |
| `getSlotStartDate()` | [09] | [10] |
| `getMemberSlotTime()` | [09] | [10] |
| `getCurrentPaymentId()` | [10] | [11], [19], [20] |
| `getCurrentOrderId()` | [10] | [11], [13], [16], [17], [18], [19], [20] |
| `getInitialTotalRewards()` | [12] | [15], [21] |
| `getRewardsGain()` | [13] | [14], [15], [16], [19], [21], [22] |
| `getFinalTotalRewards()` | [15] | [21] |
| `getAdminGuid()` | Admin login flow | [17], [18], [19] |
| `getCurrentOrderSampleNumber()` | [16] (from step20_A) | [22] (transaction filter by `reference_code`) |
| `getCurrentMembershipCustomerId()` | [21] | [22] (`customer_id` cross-check) |
| `getMemberCouponGuid()` | [07-C] coupon flow | [20] |

---

---

## Assertion Counts Summary

| Step | Approx Hard Asserts | Approx Soft Warns | Critical verifications |
|------|---------------------|-------------------|----------------------|
| [01] Member Login | 1 (implicit) | 0 | Token generation; patient stored |
| [02] COD_01 Setup | 2 | 0 | Token + UserId not null |
| [03] Location API | 3 | 1 | `success=true`, list > 0, DEFAULT_LOCATION found |
| [04] Brand API | 3 | 0 | `success=true`, list not empty, Diagnostics brand ID |
| [05] Global Search | 2 | 0 | Location validated, tests stored |
| [06] Catalog Verify | 5 per catalog × 5 = ~25 | 0 | Item found in pages; price > 0 |
| [07] Add to Cart | 4 | 0 | success=true, cartId, totalPrice, < 2500 |
| [07-C] Coupon | 5+ | 1 | Applied, persists, math correct |
| [08] Get Cart | 3 hard + validations | 1 | CartId, totalPrice < 2500, coupon persists |
| [09] Add Lab Slot | 2 | 0 | Slot GUID not null, cart updated |
| [10] Payment Pre-Check | 3 | 0 | HTTP 200, paymentId, orderId captured |
| [11] Cross-API Validation | 6 | 0 | Payment amount ≈ cart total, products match |
| [12] Initial Rewards | 1 | 0 | total_rewards >= 0, stored |
| [13] Approve Payment | 4 | 0 | HTTP 200, success=true, rewards_gain, status=paid |
| [14] Rewards Gain | 2 | 0 | gain > 0, proportional to due_amount |
| [15] Final Rewards Balance | 2 | 0 | initial + gain = final (±0.1) |
| [16] Process Cashback | ~28 | ~5 | Cash formula, reference_code, adjustedRefundAmount |
| [17] Update to Cancelled | ~4 | 0 | msg = success, GET confirms Cancelled |
| [18] Approve Cancellation | ~3 | 0 | HTTP 200, success=true |
| [19] Verify Cancelled Order | ~50+ | ~4 | order_status, per-item formula, sum formula, payment status |
| [20] Verify Payment | ~12 | ~2 | guid cross-API, payment_type COD, amounts >= 0 |
| [21] Verify Rewards | ~7 | 0 | postCancel < finalBalance, reversal ≈ gain |
| [22] Verify Transactions | ~20+ per pair | ~1 | Success + Cancelled pair exists, all fields |

---

## Soft Warning Catalogue

All of these write a `[SOFT]` line to `logs/cod_failures.log`.

| Condition | Step | Reason |
|-----------|------|--------|
| Default location not found | [03] | Config mismatch; flow may proceed with fallback |
| Coupon `already redeemed` | [08] | Business rule: 1 use per user — expected on repeated staging runs |
| `canceled_amount.actual_price ≠ outer actual_price` | [16] | Multi-member: each level may report different sub-totals |
| `sum(sub-orders orderedByCash) ≠ storedCartTotal` | [16] | Cart total pre-discount; orderedByCash post-discount |
| `it_dose_order_items_status ≠ "Cancelled"` | [19] | Async IT-DOSE update lag in multi-member |
| `admin_approval_status / by / at` null | [19] | Not always set in COD cash-refund flows |
| `coupon_discount ≠ storedCouponAmount` (payment) | [20] | Coupon split may differ per API |
| `coupon_guid ≠ storedCouponGuid` | [20] | Environment / timing discrepancy |
| `due_amount ≠ 0` in multi-member | [19] | Primary order still active |

---

---

_Document covers the complete flow: Login → Location/Brand/Catalog discovery → Cart → Slot → Payment creation → Rewards capture → Payment approval → Rewards validation → Cancellation + Refund steps (COD_20 A–G)._

_Source files:_
- `auth/LoginAPITest.java`, `auth/COD_01_LoginTest.java`
- `tests_packages/LocationAPITest.java`, `BrandAPITest.java`, `GlobalSearchAPITest.java`, `CatalogVerificationAPITest.java`
- `cart/AddToCartAPITest.java`, `cart/COD_02_GetCartTest.java`
- `slot/COD_03_AddLabSlotTest.java`
- `payment/COD_04_VerifyPaymentPreCheckTest.java`, `payment/COD_15_ApprovePaymentTest.java`
- `order/COD_05_CrossApiValidationTest.java`
- `rewards/COD_18_RewardValidationTest.java`
- `order/COD_20_CancellationRefundTest.java`
