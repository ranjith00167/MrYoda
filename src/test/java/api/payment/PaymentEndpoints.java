package api.payment;

public class PaymentEndpoints {
    /** Step 1 – Create Razorpay order, returns razorpay_order_id + payment link. */
    public static final String INITIATE_PAYMENT  = "/gateway/v2/InitiatePayment";
    /** Step 2 – Verify Razorpay callback (razorpay_payment_id + signature). */
    public static final String VERIFY_PAYMENT    = "/gateway/v2/VerifyPayment";
    /** Fetch payment details by internal order / cart id. */
    public static final String GET_PAYMENT_BY_ID = "/gateway/getPaymentById";
    /** Fetch payment details by user ID. */
    public static final String GET_PAYMENT_BY_USER_ID = "/gateway/getPaymentByUserId/{user_id}";
    /** Poll current payment status. */
    public static final String PAYMENT_STATUS    = "/gateway/v2/paymentStatus";
}
