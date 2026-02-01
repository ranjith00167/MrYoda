package api.order;

public class OrderEndpoints {
    public static final String CREATE_ORDER = "/gateway/v2/CreateOrder";
    public static final String GET_ORDER_BY_ID = "/order/getOrderById/"; // Note: might need ID appended
    public static final String ASSIGN_ORDER = "/order_tracking/assignOrder";
    public static final String UPDATE_ORDER_TRACKING = "/order_tracking/updateOrderTracking";
    public static final String GET_ORDER_TRACKING_STATUS = "/order_tracking/getOrderTrackingStatus/{guid}";
    public static final String APPROVE_PAYMENT = "/order/approvepayment";
    public static final String UPDATE_ORDER = "/order/v2updateOrder";
    public static final String ADMIN_VERIFY_OTP = "/order_tracking/admin/verifyotp";
    public static final String VISIT_STATUS_UAT = "http://uat.yodalifeline.in/yoda_uat_8.0/api/StatusApi/GetStatus";
    public static final String GET_REPORT_DETAILS = "/report/getReportDetailsByVisitNumber/{visit_Number}";
}
