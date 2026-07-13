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
    public static final String OTP_SEND = "/otps/send";
    public static final String OTP_RESEND = "/otps/resend";
    public static final String USER_CREATE = "/users/addUser";
    public static final String USER_PROFILE = "/user/profile";
    public static final String UPDATE_PROFILE = "/user/update";
    public static final String UPDATE_USER   = "/users/updateUser";
    public static final String GET_USER      = "/users/getUser/{user_id}";
    public static final String GET_BY_GUIDS  = "/users/getByGuids";
    public static final String GET_ALL_USERS              = "/users/getAllUsers";
    public static final String GET_ADMIN_USERS            = "/users/getAdminUsers";
    public static final String GET_ALL_DELETED_USERS      = "/users/getAllDeletedUsers";
    public static final String DELETE_USER                 = "/users/deleteUser/{user_guid}";
    public static final String GET_USER_REWARDS_BY_MOBILE  = "/users/getRewardsByMobile/{mobile}";
    public static final String GET_USERS_BY_MOBILE         = "/users/getUsersByMobile/{mobile}";
    public static final String GET_USER_BY_MOBILE          = "/users/mobile/";
    public static final String GET_USER_NOTIFICATIONS      = "/users/notifications/user/{user_id}";

    // ========== LOCATION & SEARCH ==========
    public static final String GET_LOCATION = "/tests/getlocations";
    public static final String GET_ALL_LOCATIONS = "/tests/getAllLocations";
    public static final String GLOBAL_SEARCH = "tests/adminTests";
    public static final String GLOBAL_SEARCH_API = "/tests/global-search";
    public static final String GET_ALL_TESTS = "/tests/getAllTests";
    public static final String GET_TEST_BY_SLUG = "/tests/getTestBySlug";
    public static final String GET_FETAL_MEDICINE_TESTS = "/tests/getFetalMedicineTests";
    public static final String GET_ALL_PACKAGES = "/tests/getAllPackages";
    public static final String GET_PACKAGE_BY_ID = "/tests/getPackageById/";
    public static final String GET_PACKAGE_BY_SLUG = "/tests/getPackageBySlug/";
    public static final String GET_ALL_SYMPTOMS = "/tests/getAllSymptoms";
    public static final String GET_ORGANS       = "/tests/getOrgans";
    public static final String GET_ALL_DISEASES = "/tests/getAllDiseases";
    public static final String SEARCH_STRING    = "/tests/searchString";
    public static final String ADMIN_TESTS      = "/tests/adminTests";

    // ========== BRAND & MEMBERSHIP ==========
    public static final String GET_ALL_BRANDS = "https://staging-api-membership.yodaprojects.com/brand/getAllBrands";
    public static final String GET_REWARDS_BY_MOBILE = "/reward/getRewardsByMobile/{mobile_number}";

    // ========== CART MANAGEMENT ==========
    public static final String ADD_TO_CART = "/carts/v2/addCart";
    public static final String GET_CART_BY_ID = "/carts/v2/getCartById/{user_id}";

    // ========== ADDRESS MANAGEMENT ==========
    public static final String ADD_ADDRESS            = "/address/addAddress";
    public static final String GET_ADDRESS_BY_USER_ID = "/address/getAddressByUserId/{user_id}";
    public static final String GET_ADDRESS_BY_GUID    = "/address/getAddressByGuid/{address_guid}";
    public static final String DELETE_ADDRESS_BY_ID   = "/address/deleteAddressById/{address_guid}";
    public static final String UPDATE_ADDRESS_BY_ID   = "/address/UpdateAddressById";
    public static final String CHECK_SERVING_LOCATION = "/address/checkServingLocation";

    // ========== SLOT & CENTER MANAGEMENT ==========
    public static final String GET_CENTERS_BY_ADD = "/slot/getCentersByadd";
    public static final String GET_SLOT_COUNT_BY_TIME = "/slot/getSlotCountByTime";

    // ========== ORDER MANAGEMENT ==========
    public static final String CREATE_ORDER = "/gateway/v2/CreateOrder";
    public static final String VERIFY_PAYMENT = "/gateway/v2/VerifyPayment";
    public static final String GET_PAYMENT_BY_ID = "/gateway/getPaymentById";
    public static final String GET_ORDER_BY_ID = "/order/getOrderById/";
    public static final String GET_ALL_ORDERS_BY_USER = "/order/getAllOrdersByUser/";
    public static final String UPDATE_ORDER_SLOTS = "/order/updateOrderSlots";
    public static final String PHLEBO_LOGIN = "/phlebo/loginPhlebo";

    // ========== BASE URLS ==========
    public static final String MEMBER_BASE_URL = "https://staging-api-membership.yodaprojects.com";
    public static final String DIAGNOSTICS_BASE_URL = "https://staging-api-diagnostics.yodaprojects.com";
    public static final String DEV_DIAGNOSTICS_BASE_URL = "https://dev-api-yodadiagnostics.yodaprojects.com";

    // ========== SUPER ADMIN OTP ==========
    public static final String SUPER_ADMIN_OTP_VERIFY = "/auth/admin/otp/verify";

    // ========== MEMBERSHIP & CUSTOMER ==========
    public static final String ADD_CUSTOMER = "/membership/customer/addCustomer";

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

    // ========== CANCELLATION & REFUND ==========
    public static final String APPROVE_CANCELLED_ORDER = "/order/approveCancelldOrder";
    public static final String NEW_RETURNING_CASHBACK   = "/order/v2NewReturningCashback";   // legacy — kept for reference
    public static final String ADMIN_RETURNING_CASHBACK = "/order/adminReturningCashback";

    // ========== TRANSACTION ==========
    public static final String GET_TRANSACTION_BY_MOBILE = "/transaction/getTransactionByMobile/{mobile_number}";

    // ========== FAMILY MEMBER ==========
    public static final String GET_ALL_FAMILY_MEMBERS = "/familymembers/GetAllFamilyMembersByUser/{user_id}";
    public static final String ADD_FAMILY_MEMBER = "/familymembers/addFamilyMember";
    public static final String GET_FAMILY_MEMBER_BY_ID = "/familymembers/GetFamilyMemberById/{guid}";
    public static final String UPDATE_FAMILY_MEMBER = "/familymembers/updateFamilyMember";
    public static final String DELETE_FAMILY_MEMBER_BY_ID = "/familymembers/deleteFamilyMemberById/{guid}";
    public static final String GET_ALL_COUPONS = "/coupons/getAllCoupons";
    public static final String ADMIN_LOGIN = "/auth/login";

    // ========== PHLEBO NOTIFICATION SERVICE ==========
    public static final String PHLEBO_NOTIFICATION_BASE_URL = "https://staging-api-phlebo-notification.yodadiagnostics.com";
    public static final String PHLEBO_NOTIFICATION_LOGIN = "/api/v1/phlebo/login";
    public static final String PHLEBO_ASSIGN_SHIFTS = "/api/v1/phlebo/assign-shifts";
    public static final String PHLEBO_GET_SHIFTS = "/api/v1/phlebo/{phlebo_guid}/shifts";
    public static final String PHLEBO_CLOCK_IN = "/api/v1/phlebo/shift/clock-in";
}
