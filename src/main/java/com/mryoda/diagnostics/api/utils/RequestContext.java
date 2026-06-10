package com.mryoda.diagnostics.api.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class RequestContext {

    // ============================================================
    // TOKENS (MEMBER / EXISTING MEMBER / NEW USER)
    // ============================================================
    private static String memberToken;
    private static String existingMemberToken;
    private static String newUserToken;
    private static String currentFlowName = "default";
    private static List<Map<String, Object>> activeProductDetails;

    private static List<Map<String, String>> expectedPatientDetails = new ArrayList<>();
    private static String expectedAddressName;
    private static String expectedSlotTimeString;
    private static String expectedSlotDate;
    private static String memberCouponGuid;
    private static String nonMemberCouponGuid;
    private static String newUserCouponGuid;
    private static boolean memberCouponFlowEnabled;
    private static boolean nonMemberCouponFlowEnabled;
    private static boolean newUserCouponFlowEnabled;

    public static void clearFlowState() {
        System.out.println("\n[CLEANING] RequestContext Flow State...");
        visitNumber = null;
        currentOrderId = null;
        currentPaymentId = null;
        currentOrderTrackingId = null;
        currentPhleboGuid = null;
        currentSlotGuid = null;
        currentCartId = null;
        currentAddressGuid = null;
        currentAddressId = null;

        if (currentOrderIds != null)
            currentOrderIds.clear();
        if (currentOrderTrackingIds != null)
            currentOrderTrackingIds.clear();
        if (currentVisitNumbers != null)
            currentVisitNumbers.clear();
        if (orderVisitMap != null)
            orderVisitMap.clear();
        if (visitSinMap != null)
            visitSinMap.clear();
        if (cancelledOrderIds != null)
            cancelledOrderIds.clear();
        if (cancelledVisitNumbers != null)
            cancelledVisitNumbers.clear();

        currentTotalPrice = 0;
        currentSampleType = null;
        activeProductDetails = null;
        if (expectedPatientDetails != null)
            expectedPatientDetails.clear();
        expectedAddressName = null;
        expectedSlotTimeString = null;
        expectedSlotDate = null;

        clearAllTests();
        clearExpectedTestResults();
        couponAmount = 0.0;
        memberCouponGuid = null;
        nonMemberCouponGuid = null;
        newUserCouponGuid = null;
        memberCouponFlowEnabled = false;
        nonMemberCouponFlowEnabled = false;
        newUserCouponFlowEnabled = false;
        cancelledOrderRewardsGain.remove();
        visitsProcessedByUI = false;

        System.out.println("[SUCCESS] RequestContext State Successfully Cleared.");
    }

    public static void storeExpectedPatient(String guid, String name) {
        Map<String, String> patient = new HashMap<>();
        patient.put("guid", guid);
        patient.put("name", name);
        expectedPatientDetails.add(patient);
    }

    public static List<Map<String, String>> getExpectedPatientDetails() {
        return expectedPatientDetails;
    }

    public static void setExpectedAddressName(String name) {
        expectedAddressName = name;
    }

    public static String getExpectedAddressName() {
        return expectedAddressName;
    }

    public static void setExpectedSlotTiming(String date, String time) {
        expectedSlotDate = date;
        expectedSlotTimeString = time;
    }

    public static String getExpectedSlotTimeString() {
        return expectedSlotTimeString;
    }

    public static String getExpectedSlotDate() {
        return expectedSlotDate;
    }

    public static void setActiveProductDetails(List<Map<String, Object>> details) {
        activeProductDetails = details;
    }

    public static List<Map<String, Object>> getActiveProductDetails() {
        return activeProductDetails;
    }

    public static void setCurrentFlowName(String name) {
        currentFlowName = name;
    }

    public static String getCurrentFlowName() {
        return currentFlowName;
    }

    private static String visitNumber;
    private static String currentOrderId;
    private static String currentOrderSampleNumber;  // e.g. "MY26AAA1888" — reference_code in transaction
    private static String currentMembershipCustomerId; // membership system's customer_id (different from diagnostics userId)
    private static String currentPaymentId;
    private static String currentOrderTrackingId;
    private static String currentPhleboGuid;
    private static String currentSlotGuid;
    private static String currentCartId;
    private static String currentAddressGuid;
    private static String currentAddressId;
    private static List<String> currentOrderIds = new ArrayList<>();
    private static List<String> currentOrderTrackingIds = new ArrayList<>();
    private static List<String> currentVisitNumbers = new ArrayList<>();
    private static Map<String, String> orderVisitMap = new HashMap<>();
    private static Map<String, String> visitSinMap = new HashMap<>();
    private static Set<String> cancelledOrderIds = new HashSet<>();
    private static Set<String> cancelledVisitNumbers = new HashSet<>();
    private static volatile boolean visitsProcessedByUI = false;
    private static int currentTotalPrice;
    private static String currentSampleType;

    public static void setVisitNumber(String v) {
        visitNumber = v;
        if (v != null && !v.isEmpty() && !currentVisitNumbers.contains(v)) {
            currentVisitNumbers.add(v);
        }
        System.out.println(">>> STORED VISIT NUMBER: " + visitNumber);
    }

    public static String getVisitNumber() {
        return visitNumber;
    }

    public static void setCurrentVisitNumbers(List<String> visits) {
        // Always store a mutable copy to prevent UnsupportedOperationException
        // when callers pass Collections.singletonList() or other immutable lists.
        currentVisitNumbers = (visits != null) ? new ArrayList<>(visits) : new ArrayList<>();
        if (!currentVisitNumbers.isEmpty()) {
            visitNumber = currentVisitNumbers.get(0);
        }
    }

    public static List<String> getCurrentVisitNumbers() {
        return currentVisitNumbers;
    }

    public static void mapOrderToVisit(String orderId, String visitNo) {
        if (orderId != null && visitNo != null) {
            orderVisitMap.put(orderId, visitNo);
            if (!currentVisitNumbers.contains(visitNo)) {
                currentVisitNumbers.add(visitNo);
            }
            if (visitNumber == null) {
                visitNumber = visitNo;
            }
        }
    }

    public static String getVisitForOrder(String orderId) {
        return orderVisitMap.get(orderId);
    }

    public static Map<String, String> getOrderVisitMap() {
        return orderVisitMap;
    }

    public static void addCancelledOrderId(String orderId) {
        if (orderId != null && !orderId.isEmpty()) {
            cancelledOrderIds.add(orderId);
            System.out.println(">>> MARKED ORDER AS CANCELLED (excluded from UI trigger): " + orderId);
        }
    }

    public static Set<String> getCancelledOrderIds() {
        return cancelledOrderIds;
    }

    public static void addCancelledVisitNumber(String visitNo) {
        if (visitNo != null && !visitNo.isEmpty()) {
            cancelledVisitNumbers.add(visitNo);
            System.out.println(">>> MARKED VISIT AS CANCELLED (excluded from UI trigger): " + visitNo);
        }
    }

    public static Set<String> getCancelledVisitNumbers() {
        return cancelledVisitNumbers;
    }

    public static void setSinNumberForVisit(String visitNo, String sinNo) {
        if (visitNo != null && sinNo != null) {
            visitSinMap.put(visitNo, sinNo);
            System.out.println(">>> MAPPED Visit: " + visitNo + " → SIN: " + sinNo);
        }
    }

    public static String getSinNumberForVisit(String visitNo) {
        return visitSinMap.get(visitNo);
    }

    public static void setCurrentOrderId(String v) {
        currentOrderId = v;
    }

    public static String getCurrentOrderId() {
        return currentOrderId;
    }

    public static void setCurrentOrderSampleNumber(String v) {
        currentOrderSampleNumber = v;
    }

    public static String getCurrentOrderSampleNumber() {
        return currentOrderSampleNumber;
    }

    public static void setCurrentMembershipCustomerId(String v) {
        currentMembershipCustomerId = v;
    }

    public static String getCurrentMembershipCustomerId() {
        return currentMembershipCustomerId;
    }

    public static void setCurrentOrderIds(List<String> ids) {
        currentOrderIds = (ids != null) ? new ArrayList<>(ids) : new ArrayList<>();
        if (ids != null && !ids.isEmpty()) {
            currentOrderId = ids.get(0); // Fallback for single ID getters
        }
    }

    public static List<String> getCurrentOrderIds() {
        return currentOrderIds;
    }

    public static void setCurrentPaymentId(String v) {
        currentPaymentId = v;
    }

    public static String getCurrentPaymentId() {
        return currentPaymentId;
    }

    public static void setCurrentOrderTrackingId(String v) {
        currentOrderTrackingId = v;
    }

    public static String getCurrentOrderTrackingId() {
        return currentOrderTrackingId;
    }

    public static void setCurrentOrderTrackingIds(List<String> ids) {
        currentOrderTrackingIds = (ids != null) ? new ArrayList<>(ids) : new ArrayList<>();
        if (ids != null && !ids.isEmpty()) {
            currentOrderTrackingId = ids.get(0); // Fallback for single ID getters
        }
    }

    public static List<String> getCurrentOrderTrackingIds() {
        return currentOrderTrackingIds;
    }

    public static void setCurrentPhleboGuid(String v) {
        currentPhleboGuid = v;
    }

    public static String getCurrentPhleboGuid() {
        return currentPhleboGuid;
    }

    private static String phleboToken;

    public static void setPhleboToken(String token) {
        phleboToken = token;
    }

    public static String getPhleboToken() {
        return phleboToken;
    }

    private static String adminToken;
    private static String adminGuid;

    public static void setAdminToken(String token) {
        adminToken = token;
    }

    public static String getAdminToken() {
        return adminToken;
    }

    public static void setAdminGuid(String guid) {
        adminGuid = guid;
    }

    public static String getAdminGuid() {
        return adminGuid;
    }

    public static void setCurrentSlotGuid(String v) {
        currentSlotGuid = v;
    }

    public static String getCurrentSlotGuid() {
        return currentSlotGuid;
    }

    public static void setCurrentCartId(String v) {
        currentCartId = v;
    }

    public static String getCurrentCartId() {
        return currentCartId;
    }

    public static void setCurrentAddressGuid(String v) {
        currentAddressGuid = v;
    }

    public static String getCurrentAddressGuid() {
        return currentAddressGuid;
    }

    public static void setCurrentAddressId(String v) {
        currentAddressId = v;
    }

    public static String getCurrentAddressId() {
        return currentAddressId;
    }

    public static void setCurrentTotalPrice(int v) {
        currentTotalPrice = v;
    }

    public static int getCurrentTotalPrice() {
        return currentTotalPrice;
    }

    public static void setCurrentSampleType(String v) {
        currentSampleType = v;
    }

    public static String getCurrentSampleType() {
        return currentSampleType;
    }

    // ============================================================
    // USER DETAILS (Stored per user type)
    // ============================================================
    private static String memberFirstName, memberLastName, memberUserId;
    private static String existingMemberFirstName, existingMemberLastName, existingMemberUserId;
    private static String newUserFirstName, newUserLastName, newUserUserId;

    // Generic (used by TokenManager)
    private static String token;
    private static String firstName;
    private static String lastName;
    private static String userId;
    private static String mobile;

    // ============================================================
    // LOCATION STORAGE
    // ============================================================
    private static final Map<String, String> locations = new HashMap<>();
    private static final Map<String, String> locationLatitudes = new HashMap<>();
    private static final Map<String, String> locationLongitudes = new HashMap<>();
    private static final Map<String, String> locationCities = new HashMap<>();
    private static final Map<String, String> locationStates = new HashMap<>();
    private static String selectedLocationId;

    public static void storeLocation(String title, String id) {
        locations.put(title, id);
    }

    public static void storeLocationCoordinates(String title, String latitude, String longitude) {
        locationLatitudes.put(title, latitude);
        locationLongitudes.put(title, longitude);
    }

    public static void storeLocationCityState(String title, String city, String state) {
        locationCities.put(title, city);
        locationStates.put(title, state);
    }

    public static String getLocationId(String title) {
        return locations.get(title);
    }

    public static String getLocationLatitude(String title) {
        return locationLatitudes.get(title);
    }

    public static String getLocationLongitude(String title) {
        return locationLongitudes.get(title);
    }

    public static String getLocationCity(String title) {
        return locationCities.get(title);
    }

    public static String getLocationState(String title) {
        return locationStates.get(title);
    }

    public static Map<String, String> getAllLocations() {
        return locations;
    }

    public static Map<String, String> getAllLocationLatitudes() {
        return locationLatitudes;
    }

    public static Map<String, String> getAllLocationLongitudes() {
        return locationLongitudes;
    }

    public static void setSelectedLocation(String title) {
        String id = locations.get(title);
        if (id == null) {
            // Case-insensitive fallback — handles mismatches like "ameerpet (hq)" vs "Ameerpet (HQ)"
            for (Map.Entry<String, String> entry : locations.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(title)) {
                    id = entry.getValue();
                    System.out.println("   ℹ️ setSelectedLocation: matched '" + title + "' → stored key '" + entry.getKey() + "'");
                    break;
                }
            }
        }
        if (id == null) {
            throw new RuntimeException("❌ Location not found in RequestContext: " + title
                    + ". Available locations: " + locations.keySet());
        }
        selectedLocationId = id;
    }

    public static void setSelectedLocationId(String id) {
        selectedLocationId = id;
    }

    public static String getSelectedLocationId() {
        return selectedLocationId;
    }

    // ============================================================
    // BRAND STORAGE
    // ============================================================
    private static final Map<String, String> brands = new HashMap<>();
    private static String selectedBrandId;

    public static void storeBrand(String title, String brandId) {
        brands.put(title, brandId);
    }

    public static String getBrandId(String title) {
        return brands.get(title);
    }

    public static Map<String, String> getAllBrands() {
        return brands;
    }

    public static void setSelectedBrand(String title) {
        String id = brands.get(title);
        if (id == null) {
            throw new RuntimeException("❌ Brand not found in RequestContext: " + title);
        }
        selectedBrandId = id;
    }

    public static String getSelectedBrandId() {
        return selectedBrandId;
    }

    // ============================================================
    // GLOBAL SEARCH TEST STORAGE
    // ============================================================
    /**
     * Each testName → details map details = { id, test_id, price, original_price,
     * discount_percentage, type }
     */
    private static final Map<String, Map<String, Object>> storedTests = new HashMap<>();

    public static void storeTestDetails(String testName, String id, String testId, int price, int originalPrice,
            int discountPercentage, String type) {

        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("test_id", testId);
        data.put("price", price);
        data.put("original_price", originalPrice);
        data.put("discount_percentage", discountPercentage);
        data.put("type", type);

        storedTests.put(testName, data);
    }

    public static Map<String, Object> getTestDetails(String testName) {
        return storedTests.get(testName);
    }

    public static Map<String, Map<String, Object>> getAllStoredTests() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : storedTests.entrySet()) {
            String name = entry.getKey();
            Map<String, Object> details = new HashMap<>(entry.getValue()); // Return a copy
            
            // Logic to handle packages that might have been stored as 'test' type
            String type = (details.get("type") != null) ? details.get("type").toString().toLowerCase() : "test";
            if (type.equals("test") && (name.contains("Panel") || name.contains("Profile") || name.contains("Package"))) {
                details.put("type", "package");
            }
            result.put(name, details);
        }
        return result;
    }

    // ============================================================
    // SETTERS
    // ============================================================
    public static void setMemberToken(String v) {
        memberToken = v;
    }

    public static void setMemberFirstName(String v) {
        memberFirstName = v;
    }

    public static void setMemberLastName(String v) {
        memberLastName = v;
    }

    public static void setMemberUserId(String v) {
        memberUserId = v;
    }

    // NON-MEMBER setters (Mobile: 8220220227 - NOT a paid member)
    public static void setNonMemberToken(String v) {
        existingMemberToken = v;
    }

    public static void setNonMemberFirstName(String v) {
        existingMemberFirstName = v;
    }

    public static void setNonMemberLastName(String v) {
        existingMemberLastName = v;
    }

    public static void setNonMemberUserId(String v) {
        existingMemberUserId = v;
    }

    // Deprecated - use NON_MEMBER methods instead
    @Deprecated
    public static void setExistingMemberToken(String v) {
        setNonMemberToken(v);
    }

    @Deprecated
    public static void setExistingMemberFirstName(String v) {
        setNonMemberFirstName(v);
    }

    @Deprecated
    public static void setExistingMemberLastName(String v) {
        setNonMemberLastName(v);
    }

    @Deprecated
    public static void setExistingMemberUserId(String v) {
        setNonMemberUserId(v);
    }

    public static void setNewUserToken(String v) {
        newUserToken = v;
    }

    public static void setNewUserFirstName(String v) {
        newUserFirstName = v;
    }

    public static void setNewUserLastName(String v) {
        newUserLastName = v;
    }

    public static void setNewUserUserId(String v) {
        newUserUserId = v;
    }

    public static void setMobile(String v) {
        mobile = v;
    }

    public static void setToken(String v) {
        token = v;
    }

    public static void setFirstName(String v) {
        firstName = v;
    }

    public static void setLastName(String v) {
        lastName = v;
    }

    public static void setUserId(String v) {
        userId = v;
    }

    // ============================================================
    // GETTERS
    // ============================================================
    public static String getMemberToken() {
        return memberToken;
    }

    public static String getMemberFirstName() {
        return memberFirstName;
    }

    public static String getMemberLastName() {
        return memberLastName;
    }

    public static String getMemberUserId() {
        return memberUserId;
    }

    // NON-MEMBER getters (Mobile: 8220220227 - NOT a paid member)
    public static String getNonMemberToken() {
        return existingMemberToken;
    }

    public static String getNonMemberFirstName() {
        return existingMemberFirstName;
    }

    public static String getNonMemberLastName() {
        return existingMemberLastName;
    }

    public static String getNonMemberUserId() {
        return existingMemberUserId;
    }

    // Deprecated - use NON_MEMBER methods instead
    @Deprecated
    public static String getExistingMemberToken() {
        return getNonMemberToken();
    }

    @Deprecated
    public static String getExistingMemberFirstName() {
        return getNonMemberFirstName();
    }

    @Deprecated
    public static String getExistingMemberLastName() {
        return getNonMemberLastName();
    }

    @Deprecated
    public static String getExistingMemberUserId() {
        return getNonMemberUserId();
    }

    public static String getNewUserToken() {
        return newUserToken;
    }

    public static String getNewUserFirstName() {
        return newUserFirstName;
    }

    public static String getNewUserLastName() {
        return newUserLastName;
    }

    public static String getNewUserUserId() {
        return newUserUserId;
    }

    public static String getMobile() {
        return mobile;
    }

    public static String getToken() {
        return token;
    }

    public static String getFirstName() {
        return firstName;
    }

    public static String getLastName() {
        return lastName;
    }

    public static String getUserId() {
        return userId;
    }

    // ----------------------------------------------------
    // GLOBAL SEARCH TEST STORAGE
    // ----------------------------------------------------
    private static List<Map<String, Object>> globalTests = new ArrayList<>();

    public static void storeGlobalTests(List<Map<String, Object>> tests) {
        globalTests.clear();
        globalTests.addAll(tests);
    }

    public static List<Map<String, Object>> getGlobalTests() {
        return globalTests;
    }

    // Stores selected tests from global search
    private static final Map<String, Map<String, Object>> selectedTests = new HashMap<>();

    // ----------------------------------------------------
    // SELECTED TEST STORAGE
    // ----------------------------------------------------
    public static void storeTest(String testName, Map<String, Object> testData) {
        selectedTests.put(testName, testData);
    }

    public static Map<String, Object> getTest(String testName) {
        return selectedTests.get(testName);
    }

    public static Map<String, Map<String, Object>> getAllTests() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : selectedTests.entrySet()) {
            String name = entry.getKey();
            Map<String, Object> details = new HashMap<>(entry.getValue());
            
            // Auto-detect packages by name patterns
            String type = (details.get("type") != null) ? details.get("type").toString().toLowerCase() : "test";
            if (type.equals("test") && (name.contains("Panel") || name.contains("Profile") || name.contains("Package"))) {
                details.put("type", "package");
            }
            result.put(name, details);
        }
        return result;
    }

    public static void clearAllTests() {
        selectedTests.clear();
    }

    // ============================================================
    // CART STORAGE (Separate for each user type)
    // ============================================================
    private static String memberCartId;
    private static Integer memberCartNumericId;
    private static Integer memberTotalAmount;
    private static Map<String, Object> memberAddToCartResponse;
    private static Map<String, Object> memberGetCartResponse;
    private static List<Map<String, Object>> memberCartItems;

    private static String existingMemberCartId;
    private static Integer existingMemberCartNumericId;
    private static Integer existingMemberTotalAmount;
    private static Map<String, Object> existingMemberAddToCartResponse;
    private static Map<String, Object> existingMemberGetCartResponse;
    private static List<Map<String, Object>> existingMemberCartItems;

    private static String newUserCartId;
    private static Integer newUserCartNumericId;
    private static Integer newUserTotalAmount;
    private static Map<String, Object> newUserAddToCartResponse;
    private static Map<String, Object> newUserGetCartResponse;
    private static List<Map<String, Object>> newUserCartItems;

    // Cart setters for Member
    public static void setMemberCartId(String id) {
        memberCartId = id;
    }

    public static void setMemberCartNumericId(Integer id) {
        memberCartNumericId = id;
    }

    public static void setMemberTotalAmount(Integer amount) {
        memberTotalAmount = amount;
    }

    public static void setMemberAddToCartResponse(Map<String, Object> response) {
        memberAddToCartResponse = response;
    }

    public static void setMemberGetCartResponse(Map<String, Object> response) {
        memberGetCartResponse = response;
    }

    public static void setMemberCartItems(List<Map<String, Object>> items) {
        memberCartItems = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
    }

    // Cart setters for NON-MEMBER (Mobile: 8220220227)
    public static void setNonMemberCartId(String id) {
        existingMemberCartId = id;
    }

    public static void setNonMemberCartNumericId(Integer id) {
        existingMemberCartNumericId = id;
    }

    public static void setNonMemberTotalAmount(Integer amount) {
        existingMemberTotalAmount = amount;
    }

    public static void setNonMemberAddToCartResponse(Map<String, Object> response) {
        existingMemberAddToCartResponse = response;
    }

    public static void setNonMemberGetCartResponse(Map<String, Object> response) {
        existingMemberGetCartResponse = response;
    }

    public static void setNonMemberCartItems(List<Map<String, Object>> items) {
        existingMemberCartItems = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
    }

    // Cart setters for Existing Member (Deprecated - use NON_MEMBER)
    @Deprecated
    public static void setExistingMemberCartId(String id) {
        setNonMemberCartId(id);
    }

    @Deprecated
    public static void setExistingMemberCartNumericId(Integer id) {
        setNonMemberCartNumericId(id);
    }

    @Deprecated
    public static void setExistingMemberTotalAmount(Integer amount) {
        setNonMemberTotalAmount(amount);
    }

    @Deprecated
    public static void setExistingMemberAddToCartResponse(Map<String, Object> response) {
        setNonMemberAddToCartResponse(response);
    }

    @Deprecated
    public static void setExistingMemberGetCartResponse(Map<String, Object> response) {
        setNonMemberGetCartResponse(response);
    }

    @Deprecated
    public static void setExistingMemberCartItems(List<Map<String, Object>> items) {
        setNonMemberCartItems(items);
    }

    // Cart setters for New User
    public static void setNewUserCartId(String id) {
        newUserCartId = id;
    }

    public static void setNewUserCartNumericId(Integer id) {
        newUserCartNumericId = id;
    }

    public static void setNewUserTotalAmount(Integer amount) {
        newUserTotalAmount = amount;
    }

    public static void setNewUserAddToCartResponse(Map<String, Object> response) {
        newUserAddToCartResponse = response;
    }

    public static void setNewUserGetCartResponse(Map<String, Object> response) {
        newUserGetCartResponse = response;
    }

    public static void setNewUserCartItems(List<Map<String, Object>> items) {
        newUserCartItems = (items != null) ? new ArrayList<>(items) : new ArrayList<>();
    }

    // Cart getters for Member
    public static String getMemberCartId() {
        return memberCartId;
    }

    public static Integer getMemberCartNumericId() {
        return memberCartNumericId;
    }

    public static Integer getMemberTotalAmount() {
        return memberTotalAmount;
    }

    public static Map<String, Object> getMemberAddToCartResponse() {
        return memberAddToCartResponse;
    }

    public static Map<String, Object> getMemberGetCartResponse() {
        return memberGetCartResponse;
    }

    public static List<Map<String, Object>> getMemberCartItems() {
        return memberCartItems;
    }

    // Cart getters for NON-MEMBER (Mobile: 8220220227)
    public static String getNonMemberCartId() {
        return existingMemberCartId;
    }

    public static Integer getNonMemberCartNumericId() {
        return existingMemberCartNumericId;
    }

    public static Integer getNonMemberTotalAmount() {
        return existingMemberTotalAmount;
    }

    public static Map<String, Object> getNonMemberAddToCartResponse() {
        return existingMemberAddToCartResponse;
    }

    public static Map<String, Object> getNonMemberGetCartResponse() {
        return existingMemberGetCartResponse;
    }

    public static List<Map<String, Object>> getNonMemberCartItems() {
        return existingMemberCartItems;
    }

    // Cart getters for Existing Member (Deprecated - use NON_MEMBER)
    @Deprecated
    public static String getExistingMemberCartId() {
        return getNonMemberCartId();
    }

    @Deprecated
    public static Integer getExistingMemberCartNumericId() {
        return getNonMemberCartNumericId();
    }

    @Deprecated
    public static Integer getExistingMemberTotalAmount() {
        return getNonMemberTotalAmount();
    }

    @Deprecated
    public static Map<String, Object> getExistingMemberAddToCartResponse() {
        return getNonMemberAddToCartResponse();
    }

    @Deprecated
    public static Map<String, Object> getExistingMemberGetCartResponse() {
        return getNonMemberGetCartResponse();
    }

    @Deprecated
    public static List<Map<String, Object>> getExistingMemberCartItems() {
        return getNonMemberCartItems();
    }

    // Cart getters for New User
    public static String getNewUserCartId() {
        return newUserCartId;
    }

    public static Integer getNewUserCartNumericId() {
        return newUserCartNumericId;
    }

    public static Integer getNewUserTotalAmount() {
        return newUserTotalAmount;
    }

    public static Map<String, Object> getNewUserAddToCartResponse() {
        return newUserAddToCartResponse;
    }

    public static Map<String, Object> getNewUserGetCartResponse() {
        return newUserGetCartResponse;
    }

    public static List<Map<String, Object>> getNewUserCartItems() {
        return newUserCartItems;
    }

    // ============================================================
    // COUPON STORAGE (Separate for each user type)
    // ============================================================
    public static void setMemberCouponGuid(String couponGuid) {
        memberCouponGuid = couponGuid;
    }

    public static String getMemberCouponGuid() {
        return memberCouponGuid;
    }

    public static void setMemberCouponFlowEnabled(boolean enabled) {
        memberCouponFlowEnabled = enabled;
    }

    public static boolean isMemberCouponFlowEnabled() {
        return memberCouponFlowEnabled;
    }

    public static void setNonMemberCouponGuid(String couponGuid) {
        nonMemberCouponGuid = couponGuid;
    }

    public static String getNonMemberCouponGuid() {
        return nonMemberCouponGuid;
    }

    public static void setNonMemberCouponFlowEnabled(boolean enabled) {
        nonMemberCouponFlowEnabled = enabled;
    }

    public static boolean isNonMemberCouponFlowEnabled() {
        return nonMemberCouponFlowEnabled;
    }

    public static void setNewUserCouponGuid(String couponGuid) {
        newUserCouponGuid = couponGuid;
    }

    public static String getNewUserCouponGuid() {
        return newUserCouponGuid;
    }

    public static void setNewUserCouponFlowEnabled(boolean enabled) {
        newUserCouponFlowEnabled = enabled;
    }

    public static boolean isNewUserCouponFlowEnabled() {
        return newUserCouponFlowEnabled;
    }

    // Legacy methods for backward compatibility
    public static void storeCartId(String id) {
        memberCartId = id;
    }

    public static String getCartId() {
        return memberCartId;
    }

    // ============================================================
    // ADDRESS STORAGE (Separate for each user type)
    // ============================================================
    private static String memberAddressId;
    private static List<Map<String, Object>> memberAddresses;

    private static String existingMemberAddressId;
    private static List<Map<String, Object>> existingMemberAddresses;

    private static String newUserAddressId;
    private static List<Map<String, Object>> newUserAddresses;

    // Address setters for Member
    public static void setMemberAddressId(String id) {
        memberAddressId = id;
    }

    public static void setMemberAddresses(List<Map<String, Object>> addresses) {
        memberAddresses = (addresses != null) ? new ArrayList<>(addresses) : new ArrayList<>();
    }

    // Address setters for NON-MEMBER (Mobile: 8220220227)
    public static void setNonMemberAddressId(String id) {
        existingMemberAddressId = id;
    }

    public static void setNonMemberAddresses(List<Map<String, Object>> addresses) {
        existingMemberAddresses = (addresses != null) ? new ArrayList<>(addresses) : new ArrayList<>();
    }

    // Address setters for Existing Member (Deprecated - use NON_MEMBER)
    @Deprecated
    public static void setExistingMemberAddressId(String id) {
        setNonMemberAddressId(id);
    }

    @Deprecated
    public static void setExistingMemberAddresses(List<Map<String, Object>> addresses) {
        setNonMemberAddresses(addresses);
    }

    // Address setters for New User
    public static void setNewUserAddressId(String id) {
        newUserAddressId = id;
    }

    public static void setNewUserAddresses(List<Map<String, Object>> addresses) {
        newUserAddresses = (addresses != null) ? new ArrayList<>(addresses) : new ArrayList<>();
    }

    // Address getters for Member
    public static String getMemberAddressId() {
        return memberAddressId;
    }

    public static List<Map<String, Object>> getMemberAddresses() {
        return memberAddresses;
    }

    // Address getters for NON-MEMBER (Mobile: 8220220227)
    public static String getNonMemberAddressId() {
        return existingMemberAddressId;
    }

    public static List<Map<String, Object>> getNonMemberAddresses() {
        return existingMemberAddresses;
    }

    // Address getters for Existing Member (Deprecated - use NON_MEMBER)
    @Deprecated
    public static String getExistingMemberAddressId() {
        return getNonMemberAddressId();
    }

    @Deprecated
    public static List<Map<String, Object>> getExistingMemberAddresses() {
        return getNonMemberAddresses();
    }

    // Address getters for New User
    public static String getNewUserAddressId() {
        return newUserAddressId;
    }

    public static List<Map<String, Object>> getNewUserAddresses() {
        return newUserAddresses;
    }

    // ============================================================
    // SLOT STORAGE
    // ============================================================
    private static String slotStartDate;
    private static String existingMemberSlotGuid;
    private static String existingMemberSlotTime;
    private static String memberSlotGuid;
    private static String memberSlotTime;
    private static String newUserSlotGuid;
    private static String newUserSlotTime;

    // Slot setters
    public static void setSlotStartDate(String date) {
        slotStartDate = date;
    }

    public static void setNonMemberSlotGuid(String guid) {
        existingMemberSlotGuid = guid;
    }

    // Alias for compatibility
    public static void setNonMemberSlotId(String id) {
        setNonMemberSlotGuid(id);
    }

    public static void setNonMemberSlotTime(String time) {
        existingMemberSlotTime = time;
    }

    @Deprecated
    public static void setExistingMemberSlotGuid(String guid) {
        setNonMemberSlotGuid(guid);
    }

    public static void setMemberSlotGuid(String guid) {
        memberSlotGuid = guid;
    }

    public static void setMemberSlotId(String id) {
        setMemberSlotGuid(id);
    }

    public static void setMemberSlotTime(String time) {
        memberSlotTime = time;
    }

    public static void setNewUserSlotGuid(String guid) {
        newUserSlotGuid = guid;
    }

    public static void setNewUserSlotId(String id) {
        setNewUserSlotGuid(id);
    }

    public static void setNewUserSlotTime(String time) {
        newUserSlotTime = time;
    }

    // Slot getters
    public static String getSlotStartDate() {
        return slotStartDate;
    }

    public static String getNonMemberSlotGuid() {
        return existingMemberSlotGuid;
    }

    public static String getNonMemberSlotId() {
        return existingMemberSlotGuid;
    }

    public static String getNonMemberSlotTime() {
        return existingMemberSlotTime;
    }

    @Deprecated
    public static String getExistingMemberSlotGuid() {
        return getNonMemberSlotGuid();
    }

    public static String getMemberSlotGuid() {
        return memberSlotGuid;
    }

    public static String getMemberSlotId() {
        return memberSlotGuid;
    }

    public static String getMemberSlotTime() {
        return memberSlotTime;
    }

    public static String getNewUserSlotGuid() {
        return newUserSlotGuid;
    }

    public static String getNewUserSlotId() {
        return newUserSlotGuid;
    }

    public static String getNewUserSlotTime() {
        return newUserSlotTime;
    }

    // Generic getter for selected slot (used by all user types)
    public static String getSelectedSlotGuid() {
        // Return the first non-null slot GUID
        if (existingMemberSlotGuid != null)
            return existingMemberSlotGuid;
        if (memberSlotGuid != null)
            return memberSlotGuid;
        if (newUserSlotGuid != null)
            return newUserSlotGuid;
        return null;
    }

    // ============================================================
    // ORDER STORAGE (Separate for each user type)
    // ============================================================
    private static String memberOrderGuid;
    private static String memberOrderId;

    private static String existingMemberOrderGuid;
    private static String existingMemberOrderId;

    private static String newUserOrderGuid;
    private static String newUserOrderId;

    // Order setters for Member
    public static void setMemberOrderGuid(String guid) {
        memberOrderGuid = guid;
    }

    public static void setMemberOrderId(String id) {
        memberOrderId = id;
    }

    // Order setters for NON-MEMBER (Mobile: 8220220227)
    public static void setNonMemberOrderGuid(String guid) {
        existingMemberOrderGuid = guid;
    }

    public static void setNonMemberOrderId(String id) {
        existingMemberOrderId = id;
    }

    @Deprecated
    public static void setExistingMemberOrderGuid(String guid) {
        setNonMemberOrderGuid(guid);
    }

    @Deprecated
    public static void setExistingMemberOrderId(String id) {
        setNonMemberOrderId(id);
    }

    // Order setters for NEW USER
    public static void setNewUserOrderGuid(String guid) {
        newUserOrderGuid = guid;
    }

    public static void setNewUserOrderId(String id) {
        newUserOrderId = id;
    }

    // Order getters for Member
    public static String getMemberOrderGuid() {
        return memberOrderGuid;
    }

    public static String getMemberOrderId() {
        return memberOrderId;
    }

    // Order getters for NON-MEMBER
    public static String getNonMemberOrderGuid() {
        return existingMemberOrderGuid;
    }

    public static String getNonMemberOrderId() {
        return existingMemberOrderId;
    }

    @Deprecated
    public static String getExistingMemberOrderGuid() {
        return getNonMemberOrderGuid();
    }

    @Deprecated
    public static String getExistingMemberOrderId() {
        return getNonMemberOrderId();
    }

    // Order getters for NEW USER
    public static String getNewUserOrderGuid() {
        return newUserOrderGuid;
    }

    public static String getNewUserOrderId() {
        return newUserOrderId;
    }

    // ============================================================
    // API PERFORMANCE TRACKING
    // ============================================================
    /**
     * Stores ALL performance data points for each API call.
     * Key: Endpoint URL/Name
     * Value: List of response times in ms for every execution of that endpoint.
     */
    private static final Map<String, List<Long>> apiPerformanceMetrics = new HashMap<>();

    /**
     * Stores the response time for a given endpoint.
     * Appends to existing list if endpoint has been called before.
     * 
     * @param endpoint The API endpoint URL or name
     * @param timeInMs Execution time in milliseconds
     */
    public static synchronized void storeApiPerformance(String endpoint, long timeInMs) {
        apiPerformanceMetrics.computeIfAbsent(endpoint, k -> new ArrayList<>()).add(timeInMs);
    }

    /**
     * Retrieves all stored performance metrics.
     * 
     * @return Map of endpoint -> List of response times
     */
    public static Map<String, List<Long>> getAllApiPerformance() {
        return apiPerformanceMetrics;
    }

    /**
     * Gets performance history for a specific endpoint.
     * 
     * @param endpoint The endpoint to look up
     * @return List of times in ms, or empty list if not found
     */
    public static List<Long> getApiPerformanceHistory(String endpoint) {
        return apiPerformanceMetrics.getOrDefault(endpoint, new ArrayList<>());
    }

    /**
     * Gets the latest performance time for a specific endpoint.
     * 
     * @param endpoint The endpoint to look up
     * @return Time in ms, or -1 if not found
     */
    public static long getLatestApiPerformance(String endpoint) {
        List<Long> times = apiPerformanceMetrics.get(endpoint);
        if (times == null || times.isEmpty()) {
            return -1L;
        }
        return times.get(times.size() - 1);
    }

    /**
     * Clears all stored performance data.
     */
    public static void clearPerformanceMetrics() {
        apiPerformanceMetrics.clear();
    }

    /**
     * Prints a comprehensive summary of all API performance metrics collected.
     * Shows average, max, and call count for each endpoint.
     */
    public static void printPerformanceSummary() {
        System.out.println("\n[PERFORMANCE] === COMPREHENSIVE API PERFORMANCE SUMMARY ===");
        if (apiPerformanceMetrics.isEmpty()) {
            System.out.println("   No performance data recorded.");
        } else {
            apiPerformanceMetrics.forEach((endpoint, times) -> {
                long count = times.size();
                long total = times.stream().mapToLong(Long::longValue).sum();
                long avg = total / count;
                long max = times.stream().mapToLong(Long::longValue).max().orElse(0);

                String emoji = avg < 1000 ? "[FAST]" : (avg < 5000 ? "[SLOW]" : "[VERY SLOW]");

                System.out.printf("   %-80s\n", endpoint);
                System.out.printf("   > Status: %s | Avg: %d ms | Max: %d ms | Calls: %d\n",
                        emoji, avg, max, count);
                System.out.println("   > History: " + times.toString());
                System.out
                        .println("   --------------------------------------------------------------------------------");
            });
        }
        System.out.println("====================================================\n");
    }

    // ============================================================
    // EXPECTED TEST RESULTS (Shared between UI and API)
    // ============================================================
    private static final Map<String, String> expectedTestResults = new HashMap<>();

    public static synchronized void storeExpectedTestResult(String testName, String value) {
        expectedTestResults.put(testName, value);
    }

    public static Map<String, String> getAllExpectedTestResults() {
        return new HashMap<>(expectedTestResults);
    }

    public static void clearExpectedTestResults() {
        expectedTestResults.clear();
    }

    // ============================================================
    // REWARDS DATA
    // ============================================================
    // ============================================================
    // REWARDS DATA
    // ============================================================
    private static final ThreadLocal<Double> initialTotalRewards = new ThreadLocal<>();
    private static final ThreadLocal<Double> finalTotalRewards = new ThreadLocal<>();
    private static final ThreadLocal<Double> rewardsGain = new ThreadLocal<>();
    private static final ThreadLocal<Double> rewardsBasisPaidAmount = new ThreadLocal<>();
    private static final ThreadLocal<Double> cancelledOrderRewardsGain = new ThreadLocal<>();
    private static final ThreadLocal<Double> rewardsUsed = new ThreadLocal<>();
    private static final ThreadLocal<Double> currentDueAmount = new ThreadLocal<>();

    public static void setInitialTotalRewards(double v) {
        initialTotalRewards.set(v);
    }

    public static double getInitialTotalRewards() {
        return initialTotalRewards.get() != null ? initialTotalRewards.get() : 0.0;
    }

    public static void setFinalTotalRewards(double v) {
        finalTotalRewards.set(v);
    }

    public static double getFinalTotalRewards() {
        return finalTotalRewards.get() != null ? finalTotalRewards.get() : 0.0;
    }

    public static void setRewardsGain(double v) {
        rewardsGain.set(v);
    }

    public static double getRewardsGain() {
        return rewardsGain.get() != null ? rewardsGain.get() : 0.0;
    }

    public static void setRewardsBasisPaidAmount(double v) {
        rewardsBasisPaidAmount.set(v);
    }

    public static double getRewardsBasisPaidAmount() {
        return rewardsBasisPaidAmount.get() != null ? rewardsBasisPaidAmount.get() : 0.0;
    }

    public static void setCancelledOrderRewardsGain(double v) {
        cancelledOrderRewardsGain.set(v);
    }

    /**
     * Returns the rewards_gain for the specific cancelled order (read from getOrderById
     * in step19_A). Falls back to the primary rewardsGain if not set (single-member flows
     * where primary == cancelled order).
     */
    public static double getCancelledOrderRewardsGain() {
        Double v = cancelledOrderRewardsGain.get();
        return v != null ? v : getRewardsGain();
    }

    public static void setRewardsUsed(double v) {
        rewardsUsed.set(v);
    }

    public static double getRewardsUsed() {
        return rewardsUsed.get() != null ? rewardsUsed.get() : 0.0;
    }

    // ── Excel name hints for payload builder ────────────────────────────────
    private static final ThreadLocal<String> excelFirstName  = new ThreadLocal<>();
    private static final ThreadLocal<String> excelLastName   = new ThreadLocal<>();
    private static final ThreadLocal<String> excelMiddleName = new ThreadLocal<>();

    public static void setExcelFirstName(String v)  { excelFirstName.set(v);  }
    public static void setExcelLastName(String v)   { excelLastName.set(v);   }
    public static void setExcelMiddleName(String v) { excelMiddleName.set(v); }
    public static String getExcelFirstName()  { return excelFirstName.get();  }
    public static String getExcelLastName()   { return excelLastName.get();   }
    public static String getExcelMiddleName() { return excelMiddleName.get(); }

    public static void setCurrentDueAmount(double v) {
        currentDueAmount.set(v);
    }

    public static double getCurrentDueAmount() {
        return currentDueAmount.get() != null ? currentDueAmount.get() : 0.0;
    }

    // Per-order amounts — populated during callApprovePaymentAPI from payment breakdown.
    // Key = orderId (guid), Value = sum of item final_prices for that order.
    // Used by step18_B to obtain the exact per-member due amount regardless of
    // whether members have the same or different order totals.
    private static final ThreadLocal<Map<String, Double>> orderAmounts =
            ThreadLocal.withInitial(java.util.HashMap::new);

    public static void setOrderAmounts(Map<String, Double> amounts) {
        orderAmounts.get().clear();
        if (amounts != null) {
            orderAmounts.get().putAll(amounts);
        }
    }

    public static Map<String, Double> getOrderAmounts() {
        return orderAmounts.get();
    }

    public static double getOrderAmount(String orderId) {
        if (orderId == null) return 0.0;
        Double v = orderAmounts.get().get(orderId);
        return v != null ? v : 0.0;
    }

    // ============================================================
    // PACKAGE COMPONENTS STORAGE
    // ============================================================
    // ============================================================
    // PACKAGE COMPONENTS STORAGE
    // ============================================================
    private static final ThreadLocal<List<String>> packageTestNames = new ThreadLocal<>();

    public static List<String> getPackageTestNames() {
        return packageTestNames.get();
    }

    public static void setPackageTestNames(List<String> list) {
        packageTestNames.set(list);
    }

    public static void addPackageTestNames(List<String> list) {
        if (packageTestNames.get() == null)
            packageTestNames.set(new ArrayList<>());
        packageTestNames.get().addAll(list);
    }

    public static void clearPackageTestNames() {
        if (packageTestNames.get() != null)
            packageTestNames.get().clear();
        if (componentIdToNameMap.get() != null)
            componentIdToNameMap.get().clear();
    }

    private static final ThreadLocal<Map<String, String>> componentIdToNameMap = ThreadLocal.withInitial(HashMap::new);

    public static void storeComponentIdMapping(String id, String name) {
        if (id != null && name != null) {
            componentIdToNameMap.get().put(id, name);
        }
    }

    public static String getComponentNameById(String id) {
        return componentIdToNameMap.get().get(id);
    }

    public static Map<String, String> getComponentIdToNameMap() {
        return componentIdToNameMap.get();
    }

    public static void clearComponentMappings() {
        if (componentIdToNameMap.get() != null)
            componentIdToNameMap.get().clear();
    }

    private static final ThreadLocal<Integer> packageTestCount = new ThreadLocal<>();

    public static int getPackageTestCount() {
        return packageTestCount.get() != null ? packageTestCount.get() : 0;
    }

    public static void setPackageTestCount(int count) {
        packageTestCount.set(count);
    }

    private static double couponAmount = 0.0;

    public static void setCouponAmount(double amount) {
        couponAmount = amount;
        System.out.println(">>> RequestContext: Stored Coupon Discount: ₹" + amount);
    }

    public static double getCouponAmount() {
        return couponAmount;
    }

    private static List<String> memberIds;

    public static void setMemberIds(List<String> ids) {
        memberIds = (ids != null) ? new ArrayList<>(ids) : new ArrayList<>();
    }

    public static List<String> getMemberIds() {
        return memberIds;
    }

    public static void addMemberId(String id) {
        if (memberIds == null) {
            memberIds = new ArrayList<>();
        }
        memberIds.add(id);
    }

    // ── Per-visit processing flag ────────────────────────────────────────────
    // Set to true by COD_99 after it has processed every visit inline.
    // COD_16 / COD_17 in the XML suite check this flag and skip if already done.
    public static void setVisitsProcessedByUI(boolean value) {
        visitsProcessedByUI = value;
    }

    public static boolean isVisitsProcessedByUI() {
        return visitsProcessedByUI;
    }
}
