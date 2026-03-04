# Converts Full_COD_Flow_Validation_Documentation.md into a single Excel sheet.
# Output: c:/Users/RANJITH/MrYoda/docs/Full_COD_Flow_Validation.xlsx

import openpyxl
from openpyxl.styles import (
    PatternFill, Font, Alignment, Border, Side
)
from openpyxl.utils import get_column_letter

# ─── colour palette ─────────────────────────────────────────────────────────
HEADER_FILL    = PatternFill("solid", fgColor="1F4E79")   # dark navy
STEP_FILL      = PatternFill("solid", fgColor="2E75B6")   # medium blue
SUBSEC_FILL    = PatternFill("solid", fgColor="D6E4F0")   # very light blue
HARD_FILL      = PatternFill("solid", fgColor="E2EFDA")   # light green
SOFT_FILL      = PatternFill("solid", fgColor="FFF2CC")   # light yellow
INFO_FILL      = PatternFill("solid", fgColor="EDEDED")   # light grey
CROSS_FILL     = PatternFill("solid", fgColor="FCE4D6")   # light orange
FORMULA_FILL   = PatternFill("solid", fgColor="EBF3FB")   # pale blue
ALT_ROW_FILL   = PatternFill("solid", fgColor="F5FBFF")   # barely-there blue

THIN  = Side(style="thin",   color="AAAAAA")
MED   = Side(style="medium", color="888888")

def thin_border():
    return Border(left=THIN, right=THIN, top=THIN, bottom=THIN)

def med_border():
    return Border(left=MED, right=MED, top=MED, bottom=MED)

# ─── data rows ───────────────────────────────────────────────────────────────
# Columns: Step# | Step Name | Sub-Section | Field / Check | Assertion | Rule | API Endpoint | Source File
ROWS = [

# ── [01] Member Login ──────────────────────────────────────────────────────
("[01]","Member Login — testLoginWithOTP","Assertion / Storage","Token returned","✅ Hard","TokenManager.generateToken() throws on failure","—","auth/LoginAPITest.java"),
("[01]","Member Login — testLoginWithOTP","Assertion / Storage","Expected patient stored","✅ Hard","RequestContext.storeExpectedPatient(userId, fullName) called immediately","—","auth/LoginAPITest.java"),
("[01]","Member Login — testLoginWithOTP","Context Set","getMemberToken()","ℹ️ Info","Bearer JWT for paid member","—","auth/LoginAPITest.java"),
("[01]","Member Login — testLoginWithOTP","Context Set","getMemberUserId()","ℹ️ Info","UUID of member user","—","auth/LoginAPITest.java"),
("[01]","Member Login — testLoginWithOTP","Context Set","setCurrentFlowName('member_flow')","ℹ️ Info","Used by rewards steps to skip non-member flow","—","auth/LoginAPITest.java"),

# ── [02] COD_01 Setup ─────────────────────────────────────────────────────
("[02]","COD_01 Login & Setup — step01_LoginAndSetup","2.1 Context Cleanup","RequestContext.setVisitNumber(null)","ℹ️ Info","Cleared on each run to avoid stale state","—","auth/COD_01_LoginTest.java"),
("[02]","COD_01 Login & Setup — step01_LoginAndSetup","2.1 Context Cleanup","RequestContext.setCurrentOrderId(null)","ℹ️ Info","Cleared on each run","—","auth/COD_01_LoginTest.java"),
("[02]","COD_01 Login & Setup — step01_LoginAndSetup","2.1 Context Cleanup","RequestContext.setCurrentPaymentId(null)","ℹ️ Info","Cleared on each run","—","auth/COD_01_LoginTest.java"),
("[02]","COD_01 Login & Setup — step01_LoginAndSetup","2.1 Context Cleanup","ScenarioContext.extractedSinNo / extractedEmrId / orderId","ℹ️ Info","Playwright ScenarioContext fields cleared","—","auth/COD_01_LoginTest.java"),
("[02]","COD_01 Login & Setup — step01_LoginAndSetup","2.2 Token Resolution","userType=member  → getMemberToken() / getMemberUserId()","ℹ️ Info","Resolved from XML parameter","—","auth/COD_01_LoginTest.java"),
("[02]","COD_01 Login & Setup — step01_LoginAndSetup","2.2 Token Resolution","userType=non_member → getNonMemberToken() / getNonMemberUserId()","ℹ️ Info","Resolved from XML parameter","—","auth/COD_01_LoginTest.java"),
("[02]","COD_01 Login & Setup — step01_LoginAndSetup","2.2 Token Resolution","userType=new_user → getNewUserToken() → fallback getToken()","ℹ️ Info","Resolved from XML parameter","—","auth/COD_01_LoginTest.java"),
("[02]","COD_01 Login & Setup — step01_LoginAndSetup","2.3 Assertions","Token not null","✅ Hard","Assert.assertNotNull(token, 'Token should not be null')","—","auth/COD_01_LoginTest.java"),
("[02]","COD_01 Login & Setup — step01_LoginAndSetup","2.3 Assertions","UserId not null","✅ Hard","Assert.assertNotNull(userId, 'UserId should not be null')","—","auth/COD_01_LoginTest.java"),

# ── [03] Location API ─────────────────────────────────────────────────────
("[03]","Location API — testGetLocations_ForMember","3.1 Response","HTTP status","✅ Hard","200","POST /tests/getlocations","tests_packages/LocationAPITest.java"),
("[03]","Location API — testGetLocations_ForMember","3.1 Response","success","✅ Hard","true","POST /tests/getlocations","tests_packages/LocationAPITest.java"),
("[03]","Location API — testGetLocations_ForMember","3.1 Response","data list size","✅ Hard","data.size() > 0","POST /tests/getlocations","tests_packages/LocationAPITest.java"),
("[03]","Location API — testGetLocations_ForMember","3.2 Per-location","data[i]._id","ℹ️ Info","Stored via RequestContext.storeLocation(title, id)","POST /tests/getlocations","tests_packages/LocationAPITest.java"),
("[03]","Location API — testGetLocations_ForMember","3.2 Per-location","data[i].city / state","ℹ️ Info","Stored via RequestContext.storeLocationCityState()","POST /tests/getlocations","tests_packages/LocationAPITest.java"),
("[03]","Location API — testGetLocations_ForMember","3.2 Per-location","data[i].google_map_latitude / langitude","ℹ️ Info","Stored via RequestContext.storeLocationCoordinates() when present","POST /tests/getlocations","tests_packages/LocationAPITest.java"),
("[03]","Location API — testGetLocations_ForMember","3.3 Critical Location","DEFAULT_LOCATION found in response","✅ Hard","The configured default location (e.g. 'Madhapur') must exist","POST /tests/getlocations","tests_packages/LocationAPITest.java"),
("[03]","Location API — testGetLocations_ForMember","3.3 Critical Location","setSelectedLocation(DEFAULT_LOCATION) called","✅ Hard","Location set for downstream steps","POST /tests/getlocations","tests_packages/LocationAPITest.java"),

# ── [04] Brand API ────────────────────────────────────────────────────────
("[04]","Brand API — testGetBrands_ForMember","4.1 Response","HTTP status","✅ Hard","200","POST /brand/getAllBrands (membership server)","tests_packages/BrandAPITest.java"),
("[04]","Brand API — testGetBrands_ForMember","4.1 Response","success","✅ Hard","true","POST /brand/getAllBrands (membership server)","tests_packages/BrandAPITest.java"),
("[04]","Brand API — testGetBrands_ForMember","4.1 Response","data list","✅ Hard","data != null && data.size() > 0","POST /brand/getAllBrands (membership server)","tests_packages/BrandAPITest.java"),
("[04]","Brand API — testGetBrands_ForMember","4.2 Per-brand","data[i].title","ℹ️ Info","Brand name stored via RequestContext.storeBrand(title, guid)","POST /brand/getAllBrands (membership server)","tests_packages/BrandAPITest.java"),
("[04]","Brand API — testGetBrands_ForMember","4.2 Per-brand","data[i].Guid","ℹ️ Info","Brand ID stored alongside name","POST /brand/getAllBrands (membership server)","tests_packages/BrandAPITest.java"),
("[04]","Brand API — testGetBrands_ForMember","4.3 Critical Brand","'Diagnostics' brand ID resolved","✅ Hard","Non-null GUID; hardcoded fallback '967a5f02-2e38-47c8-b850-c4aeee8898ed'","POST /brand/getAllBrands (membership server)","tests_packages/BrandAPITest.java"),
("[04]","Brand API — testGetBrands_ForMember","4.3 Critical Brand","setSelectedBrand('Diagnostics')","ℹ️ Info","Used in GET_CART_BY_ID query param","POST /brand/getAllBrands (membership server)","tests_packages/BrandAPITest.java"),

# ── [05] Global Search ────────────────────────────────────────────────────
("[05]","Global Search — testGlobalSearch_ForMember","5.1 Tests searched","Bone Profile -1","ℹ️ Info","Searched and stored with test_id, b2b_price, rewards_pct, home_collection, diseases, organ","POST tests/adminTests","tests_packages/GlobalSearchAPITest.java"),
("[05]","Global Search — testGlobalSearch_ForMember","5.1 Tests searched","RANDOM BLOOD GLUCOSE (RBS)","ℹ️ Info","Same fields stored","POST tests/adminTests","tests_packages/GlobalSearchAPITest.java"),
("[05]","Global Search — testGlobalSearch_ForMember","5.1 Tests searched","CLOTTING TIME","ℹ️ Info","Same fields stored","POST tests/adminTests","tests_packages/GlobalSearchAPITest.java"),
("[05]","Global Search — testGlobalSearch_ForMember","5.1 Tests searched","Complete Blood Count (CBC)","ℹ️ Info","Same fields stored","POST tests/adminTests","tests_packages/GlobalSearchAPITest.java"),
("[05]","Global Search — testGlobalSearch_ForMember","5.1 Tests searched","T4 - THYROXINE","ℹ️ Info","Same fields stored","POST tests/adminTests","tests_packages/GlobalSearchAPITest.java"),
("[05]","Global Search — testGlobalSearch_ForMember","5.2 Validations","Location ID from context (DEFAULT_LOCATION)","✅ Hard","Must be non-null (log warning if missing)","POST tests/adminTests","tests_packages/GlobalSearchAPITest.java"),
("[05]","Global Search — testGlobalSearch_ForMember","5.2 Validations","Tests stored in RequestContext.getAllTests()","✅ Hard","GlobalSearchHelper.extractAndStoreTests() must populate context","POST tests/adminTests","tests_packages/GlobalSearchAPITest.java"),
("[05]","Global Search — testGlobalSearch_ForMember","5.2 Validations","Unexpected test guard","⚠️ Soft","Logs warning if un-searched test ends up in context","POST tests/adminTests","tests_packages/GlobalSearchAPITest.java"),
("[05]","Global Search — testGlobalSearch_ForMember","5.2 Validations","home_collection analysis","ℹ️ Info","Counts AVAILABLE vs NOT AVAILABLE; no hard assertion on counts","POST tests/adminTests","tests_packages/GlobalSearchAPITest.java"),

# ── [06] Catalog Verification ─────────────────────────────────────────────
("[06]","Catalog Verification — verifyDiagnosticsCatalog","Priority 1 — Diagnostics","HTTP status","✅ Hard","200","POST /tests/getAllTests","tests_packages/CatalogVerificationAPITest.java"),
("[06]","Catalog Verification — verifyDiagnosticsCatalog","Priority 1 — Diagnostics","Target item found in paginated results (≤ 5 pages)","✅ Hard","Name match (contains); target from context Strategy 1 or API Strategy 2","POST /tests/getAllTests","tests_packages/CatalogVerificationAPITest.java"),
("[06]","Catalog Verification — verifyDiagnosticsCatalog","Priority 1 — Diagnostics","Item price > 0","✅ Hard","verifyPriceAndDetails(item)","POST /tests/getAllTests","tests_packages/CatalogVerificationAPITest.java"),
("[06]","Catalog Verification — verifyDnaDecoderCatalog","Priority 2 — DNA Decoder","HTTP status","✅ Hard","200","POST /tests/getAllTests (dnadecoder:true)","tests_packages/CatalogVerificationAPITest.java"),
("[06]","Catalog Verification — verifyDnaDecoderCatalog","Priority 2 — DNA Decoder","Target name not null","✅ Hard","Dynamic fetch fallback if context empty","POST /tests/getAllTests (dnadecoder:true)","tests_packages/CatalogVerificationAPITest.java"),
("[06]","Catalog Verification — verifyPgxCatalog","Priority 3 — PGX","HTTP status","✅ Hard","200","POST /tests/getAllTests (pharmacogenomics:true)","tests_packages/CatalogVerificationAPITest.java"),
("[06]","Catalog Verification — verifyPgxCatalog","Priority 3 — PGX","Target name not null","✅ Hard","Dynamic fetch fallback","POST /tests/getAllTests (pharmacogenomics:true)","tests_packages/CatalogVerificationAPITest.java"),
("[06]","Catalog Verification — verifyFetalMedicineCatalog","Priority 4 — Fetal Medicine","HTTP status","✅ Hard","200","POST /tests/getFetalMedicineTests","tests_packages/CatalogVerificationAPITest.java"),
("[06]","Catalog Verification — verifyFetalMedicineCatalog","Priority 4 — Fetal Medicine","Target name not null","✅ Hard","Dynamic fetch fallback","POST /tests/getFetalMedicineTests","tests_packages/CatalogVerificationAPITest.java"),
("[06]","Catalog Verification — verifyPackagesCatalog","Priority 5 — Packages","HTTP status","✅ Hard","200","POST /tests/getAllPackages","tests_packages/CatalogVerificationAPITest.java"),
("[06]","Catalog Verification — verifyPackagesCatalog","Priority 5 — Packages","Target name not null","✅ Hard","Dynamic fetch fallback","POST /tests/getAllPackages","tests_packages/CatalogVerificationAPITest.java"),

# ── [07] Add to Cart ──────────────────────────────────────────────────────
("[07]","Add to Cart — testAddToCart_ForMember","7.1 Request","user_id","ℹ️ Info","RequestContext.getUserId()","POST /carts/v2/addCart","cart/AddToCartAPITest.java"),
("[07]","Add to Cart — testAddToCart_ForMember","7.1 Request","lab_location_id","ℹ️ Info","RequestContext.getLocationId(DEFAULT_LOCATION)","POST /carts/v2/addCart","cart/AddToCartAPITest.java"),
("[07]","Add to Cart — testAddToCart_ForMember","7.1 Request","product_details[]","ℹ️ Info","Built from RequestContext.getAllTests()","POST /carts/v2/addCart","cart/AddToCartAPITest.java"),
("[07]","Add to Cart — testAddToCart_ForMember","7.1 Request","coupon_guid","ℹ️ Info","Resolved if coupon flow enabled for userType; else absent","POST /carts/v2/addCart","cart/AddToCartAPITest.java"),
("[07]","Add to Cart — testAddToCart_ForMember","7.2 Response","HTTP status","✅ Hard","200","POST /carts/v2/addCart","cart/AddToCartAPITest.java"),
("[07]","Add to Cart — testAddToCart_ForMember","7.2 Response","success","✅ Hard","true","POST /carts/v2/addCart","cart/AddToCartAPITest.java"),
("[07]","Add to Cart — testAddToCart_ForMember","7.2 Response","data.guid (cart ID)","✅ Hard","Not null; stored to RequestContext.setCurrentCartId()","POST /carts/v2/addCart","cart/AddToCartAPITest.java"),
("[07]","Add to Cart — testAddToCart_ForMember","7.2 Response","data.totalPrice","✅ Hard","= sum(unitPrice x qty) for each product across all cart items. Must be > 0. Stored to RequestContext.setCurrentTotalPrice().","POST /carts/v2/addCart","cart/AddToCartAPITest.java"),
("[07]","Add to Cart — testAddToCart_ForMember","7.2 Response","data.totalPrice < 2500","✅ Hard","COD limit enforcement — test fails if ≥ 2500","POST /carts/v2/addCart","cart/AddToCartAPITest.java"),
("[07]","Add to Cart — testAddToCart_ForMember","7.2 Response","coupon_guid (coupon flow)","✅ Hard","Non-null in response when coupon flow is enabled","POST /carts/v2/addCart","cart/AddToCartAPITest.java"),
("[07]","Add to Cart — testAddToCart_ForMember","7.3 Price Formula","coupon_amount (percentage)","ℹ️ Info","FORMULA: coupon_amount = min(subtotal x pct / 100, max_redeemable). Stored to RequestContext.setCouponAmount().","POST /carts/v2/addCart","cart/AddToCartAPITest.java"),
("[07]","Add to Cart — testAddToCart_ForMember","7.3 Price Formula","coupon_amount (flat)","ℹ️ Info","FORMULA: coupon_amount = flat_discount_value (fixed amount from coupon record). Stored to RequestContext.setCouponAmount().","POST /carts/v2/addCart","cart/AddToCartAPITest.java"),
("[07]","Add to Cart — testAddToCart_ForMember","7.3 Price Formula","payable = subtotal - coupon_amount","ℹ️ Info","FORMULA: payable = totalPrice - coupon_amount. Stored to RequestContext.setCurrentDueAmount(). Must be > 0.","POST /carts/v2/addCart","cart/AddToCartAPITest.java"),

# ── [07-C] Coupon Flow ────────────────────────────────────────────────────
("[07-C]","Coupon Flow (COUPON SUITES ONLY)","TC_CPN_012","Apply valid coupon to cart","✅ Hard","HTTP 200; coupon applied in response","POST /coupons/applyCoupon","coupons/CouponComprehensiveTest.java"),
("[07-C]","Coupon Flow (COUPON SUITES ONLY)","TC_CPN_021","Total price after coupon subtraction","✅ Hard","totalPrice − coupon_amount = payable","POST /coupons/applyCoupon","coupons/CouponComprehensiveTest.java"),
("[07-C]","Coupon Flow (COUPON SUITES ONLY)","TC_CPN_017","Remaining payable after coupon","✅ Hard","payable_amount > 0","POST /coupons/applyCoupon","coupons/CouponComprehensiveTest.java"),
("[07-C]","Coupon Flow (COUPON SUITES ONLY)","TC_CPN_019","Coupon persists in cart","✅ Hard","Reload cart; coupon_guid and coupon_amount must still be present","GET /carts/v2/getCartById/{user_id}","coupons/CouponComprehensiveTest.java"),
("[07-C]","Coupon Flow (COUPON SUITES ONLY)","TC_CPN_018","Admin vs cash split","✅ Hard","Coupon discount subtracted from user payable; admin bears nothing","—","coupons/CouponComprehensiveTest.java"),

# ── [08] Get Cart / Verify Price ─────────────────────────────────────────
("[08]","Get Cart — step02_VerifyCartAndPrice","8.1 Response","HTTP status","✅ Hard","200 (or fallback from context)","GET /carts/v2/getCartById/{user_id}","cart/COD_02_GetCartTest.java"),
("[08]","Get Cart — step02_VerifyCartAndPrice","8.1 Response","data.guid (cart ID)","✅ Hard","Not null; stored to RequestContext.setCurrentCartId()","GET /carts/v2/getCartById/{user_id}","cart/COD_02_GetCartTest.java"),
("[08]","Get Cart — step02_VerifyCartAndPrice","8.1 Response","data.totalPrice","✅ Hard","CROSS-STEP EXACT: must equal [07] AddToCart totalPrice (same cart re-fetched). Must be < 2500 (COD limit). Stored to RequestContext.setCurrentTotalPrice() for downstream price cross-checks.","GET /carts/v2/getCartById/{user_id}","cart/COD_02_GetCartTest.java"),
("[08]","Get Cart — step02_VerifyCartAndPrice","8.1 Response","data.totalPrice < 2500","✅ Hard","Assert.fail if ≥ 2500 (COD limit)","GET /carts/v2/getCartById/{user_id}","cart/COD_02_GetCartTest.java"),
("[08]","Get Cart — step02_VerifyCartAndPrice","8.1 Response","data.coupon_amount","ℹ️ Info","Stored to RequestContext.setCouponAmount()","GET /carts/v2/getCartById/{user_id}","cart/COD_02_GetCartTest.java"),
("[08]","Get Cart — step02_VerifyCartAndPrice","8.1 Response","data.due_amount / payable_amount","ℹ️ Info","FORMULA: due_amount = totalPrice - coupon_amount. Stored to RequestContext.setCurrentDueAmount() — used in rewards_gain formula at steps [13] and [14].","GET /carts/v2/getCartById/{user_id}","cart/COD_02_GetCartTest.java"),
("[08]","Get Cart — step02_VerifyCartAndPrice","8.2 Cart Item Cross-Validation","rewards_percentage match vs Global Search","ℹ️ Info","Expected vs actual; logged","GET /carts/v2/getCartById/{user_id}","cart/COD_02_GetCartTest.java"),
("[08]","Get Cart — step02_VerifyCartAndPrice","8.2 Cart Item Cross-Validation","b2b_price match (±0.01) vs Global Search","ℹ️ Info","Expected vs actual; logged","GET /carts/v2/getCartById/{user_id}","cart/COD_02_GetCartTest.java"),
("[08]","Get Cart — step02_VerifyCartAndPrice","8.2 Cart Item Cross-Validation","diseases match vs Global Search","ℹ️ Info","String comparison; logged","GET /carts/v2/getCartById/{user_id}","cart/COD_02_GetCartTest.java"),
("[08]","Get Cart — step02_VerifyCartAndPrice","8.2 Cart Item Cross-Validation","organ match vs Global Search","ℹ️ Info","String comparison; logged","GET /carts/v2/getCartById/{user_id}","cart/COD_02_GetCartTest.java"),
("[08]","Get Cart — step02_VerifyCartAndPrice","8.3 Coupon (coupon flow)","coupon_guid in cart response","✅ Hard","Non-null when coupon flow expected (unless cart already converted to order)","GET /carts/v2/getCartById/{user_id}","cart/COD_02_GetCartTest.java"),
("[08]","Get Cart — step02_VerifyCartAndPrice","8.3 Coupon (coupon flow)","couponResult.valid = true","✅ Hard","Validates coupon is still active","GET /carts/v2/getCartById/{user_id}","cart/COD_02_GetCartTest.java"),
("[08]","Get Cart — step02_VerifyCartAndPrice","8.3 Coupon (coupon flow)","couponResult.reason = 'already redeemed'","⚠️ Soft","Business rule: 1 use per user; soft-pass on staging reruns","GET /carts/v2/getCartById/{user_id}","cart/COD_02_GetCartTest.java"),

# ── [09] Add Lab Slot ─────────────────────────────────────────────────────
("[09]","Add Lab Slot — step03_AddLabSlot","9.1 Slot Selection","Available slot found (slot.guid)","✅ Hard","Assert.assertNotNull(slotGuid, 'Lab Slot GUID required')","GET /slot/getSlotCountByTime","slot/COD_03_AddLabSlotTest.java"),
("[09]","Add Lab Slot — step03_AddLabSlot","9.1 Slot Selection","slot.date + slot.time stored","✅ Hard","RequestContext.setExpectedSlotTiming(date, time)","GET /slot/getSlotCountByTime","slot/COD_03_AddLabSlotTest.java"),
("[09]","Add Lab Slot — step03_AddLabSlot","9.2 Center Resolution","centerId resolved","ℹ️ Info","getSelectedLocationId() → fallback getLocationId(DEFAULT_LOCATION)","GET /slot/getSlotCountByTime","slot/COD_03_AddLabSlotTest.java"),
("[09]","Add Lab Slot — step03_AddLabSlot","9.3 Cart Update","Cart update for lab visit","✅ Hard","HTTP 200; order_type=lab, slot_guid set, address_id=null","POST /carts/v2/addCart","slot/COD_03_AddLabSlotTest.java"),

# ── [10] Payment Pre-Check ────────────────────────────────────────────────
("[10]","Verify Payment Pre-Check — step04_VerifyPaymentPreCheck","10.1 Inputs","cartId, addressId, slotGuid, totalPrice, labLocationId, date, time","ℹ️ Info","All resolved from RequestContext","POST /gateway/v2/VerifyPayment","payment/COD_04_VerifyPaymentPreCheckTest.java"),
("[10]","Verify Payment Pre-Check — step04_VerifyPaymentPreCheck","10.2 Response","HTTP status","✅ Hard","200","POST /gateway/v2/VerifyPayment","payment/COD_04_VerifyPaymentPreCheckTest.java"),
("[10]","Verify Payment Pre-Check — step04_VerifyPaymentPreCheck","10.2 Response","data.payment_id","✅ Hard","Not null; stored to RequestContext.setCurrentPaymentId()","POST /gateway/v2/VerifyPayment","payment/COD_04_VerifyPaymentPreCheckTest.java"),
("[10]","Verify Payment Pre-Check — step04_VerifyPaymentPreCheck","10.2 Response","data.order_id","✅ Hard","Not null; stored to RequestContext.setCurrentOrderId()","POST /gateway/v2/VerifyPayment","payment/COD_04_VerifyPaymentPreCheckTest.java"),

# ── [11] Cross-API Validation ─────────────────────────────────────────────
("[11]","Cross-API Validation — step05_CrossApiValidation","11.1 Internal APIs","GET /gateway/getPaymentById","ℹ️ Info","Fetches payment record for comparison","POST /gateway/getPaymentById","order/COD_05_CrossApiValidationTest.java"),
("[11]","Cross-API Validation — step05_CrossApiValidation","11.1 Internal APIs","GET /carts/v2/getCartById/{user_id}","ℹ️ Info","Re-fetches cart for product name extraction","GET /carts/v2/getCartById/{user_id}","order/COD_05_CrossApiValidationTest.java"),
("[11]","Cross-API Validation — step05_CrossApiValidation","11.2 Checks","Payment record found","✅ Hard","getPaymentById returns HTTP 200","POST /gateway/getPaymentById","order/COD_05_CrossApiValidationTest.java"),
("[11]","Cross-API Validation — step05_CrossApiValidation","11.2 Checks","payment.amount ≈ cart totalPrice (±1.0)","✅ Hard","CROSS-API (hard, +-1.0): payment.amount ~= RequestContext.getCurrentTotalPrice() stored by [08] GetCart. |payment.amount - totalPrice| <= 1.0 required.","POST /gateway/getPaymentById","order/COD_05_CrossApiValidationTest.java"),
("[11]","Cross-API Validation — step05_CrossApiValidation","11.2 Checks","Product names in payment match cart product_details[]","✅ Hard","Each cart product_name found in payment order_items","POST /gateway/getPaymentById","order/COD_05_CrossApiValidationTest.java"),
("[11]","Cross-API Validation — step05_CrossApiValidation","11.2 Checks","slot_guid in payment","🔗 Cross-API","Must equal RequestContext.getCurrentSlotGuid() when present","POST /gateway/getPaymentById","order/COD_05_CrossApiValidationTest.java"),
("[11]","Cross-API Validation — step05_CrossApiValidation","11.2 Checks","user_id in payment","🔗 Cross-API","Must equal RequestContext.getUserId()","POST /gateway/getPaymentById","order/COD_05_CrossApiValidationTest.java"),
("[11]","Cross-API Validation — step05_CrossApiValidation","11.2 Checks","cart_id in payment","🔗 Cross-API","Must equal RequestContext.getCurrentCartId()","POST /gateway/getPaymentById","order/COD_05_CrossApiValidationTest.java"),

# ── [12] Initial Rewards ──────────────────────────────────────────────────
("[12]","Initial Rewards — step18_A_VerifyInitialRewards","12.1 Flow Guard","getCurrentFlowName() == 'member_flow'","ℹ️ Info","Non-member flows skip entirely with log message","GET /reward/getRewardsByMobile/{mobile}","rewards/COD_18_RewardValidationTest.java"),
("[12]","Initial Rewards — step18_A_VerifyInitialRewards","12.1 Assertion","total_rewards","✅ Hard",">= 0; stored to RequestContext.setInitialTotalRewards()","GET /reward/getRewardsByMobile/{mobile}","rewards/COD_18_RewardValidationTest.java"),

# ── [13] Approve Payment ──────────────────────────────────────────────────
("[13]","Approve Payment — step15_ApprovePayment","13.1 Request","order_guid = RequestContext.getCurrentOrderId()","ℹ️ Info","Passed in callApprovePaymentAPI()","POST /order/approvepayment","payment/COD_15_ApprovePaymentTest.java"),
("[13]","Approve Payment — step15_ApprovePayment","13.2 Response","HTTP status","✅ Hard","200","POST /order/approvepayment","payment/COD_15_ApprovePaymentTest.java"),
("[13]","Approve Payment — step15_ApprovePayment","13.2 Response","success","✅ Hard","true","POST /order/approvepayment","payment/COD_15_ApprovePaymentTest.java"),
("[13]","Approve Payment — step15_ApprovePayment","13.2 Response","data[0].rewards_gain","✅ Hard","Not null; >= 0. FORMULA: rewards_gain = ceil(dueAmount x rewards_pct / 100), where dueAmount = RequestContext.getCurrentDueAmount() from [08] and rewards_pct from GlobalSearch. Stored to RequestContext.setRewardsGain().","POST /order/approvepayment","payment/COD_15_ApprovePaymentTest.java"),
("[13]","Approve Payment — step15_ApprovePayment","13.2 Response","data[0].order_status","✅ Hard","'paid'","POST /order/approvepayment","payment/COD_15_ApprovePaymentTest.java"),
("[13]","Approve Payment — step15_ApprovePayment","13.2 Response","data[0].payment_status","✅ Hard","'Success'","POST /order/approvepayment","payment/COD_15_ApprovePaymentTest.java"),

# ── [14] Rewards Gain Validation ─────────────────────────────────────────
("[14]","Rewards Gain — step18_B_VerifyRewardsGain","14.1 Flow Guard","getCurrentFlowName() == 'member_flow'","ℹ️ Info","Skipped for non-member","—","rewards/COD_18_RewardValidationTest.java"),
("[14]","Rewards Gain — step18_B_VerifyRewardsGain","14.1 Assertions","actualGain > 0","✅ Hard","Rewards must have been earned on payment","—","rewards/COD_18_RewardValidationTest.java"),
("[14]","Rewards Gain — step18_B_VerifyRewardsGain","14.1 Assertions","actualGain ≈ dueAmount × rewards_pct/100","✅ Hard","FORMULA: actualGain = ceil(RequestContext.getCurrentDueAmount() x rewards_pct / 100). rewards_pct from GlobalSearch response. dueAmount = what user paid after coupon from [08].","—","rewards/COD_18_RewardValidationTest.java"),

# ── [15] Final Rewards Balance ────────────────────────────────────────────
("[15]","Final Rewards Balance — step18_C_VerifyFinalRewardsBalance","15.1 Formula","expected = initialBalance + rewardsGain","ℹ️ Info","FORMULA: expected = RequestContext.getInitialTotalRewards() [step 12] + RequestContext.getRewardsGain() [step 13]. Fresh GET call must confirm actual == expected.","GET /reward/getRewardsByMobile/{mobile}","rewards/COD_18_RewardValidationTest.java"),
("[15]","Final Rewards Balance — step18_C_VerifyFinalRewardsBalance","15.1 Assertions","|actual − expected| < 0.1","✅ Hard","HARD: |getRewardsByMobile().total_rewards - (initialBalance + rewardsGain)| < 0.1. initialBalance = getInitialTotalRewards() [step 12], rewardsGain = getRewardsGain() [step 13].","GET /reward/getRewardsByMobile/{mobile}","rewards/COD_18_RewardValidationTest.java"),
("[15]","Final Rewards Balance — step18_C_VerifyFinalRewardsBalance","15.1 Assertions","actual >= 0","✅ Hard","Balance cannot go negative","GET /reward/getRewardsByMobile/{mobile}","rewards/COD_18_RewardValidationTest.java"),
("[15]","Final Rewards Balance — step18_C_VerifyFinalRewardsBalance","15.2 Storage","setFinalTotalRewards(actualBalance)","ℹ️ Info","Used in step [21] rewards reversal check","GET /reward/getRewardsByMobile/{mobile}","rewards/COD_18_RewardValidationTest.java"),

# ── [16] Process Cashback (step20_A) ─────────────────────────────────────
("[16]","Process Cashback — step20_A_ProcessCashback","16.1 Top-level","status (integer)","✅ Hard","Not null; equals HTTP status code","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.1 Top-level","success (boolean)","✅ Hard","Not null","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.1 Top-level","msg (string)","✅ Hard","Not null","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.1 Top-level","message (200 only)","✅ Hard","Not null when HTTP 200","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.1 Top-level","total_amount","ℹ️ Info","Always 0 (dummy field); logged only","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.2 HTTP 409","success","✅ Hard","Must be false when phlebotomist already assigned","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.2 HTTP 409","msg","✅ Hard","Contains one of: cancel / phlebotomist / assigned / cannot","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.3 membershipCancelAmount (outer)","paid_amount → memberDiscountAmt","✅ Hard",">= 0. SEMANTIC: This field is the membership DISCOUNT amount -- NOT cash paid by user. Formula role: actual_price - paid_amount(memberDiscountAmt) - couponSplit = canceled_amount.orderedByCash (actual cash).","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.3 membershipCancelAmount (outer)","orderItemAmount","✅ Hard",">= 0. Rewards-eligible portion","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.3 membershipCancelAmount (outer)","actual_price","✅ Hard",">= 0. Full list price before membership discount","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.3 membershipCancelAmount (outer)","remaining_rewards","✅ Hard","Not null; any integer (can be negative). Current wallet balance","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.3 membershipCancelAmount (outer)","actual_taking_rewards","✅ Hard","Must be exactly 0 (API contract)","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.3 membershipCancelAmount (outer)","adjustedRefundAmount","✅ Hard","EXACT EQUALITY (hard): adjustedRefundAmount = canceled_amount.orderedByCash. Cash refund to user -- must be exactly equal, not merely >= 0.","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.4 String/Boolean fields","reference_code","✅ Hard (present) / ⚠️ Soft (null)","Must start with 'MY'. Stored to setCurrentOrderSampleNumber(). Null OK for non-member","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.4 String/Boolean fields","is_delivery_charge_added","✅ Hard","Not null. If true → actual_price > orderedByCash","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.4 String/Boolean fields","adjustedMessage","✅ Hard","Not null; not empty; must contain numeric value of orderedByCash","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.4 String/Boolean fields","walletUpdate","ℹ️ Info","Expected '' for single-member; logged only","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.5 canceled_amount (nested)","orderedByCash","✅ Hard","> 0. FORMULA: orderedByCash = actual_price - memberDiscountAmt(paid_amount) - couponSplitThisOrder. CROSS-API (single order hard): orderedByCash == RequestContext.getCurrentTotalPrice() from [08].","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.5 canceled_amount (nested)","orderItemAmount","✅ Hard","Must equal orderedByCash","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.5 canceled_amount (nested)","actual_price","✅ Hard",">= 0","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.5 canceled_amount (nested)","reference_code","✅ Hard","Must equal outer membershipCancelAmount.reference_code","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.5 canceled_amount (nested)","remaining_rewards","✅ Hard","Not null; any integer (can be negative). Historical cumulative accounting","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.5 canceled_amount (nested)","actual_taking_rewards","✅ Hard",">= 0","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.6 Consistency / Cross-field","actual_price − memberDiscountAmt ≥ orderedByCash","✅ Hard","FORMULA: actual_price - memberDiscountAmt >= orderedByCash. Gap = couponSplitThisOrder (0 when no coupon). Full derivation: couponSplit = (actual_price - memberDiscountAmt) - orderedByCash.","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.6 Consistency / Cross-field","adjustedRefundAmount == canceled_amount.orderedByCash","✅ Hard","Refund equals cash paid","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.6 Consistency / Cross-field","actual_price ≥ memberDiscountAmt","✅ Hard","Price sanity","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.6 Consistency / Cross-field","outer.actual_taking_rewards == 0","✅ Hard","API contract","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.6 Consistency / Cross-field","canceled_amount.orderItemAmount == orderedByCash","✅ Hard","Cash view consistency","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.6 Consistency / Cross-field","outer.remaining_rewards ≥ 0","✅ Hard","Wallet non-negative","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.6 Consistency / Cross-field","adjustedMessage contains cash amount","✅ Hard","Contains String.valueOf((int)orderedByCash)","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.6 Consistency / Cross-field","outer.actual_price ≠ nested.actual_price (multi-member)","⚠️ Soft","Different sub-totals expected in multi-member orders","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.7 Cross-API","canceled_amount.orderedByCash == storedCartTotal","✅ Hard","CROSS-API EXACT (single order): canceled_amount.orderedByCash == RequestContext.getCurrentTotalPrice() stored by [08] GetCart. Multi-member: sum of sub-orders ~= storedCartTotal (+-1.0 soft).","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),
("[16]","Process Cashback — step20_A_ProcessCashback","16.7 Cross-API","outer.remaining_rewards == rewardsGainForOrder","✅ Hard (when stored > 0)","CROSS-API EXACT (when stored > 0): outer.remaining_rewards == RequestContext.getRewardsGain() from step [13] approvepayment. This is order-specific rewards earn, not total wallet balance.","POST /order/adminReturningCashback","order/COD_20_CancellationRefundTest.java"),

# ── [17] Update to Cancelled (step20_B) ──────────────────────────────────
("[17]","Update to Cancelled — step20_B_FinalUpdateOrderCancelled","17.1 Response","HTTP status","✅ Hard","200","POST /order/v2updateOrder","order/COD_20_CancellationRefundTest.java"),
("[17]","Update to Cancelled — step20_B_FinalUpdateOrderCancelled","17.1 Response","msg","✅ Hard","Not null; equals 'Order updated successfully'","POST /order/v2updateOrder","order/COD_20_CancellationRefundTest.java"),
("[17]","Update to Cancelled — step20_B_FinalUpdateOrderCancelled","17.1 Cross-API GET Check","data[0].order_status after GET","✅ Hard","Calls getOrderById after update; must equal 'Cancelled'","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),

# ── [18] Approve Cancellation (step20_C) ─────────────────────────────────
("[18]","Approve Cancellation — step20_C_ApproveCancelledOrder","18.1 Response","HTTP status","✅ Hard","200","POST /order/approveCancelldOrder","order/COD_20_CancellationRefundTest.java"),
("[18]","Approve Cancellation — step20_C_ApproveCancelledOrder","18.1 Response","success (when present)","✅ Hard","true","POST /order/approveCancelldOrder","order/COD_20_CancellationRefundTest.java"),
("[18]","Approve Cancellation — step20_C_ApproveCancelledOrder","18.1 Response","msg or message (when success absent)","✅ Hard","At least one non-null","POST /order/approveCancelldOrder","order/COD_20_CancellationRefundTest.java"),
("[18]","Approve Cancellation — step20_C_ApproveCancelledOrder","18.1 Response","error (when present)","✅ Hard","Empty string","POST /order/approveCancelldOrder","order/COD_20_CancellationRefundTest.java"),

# ── [19] Verify Cancelled Order (step20_D) ───────────────────────────────
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.1 Top-level","HTTP status","✅ Hard","200","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.1 Top-level","status","✅ Hard","Equals HTTP code","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.1 Top-level","success","✅ Hard","true","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.1 Top-level","msg","✅ Hard","Not null","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.2 Order Identity","guid","✅ Hard + 🔗","Must equal RequestContext.getCurrentOrderId()","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.2 Order Identity","order_number","✅ Hard","Not null; not empty","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.2 Order Identity","order_sample_number","✅ Hard","Not null; starts with 'MY'","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.2 Order Identity","visit_number (when present)","✅ Hard","Starts with 'MYD'","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.2 Order Identity","user_id","🔗 Cross-API","Must equal RequestContext.getUserId()","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.3 Amount Fields","paid_amount (string)","✅ Hard",">= 0. FORMULA: paid_amount = total_price - membership_discount - coupon_discount. Exact cash paid after ALL discounts. Verified by: refund_amount = paid_amount (EXACT equality below).","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.3 Amount Fields","total_price (string)","✅ Hard",">= paid_amount. Full list price","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.3 Amount Fields","final_price (string)","✅ Hard","total_price >= final_price >= paid_amount","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.3 Amount Fields","membership_discount (when present)","✅ Hard",">= 0","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.3 Amount Fields","actual_discount (when present)","✅ Hard","EXACT FORMULA (hard): actual_discount = membership_discount + coupon_discount. coupon_discount resolved via fallback chain: response.coupon_discount -> coupon_discount_amount -> RequestContext.getCouponAmount().","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.3 Amount Fields","coupon_discount (when present)","✅ Hard",">= 0","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.3 Amount Fields","due_amount (single order)","✅ Hard","== 0","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.3 Amount Fields","due_amount (multi-member)","⚠️ Soft","Non-zero expected when sibling order still active","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.3 Amount Fields","refund_amount (string)","✅ Hard","EXACT EQUALITY (hard): refund_amount = paid_amount. User receives back exactly what they paid post-discount. Coupon NOT refunded separately -- already reduced paid_amount at purchase.","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.3 Amount Fields","rewards_gain (string)","✅ Hard + 🔗",">= 0. CROSS-API EXACT (hard when stored > 0): rewards_gain = RequestContext.getRewardsGain() stored by step [13] approvepayment. Verifies order records same rewards as granted.","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.3 Amount Fields","total_price − membership_discount − coupon_discount = paid_amount","✅ Hard","EXACT FORMULA (hard): total_price - membership_discount - coupon_discount = paid_amount. ALSO (separate hard assert): membership_discount + coupon_discount = actual_discount. Coupon via fallback chain.","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.4 Payment Fields","payment_id","✅ Hard + 🔗","CROSS-API EXACT: payment_id == RequestContext.getCurrentPaymentId() stored by step [10] VerifyPaymentPreCheck.","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.4 Payment Fields","payment.payment_type","✅ Hard","Not null (e.g. 'COD')","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.4 Payment Fields","payment.payment_mode","✅ Hard","null for COD; non-null for online","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.4 Payment Fields","payment.payment_status","✅ Hard","'Success'","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.4 Payment Fields","payment_statuses","✅ Hard","'Success'","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.5 Slot Fields","slot_guid","✅ Hard + 🔗","CROSS-API EXACT: slot_guid == RequestContext.getCurrentSlotGuid() stored by step [09] AddLabSlot.","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.5 Slot Fields","slot_start_time","✅ Hard","Not null","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.5 Slot Fields","slot_end_time","✅ Hard","Not null","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.6 Cancellation Status","order_status","✅ Hard","'Cancelled'","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.6 Cancellation Status","cancelled_date","✅ Hard","Not null; not empty","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.6 Cancellation Status","cancelled_by_user_at","✅ Hard","Not null","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.6 Cancellation Status","it_dose_order_status","✅ Hard","'Cancelled'","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.7 Admin Approval","admin_approval_status (when present)","✅ Hard","'Approved'","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.7 Admin Approval","admin_approval_status (when null)","⚠️ Soft","Not always set in COD cash-refund flows","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.7 Admin Approval","admin_approval_by (when present)","✅ Hard + 🔗","Equals RequestContext.getAdminGuid()","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.7 Admin Approval","admin_approval_at (when present)","✅ Hard","Non-empty timestamp","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.8 user_details","user_details.guid","✅ Hard","Must equal data[0].user_id","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.8 user_details","user_details.first_name","✅ Hard","Not null","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.8 user_details","user_details.mobile","✅ Hard","Not null","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.9 order_items[] per item","order_id","✅ Hard","Must equal main order guid","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.9 order_items[] per item","product_name","✅ Hard","Not null; not empty","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.9 order_items[] per item","order_item_number","✅ Hard","Pattern: '{order_sample_number}-{itemIndex}'","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.9 order_items[] per item","order_status","✅ Hard","'Cancelled'","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.9 order_items[] per item","it_dose_order_items_status","⚠️ Soft","Expected 'Cancelled'; may lag async in multi-member","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.9 order_items[] per item","cancelled_at","✅ Hard","Not null","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.9 order_items[] per item","actual_price","✅ Hard","> 0","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.9 order_items[] per item","final_price","✅ Hard","> 0; <= actual_price","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.9 order_items[] per item","membership_discount","✅ Hard",">= 0","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.9 order_items[] per item","actual_price − membership_discount = final_price","✅ Hard","EXACT FORMULA PER ITEM (hard): item.actual_price - item.membership_discount = item.final_price. Accumulate sumItemFinalPrices += final_price for post-loop sum check. Also: item.actual_discount = item.membership_discount (when present, hard).","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.9 order_items[] per item","sample_types[].Tests[].TestName","✅ Hard","Not null; not empty per test","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.10 Sum Assertion","sum(order_items[].final_price) − couponDiscThisOrder = paid_amount","✅ Hard","EXACT SUM FORMULA (hard): sum(order_items[].final_price) - couponDiscThisOrder = paid_amount. Each item.final_price = actual_price - membership_discount (pre-coupon). couponDiscThisOrder from fallback chain: response.coupon_discount -> coupon_discount_amount -> RequestContext.getCouponAmount().","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.11 Sibling Order (multi-member)","Sibling order_status","✅ Hard","Must NOT be 'Cancelled' — only cancelling sub-order was cancelled","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),
("[19]","Verify Cancelled Order — step20_D_VerifyCancelledOrderDetails","19.11 Sibling Order (multi-member)","Sibling refund_amount (when present)","✅ Hard","Must be 0 — sibling was not cancelled","GET /order/getOrderById/{guid}","order/COD_20_CancellationRefundTest.java"),

# ── [20] Verify Payment (step20_E) ────────────────────────────────────────
("[20]","Verify Payment — step20_E_VerifyPaymentById","20.1 Top-level","HTTP status (up to 3 retries on 5xx)","✅ Hard","200","POST /gateway/getPaymentById","order/COD_20_CancellationRefundTest.java"),
("[20]","Verify Payment — step20_E_VerifyPaymentById","20.1 Top-level","success (when present)","✅ Hard","true","POST /gateway/getPaymentById","order/COD_20_CancellationRefundTest.java"),
("[20]","Verify Payment — step20_E_VerifyPaymentById","20.1 Top-level","msg","✅ Hard","Not null","POST /gateway/getPaymentById","order/COD_20_CancellationRefundTest.java"),
("[20]","Verify Payment — step20_E_VerifyPaymentById","20.2 data.payments","guid","✅ Hard + 🔗","CROSS-API EXACT: data.payments.guid == RequestContext.getCurrentPaymentId() stored by step [10] VerifyPaymentPreCheck.","POST /gateway/getPaymentById","order/COD_20_CancellationRefundTest.java"),
("[20]","Verify Payment — step20_E_VerifyPaymentById","20.2 data.payments","payment_type","✅ Hard","'COD'","POST /gateway/getPaymentById","order/COD_20_CancellationRefundTest.java"),
("[20]","Verify Payment — step20_E_VerifyPaymentById","20.2 data.payments","payment_status","✅ Hard (not null) / ℹ️ Info (value)","Accepted: Refunded / Cancelled / cancelled_refund / Approved / Paid / Success","POST /gateway/getPaymentById","order/COD_20_CancellationRefundTest.java"),
("[20]","Verify Payment — step20_E_VerifyPaymentById","20.2 data.payments","amount","✅ Hard",">= 0. CROSS-API (info, +-1.0): amount ~= RequestContext.getCurrentTotalPrice() from [08]. If differs, difference = membership_discount applied. Price math (info): actual_price - membership_discount - coupon_discount ~= amount.","POST /gateway/getPaymentById","order/COD_20_CancellationRefundTest.java"),
("[20]","Verify Payment — step20_E_VerifyPaymentById","20.2 data.payments","net_payable (when present)","✅ Hard",">= 0","POST /gateway/getPaymentById","order/COD_20_CancellationRefundTest.java"),
("[20]","Verify Payment — step20_E_VerifyPaymentById","20.2 data.payments","membership_discount (when present)","✅ Hard",">= 0","POST /gateway/getPaymentById","order/COD_20_CancellationRefundTest.java"),
("[20]","Verify Payment — step20_E_VerifyPaymentById","20.2 data.payments","coupon_discount (when present)","✅ Hard + ⚠️ Soft",">= 0. CROSS-API SOFT (+-1.0): coupon_discount ~= RequestContext.getCouponAmount() from [08]. Price math (info): actual_price - membership_discount - coupon_discount ~= amount (+-1.0). Soft on mismatch.","POST /gateway/getPaymentById","order/COD_20_CancellationRefundTest.java"),
("[20]","Verify Payment — step20_E_VerifyPaymentById","20.2 data.payments","coupon_guid (when stored)","🔗 Cross-API","Equals getMemberCouponGuid(); Soft on mismatch","POST /gateway/getPaymentById","order/COD_20_CancellationRefundTest.java"),
("[20]","Verify Payment — step20_E_VerifyPaymentById","20.2 data.payments","payment_mode","✅ Hard (non-COD) / ℹ️ Info (COD)","null for COD; non-null for UPI/Card/Online","POST /gateway/getPaymentById","order/COD_20_CancellationRefundTest.java"),
("[20]","Verify Payment — step20_E_VerifyPaymentById","20.3 data.order_items[]","Array not null; not empty","✅ Hard","—","POST /gateway/getPaymentById","order/COD_20_CancellationRefundTest.java"),
("[20]","Verify Payment — step20_E_VerifyPaymentById","20.3 data.order_items[]","product_name","✅ Hard","Not null; not empty","POST /gateway/getPaymentById","order/COD_20_CancellationRefundTest.java"),
("[20]","Verify Payment — step20_E_VerifyPaymentById","20.3 data.order_items[]","final_price","✅ Hard",">= 0","POST /gateway/getPaymentById","order/COD_20_CancellationRefundTest.java"),
("[20]","Verify Payment — step20_E_VerifyPaymentById","20.3 data.order_items[]","quantity","✅ Hard","> 0","POST /gateway/getPaymentById","order/COD_20_CancellationRefundTest.java"),
("[20]","Verify Payment — step20_E_VerifyPaymentById","20.3 data.order_items[]","order_id","✅ Hard + 🔗","Equals getCurrentOrderId() (single) or one of getCurrentOrderIds() (multi)","POST /gateway/getPaymentById","order/COD_20_CancellationRefundTest.java"),

# ── [21] Verify Rewards (step20_F) ────────────────────────────────────────
("[21]","Verify Rewards After Cancellation — step20_F_VerifyRewardsByMobile","21.1 Top-level","HTTP status","✅ Hard","200","GET /reward/getRewardsByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[21]","Verify Rewards After Cancellation — step20_F_VerifyRewardsByMobile","21.1 Top-level","success (when present)","✅ Hard","true","GET /reward/getRewardsByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[21]","Verify Rewards After Cancellation — step20_F_VerifyRewardsByMobile","21.1 Top-level","msg","✅ Hard","Not null","GET /reward/getRewardsByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[21]","Verify Rewards After Cancellation — step20_F_VerifyRewardsByMobile","21.2 data fields","total_rewards","✅ Hard",">= 0. Stored as postCancelRewards. Cross-checks: (1) postCancelRewards < finalRewards (hard); (2) finalRewards - postCancelRewards ~= rewardsGain (+-1.0 hard); (3) postCancelRewards ~= initialRewards (info full-reversal).","GET /reward/getRewardsByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[21]","Verify Rewards After Cancellation — step20_F_VerifyRewardsByMobile","21.2 data fields","customer_id","✅ Hard","Not null; stored to setCurrentMembershipCustomerId() — used in step [22]","GET /reward/getRewardsByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[21]","Verify Rewards After Cancellation — step20_F_VerifyRewardsByMobile","21.2 data fields","mobile","✅ Hard + 🔗","Equals RequestContext.getMobile()","GET /reward/getRewardsByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[21]","Verify Rewards After Cancellation — step20_F_VerifyRewardsByMobile","21.3 Reversal Checks","postCancelRewards < finalRewards","✅ Hard (when finalRewards > 0)","Cancellation reverses earned rewards","GET /reward/getRewardsByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[21]","Verify Rewards After Cancellation — step20_F_VerifyRewardsByMobile","21.3 Reversal Checks","postCancelRewards ≈ initialRewards","ℹ️ Info","Full reversal returns to pre-payment balance","GET /reward/getRewardsByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[21]","Verify Rewards After Cancellation — step20_F_VerifyRewardsByMobile","21.3 Reversal Checks","finalRewards − postCancelRewards ≈ rewardsGain","✅ Hard (when both > 0)","FORMULA (hard, +-1.0): finalRewards - postCancelRewards = rewardsGain. finalRewards = RequestContext.getFinalTotalRewards() [step 15], rewardsGain = RequestContext.getRewardsGain() [step 13]. Confirms exact reversal amount.","GET /reward/getRewardsByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[21]","Verify Rewards After Cancellation — step20_F_VerifyRewardsByMobile","21.3 Reversal Checks","postCancelRewards ≥ 0","✅ Hard","Wallet cannot go negative","GET /reward/getRewardsByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),

# ── [22] Verify Transactions (step20_G) ──────────────────────────────────
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.1 Pagination","success","✅ Hard","true","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.1 Pagination","msg","✅ Hard","Not null","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.1 Pagination","total_pages","✅ Hard",">= 1","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.1 Pagination","total","✅ Hard",">= 1 (at least 1 transaction must exist)","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.2 Transaction Pair","successTxn present","✅ Hard","At least one 'Success'/'Successful' txn for reference_code == sampleNumber","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.2 Transaction Pair","cancelledTxn present","✅ Hard","At least one 'Cancelled' txn for same reference_code","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.3 Per-transaction Identity","Guid (capital G; fallback: guid, _id)","✅ Hard","Not null; not empty","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.3 Per-transaction Identity","reference_code","✅ Hard + 🔗","Not null; must equal stored sampleNumber (e.g. 'MY26AAA2007')","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.3 Per-transaction Identity","reference_id (Cancelled)","✅ Hard","Not null; not empty UUID. NOT compared to reference_code","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.3 Per-transaction Identity","reference_id (Success)","ℹ️ Info","May be null/empty — info-logged only","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.3 Per-transaction Identity","order_id","✅ Hard","Not null","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.3 Per-transaction Identity","mobile","✅ Hard + 🔗","Equals RequestContext.getMobile()","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.3 Per-transaction Identity","customer_id","✅ Hard + 🔗","Equals membership customer_id from step [21]","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.4 State Fields","status","✅ Hard","One of: Success / Successful / Cancelled","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.4 State Fields","is_reverted","✅ Hard","Cancelled → true; Success/Successful → false","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.5 Amount Fields","trnsc_amount","✅ Hard",">= 0 (parsed from string). PRICE FORMULA (info, +-1.0): actual_price - membership_discount - coupon_discount ~= trnsc_amount. SUCCESS: trnsc_amount ~= getCurrentTotalPrice() (+-1.0, info). CANCELLED: same formula for reversal.","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.5 Amount Fields","actual_price","✅ Hard",">= 0","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.5 Amount Fields","net_paid_amount","✅ Hard",">= 0","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.5 Amount Fields","membership_discount","✅ Hard",">= 0","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.5 Amount Fields","coupon_discount","✅ Hard + ⚠️ Soft",">= 0. CROSS-API SOFT (+-1.0): coupon_discount ~= RequestContext.getCouponAmount() from [08]. Note: transaction API stores 0 for Success records by design -- coupon confirmed via order/payment records.","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.5 Amount Fields","rewards_gain","✅ Hard",">= 0","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.5 Amount Fields","rewards_used","✅ Hard",">= 0","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.6 Percentage Fields","membership_discount_percentage","✅ Hard","Not null","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.6 Percentage Fields","rewards_discount_percentage","✅ Hard","Not null","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.7 Patient & Timestamps","patient_first_name","✅ Hard","Not null","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.7 Patient & Timestamps","dob","✅ Hard","Not null","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.7 Patient & Timestamps","gender","✅ Hard","Not null","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.7 Patient & Timestamps","created_at","✅ Hard","Not null","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.7 Patient & Timestamps","updated_at","✅ Hard","Not null","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.8 Success Record","refund_amount (when present)","✅ Hard","EXACT VALUE (hard): refund_amount = 0 for Success/Successful records. No refund existed at order placement. Hard assert: verifyTrue(dRefund == 0.0).","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.8 Success Record","rewards_used","✅ Hard",">= 0","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.8 Success Record","net_paid_amount","ℹ️ Info","API returns 0 by design for Success records — logged","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.8 Success Record","trnsc_amount vs getCurrentTotalPrice()","ℹ️ Info","CROSS-API (info, +-1.0): Success.trnsc_amount ~= RequestContext.getCurrentTotalPrice() from [08]. Also: membership_discount = actual_price - trnsc_amount (info check).","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.9 Cancelled Record","trnsc_amount vs getCurrentTotalPrice()","ℹ️ Info","CROSS-API (info, +-1.0): Cancelled.trnsc_amount ~= RequestContext.getCurrentTotalPrice() from [08]. Represents the refunded cash amount.","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.9 Cancelled Record","coupon_discount vs storedCouponAmount","ℹ️ Info","Should mirror Success record coupon — logged (±1.0)","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.9 Cancelled Record","refund_amount vs trnsc_amount","ℹ️ Info","FORMULA (info, +-1.0): Cancelled.refund_amount ~= Cancelled.trnsc_amount (full cash refund). Also: actual_price - membership_discount - coupon_discount ~= trnsc_amount (+-1.0 info).","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.9 Cancelled Record","rewards_gain vs getRewardsGain()","ℹ️ Info","CROSS-API (info, +-1.0): Cancelled.rewards_gain ~= RequestContext.getRewardsGain() from [13]. Confirms rewards reversal matches the original earn amount.","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
("[22]","Verify Transactions — step20_G_VerifyTransactionByMobile","22.9 Cancelled Record","net_paid_amount (refund back) vs storedCartTotal","ℹ️ Info","CROSS-API (info, +-1.0): Cancelled.net_paid_amount ~= RequestContext.getCurrentTotalPrice() from [08]. Logged for refund tracking.","GET /transaction/getTransactionByMobile/{mobile}","order/COD_20_CancellationRefundTest.java"),
]

# ─── assertion colour lookup ─────────────────────────────────────────────────
def row_fill(assertion_text):
    t = assertion_text.strip()
    if "Hard" in t and "Soft" not in t:
        return HARD_FILL
    if "Soft" in t and "Hard" not in t:
        return SOFT_FILL
    if "Info" in t:
        return INFO_FILL
    if "Cross-API" in t:
        return CROSS_FILL
    if "Hard" in t:          # mixed Hard + Soft  or  Hard + Cross
        return HARD_FILL
    return None

# ─── build workbook ──────────────────────────────────────────────────────────
wb = openpyxl.Workbook()
ws = wb.active
ws.title = "COD Full Flow Validations"

COLUMNS = [
    ("Step #",       12),
    ("Step Name",    46),
    ("Sub-Section",  32),
    ("Field / Check",55),
    ("Assertion",    28),
    ("Rule / Semantic / Description", 80),
    ("API Endpoint", 50),
    ("Source File",  42),
]

# ── header row ───────────────────────────────────────────────────────────────
header_font = Font(name="Calibri", bold=True, color="FFFFFF", size=11)
for col_idx, (label, width) in enumerate(COLUMNS, start=1):
    cell = ws.cell(row=1, column=col_idx, value=label)
    cell.fill = HEADER_FILL
    cell.font = header_font
    cell.alignment = Alignment(horizontal="center", vertical="center", wrap_text=True)
    cell.border = thin_border()
    ws.column_dimensions[get_column_letter(col_idx)].width = width

ws.row_dimensions[1].height = 30
ws.freeze_panes = "A2"

# ── data rows ────────────────────────────────────────────────────────────────
normal_font = Font(name="Calibri", size=10)
step_font   = Font(name="Calibri", size=10, bold=True)

prev_step = None
data_row = 2
for record in ROWS:
    step_num, step_name, subsec, field, assertion, rule, endpoint, source = record

    # choose fill based on assertion type; alternate light stripe for readability
    fill = row_fill(assertion)
    if fill is None:
        fill = ALT_ROW_FILL if (data_row % 2 == 0) else None

    is_new_step = (step_num != prev_step)

    for col_idx, value in enumerate(record, start=1):
        cell = ws.cell(row=data_row, column=col_idx, value=value)
        cell.border = thin_border()
        cell.alignment = Alignment(vertical="top", wrap_text=True, horizontal="left")

        if col_idx == 1:                  # Step # column — bold + blue when new step
            if is_new_step:
                cell.fill = STEP_FILL
                cell.font = Font(name="Calibri", size=10, bold=True, color="FFFFFF")
                cell.alignment = Alignment(horizontal="center", vertical="center", wrap_text=False)
            else:
                cell.fill = PatternFill("solid", fgColor="E8F1FB")
                cell.font = normal_font
                cell.alignment = Alignment(horizontal="center", vertical="top")
        elif col_idx == 3:                # Sub-Section — slight highlight
            cell.fill = SUBSEC_FILL
            cell.font = Font(name="Calibri", size=10, italic=True)
        elif col_idx == 5:                # Assertion column — colour-coded
            if fill:
                cell.fill = fill
            cell.font = Font(name="Calibri", size=10, bold=True)
            cell.alignment = Alignment(horizontal="center", vertical="top", wrap_text=True)
        else:
            if fill:
                cell.fill = fill
            cell.font = normal_font

    ws.row_dimensions[data_row].height = 15 if len(field) < 40 else 28
    prev_step = step_num
    data_row += 1

# ── legend sheet ─────────────────────────────────────────────────────────────
ls = wb.create_sheet("Legend")
legend_data = [
    ("Colour / Symbol", "Meaning"),
    ("✅ Hard  (green rows)", "AssertionUtil.verifyEquals / verifyNotNull / verifyTrue — test FAILS immediately if wrong"),
    ("⚠️ Soft  (yellow rows)", "logSoft(…) — writes [SOFT] to logs/cod_failures.log; test continues"),
    ("ℹ️ Info  (grey rows)", "System.out.println(…) — logged for debugging; no assertion"),
    ("🔗 Cross-API  (orange rows)", "Value compared against a field stored in RequestContext from a prior step"),
    ("Blue Step # cells", "First row of a new step [01]–[22]"),
    ("Sub-Section (italic blue)", "Logical grouping within a step (e.g. '19.3 Amount Fields')"),
]
ls.column_dimensions["A"].width = 30
ls.column_dimensions["B"].width = 90
lh_font = Font(name="Calibri", bold=True, size=11, color="FFFFFF")
for r_idx, (col_a, col_b) in enumerate(legend_data, start=1):
    ca = ls.cell(row=r_idx, column=1, value=col_a)
    cb = ls.cell(row=r_idx, column=2, value=col_b)
    for c in (ca, cb):
        c.border = thin_border()
        c.alignment = Alignment(vertical="center", wrap_text=True)
        ls.row_dimensions[r_idx].height = 22
    if r_idx == 1:
        ca.fill = HEADER_FILL; ca.font = lh_font
        cb.fill = HEADER_FILL; cb.font = lh_font
    else:
        fills = [None, HARD_FILL, SOFT_FILL, INFO_FILL, CROSS_FILL,
                 STEP_FILL, SUBSEC_FILL]
        f = fills[r_idx - 1] if r_idx - 1 < len(fills) else None
        if f:
            ca.fill = f; cb.fill = f
        fonts_map = {STEP_FILL: Font(name="Calibri", size=10, bold=True, color="FFFFFF")}
        ca.font = fonts_map.get(f, Font(name="Calibri", size=10))
        cb.font = fonts_map.get(f, Font(name="Calibri", size=10))

# ── auto-filter on main sheet ─────────────────────────────────────────────────
ws.auto_filter.ref = f"A1:{get_column_letter(len(COLUMNS))}{data_row - 1}"

# ── save ──────────────────────────────────────────────────────────────────────
OUTPUT = r"c:\Users\RANJITH\MrYoda\docs\Full_COD_Flow_Validation.xlsx"
wb.save(OUTPUT)
print(f"✅ Excel saved → {OUTPUT}")
print(f"   Main sheet rows: {data_row - 1}  (excluding header)")
