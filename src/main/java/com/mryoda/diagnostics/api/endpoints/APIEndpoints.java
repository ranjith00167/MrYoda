package com.mryoda.diagnostics.api.endpoints;

/**
 * API Endpoints - Centralized repository for all API endpoints used in
 * framework
 * All API endpoints are defined here to maintain consistency and easy
 * maintenance
 */
public class APIEndpoints {

    // ========== AUTHENTICATION & USER MANAGEMENT ==========
    public static final String OTP_REQUEST = "/otps/getOtp";
    public static final String OTP_VERIFY = "/otps/getOtp";
    public static final String USER_CREATE = "/users/addUser";
    public static final String USER_PROFILE = "/user/profile";
    public static final String UPDATE_PROFILE = "/user/update";

    // ========== LOCATION & SEARCH ==========
    public static final String GET_LOCATION = "/tests/getlocations";
    public static final String GLOBAL_SEARCH = "tests/adminTests";
    public static final String GET_ALL_TESTS = "/tests/getAllTests";
    public static final String GET_FETAL_MEDICINE_TESTS = "/tests/getFetalMedicineTests";
    public static final String GET_ALL_PACKAGES = "/tests/getAllPackages";

    // ========== BRAND & MEMBERSHIP ==========
    public static final String GET_ALL_BRANDS = "https://staging-api-membership.yodaprojects.com/brand/getAllBrands";
    public static final String GET_REWARDS_BY_MOBILE = "/reward/getRewardsByMobile/{mobile_number}";

    // ========== CART MANAGEMENT ==========
    public static final String ADD_TO_CART = "/carts/v2/addCart";
    public static final String GET_CART_BY_ID = "/carts/v2/getCartById/{user_id}";

    // ========== ADDRESS MANAGEMENT ==========
    public static final String ADD_ADDRESS = "/address/addAddress";
    public static final String GET_ADDRESS_BY_USER_ID = "/address/getAddressByUserId/{user_id}";

    // ========== SLOT & CENTER MANAGEMENT ==========
    public static final String GET_CENTERS_BY_ADD = "/slot/getCentersByadd";
    public static final String GET_SLOT_COUNT_BY_TIME = "/slot/getSlotCountByTime";

    // ========== ORDER MANAGEMENT ==========
    public static final String CREATE_ORDER = "/gateway/v2/CreateOrder";
    public static final String VERIFY_PAYMENT = "/gateway/v2/VerifyPayment";
    public static final String GET_PAYMENT_BY_ID = "/gateway/getPaymentById";
    public static final String GET_ORDER_BY_ID = "/order/getOrderById/";
    public static final String PHLEBO_LOGIN = "/phlebo/loginPhlebo";

    // ========== BASE URLS ==========
    public static final String MEMBER_BASE_URL = "https://staging-api-membership.yodaprojects.com";
    public static final String DIAGNOSTICS_BASE_URL = "https://staging-api-diagnostics.yodaprojects.com";

    // ========== MEMBERSHIP ==========
    public static final String GET_USER = "/users/getUser/{user_id}";

    // ========== ORDER & TRACKING ==========
    public static final String ASSIGN_ORDER = "/order_tracking/assignOrder";
    public static final String UPDATE_ORDER_TRACKING = "/order_tracking/updateOrderTracking";
    public static final String GET_ORDER_TRACKING_STATUS = "/order_tracking/getOrderTrackingStatus/{guid}";

    // ========== BRAND ==========
    public static final String GET_ALL_BRANDS_PATH = "/brand/getAllBrands";
    public static final String ADMIN_VERIFY_OTP = "/order_tracking/admin/verifyotp";
    public static final String GET_SAMPLE_TYPE = "/tests/getSampleType";
    public static final String APPROVE_PAYMENT = "/order/approvepayment";
    public static final String VISIT_STATUS_UAT = "http://uat.yodalifeline.in/yoda_uat_8.0/api/StatusApi/GetStatus";
    public static final String GET_REPORT_DETAILS = "/report/getReportDetailsByVisitNumber/{visit_Number}";
    public static final String UPDATE_ORDER = "/order/v2updateOrder";

    // ========== FAMILY MEMBER ==========
    public static final String GET_ALL_FAMILY_MEMBERS = "/familymembers/GetAllFamilyMembersByUser/{user_id}";
    public static final String ADD_FAMILY_MEMBER = "/familymembers/addFamilyMember";
    public static final String GET_FAMILY_MEMBER_BY_ID = "/familymembers/GetFamilyMemberById/{guid}";
    public static final String UPDATE_FAMILY_MEMBER = "/familymembers/updateFamilyMember";
    public static final String DELETE_FAMILY_MEMBER_BY_ID = "/familymembers/deleteFamilyMemberById/{guid}";
    public static final String ADMIN_LOGIN = "/auth/login";
}
