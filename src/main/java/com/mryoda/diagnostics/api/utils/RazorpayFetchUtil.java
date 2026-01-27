package com.mryoda.diagnostics.api.utils;

import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

/**
 * REUSABLE UTILITY CLASS for Razorpay Payment Fetch
 */
public class RazorpayFetchUtil {

    // Razorpay credentials (configure from environment variables or
    // application.properties)
    private static final String RAZORPAY_KEY_ID = System.getenv("RAZORPAY_KEY_ID");
    private static final String RAZORPAY_KEY_SECRET = System.getenv("RAZORPAY_KEY_SECRET");

    public static Payment fetchPaymentWithClient(RazorpayClient razorpayClient, String paymentId)
            throws RazorpayException {

        if (paymentId == null || paymentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID cannot be null or empty");
        }

        if (!paymentId.startsWith("pay_")) {
            throw new IllegalArgumentException("Invalid payment ID format. Must start with 'pay_'");
        }

        System.out.println("🔄 Fetching payment from Razorpay API...");
        System.out.println("   Payment ID: " + paymentId);

        Payment payment = razorpayClient.payments.fetch(paymentId);

        System.out.println("✅ Payment fetched successfully from Razorpay!");

        return payment;
    }

    public static void displayPaymentSummary(Payment payment) {
        if (payment == null) {
            System.out.println("❌ Payment object is null");
            return;
        }

        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("📥 RAZORPAY PAYMENT DETAILS");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("Payment ID: " + payment.get("id"));
        System.out.println("Order ID: " + payment.get("order_id"));
        System.out.println("Status: " + payment.get("status"));
        System.out.println("Amount: ₹" + (((int) payment.get("amount")) / 100.0));
        System.out.println("Currency: " + payment.get("currency"));
        System.out.println("Method: " + payment.get("method"));
        System.out.println("Captured: " + payment.get("captured"));

        if (payment.has("email") && payment.get("email") != null) {
            System.out.println("Email: " + payment.get("email"));
        }
        if (payment.has("contact") && payment.get("contact") != null) {
            System.out.println("Contact: " + payment.get("contact"));
        }

        System.out.println("═══════════════════════════════════════════════════════\n");
    }

    public static Payment fetchPayment(String paymentId) throws RazorpayException {
        return fetchPayment(paymentId, RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET);
    }

    public static Payment fetchPayment(String paymentId, String keyId, String keySecret) throws RazorpayException {
        if (paymentId == null || paymentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID cannot be null or empty");
        }

        if (!paymentId.startsWith("pay_")) {
            throw new IllegalArgumentException("Invalid payment ID format. Must start with 'pay_'");
        }

        if (keyId == null || keySecret == null) {
            throw new IllegalArgumentException("Razorpay credentials are not configured");
        }

        System.out.println("🔄 Fetching payment from Razorpay API...");
        System.out.println("   Payment ID: " + paymentId);

        RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);
        Payment payment = razorpayClient.payments.fetch(paymentId);

        System.out.println("✅ Payment fetched successfully!");

        return payment;
    }

    public static boolean isPaymentCaptured(Payment payment) {
        if (payment == null) {
            return false;
        }

        String status = payment.get("status");
        return "captured".equals(status);
    }

    public static String getPaymentStatus(Payment payment) {
        if (payment == null) {
            return "unknown";
        }
        return payment.get("status");
    }

    public static double getAmountInINR(Payment payment) {
        if (payment == null) {
            return 0.0;
        }

        int amountInPaise = payment.get("amount");
        return amountInPaise / 100.0;
    }

    public static String getPaymentMethod(Payment payment) {
        if (payment == null) {
            return "unknown";
        }
        return payment.get("method");
    }

    public static void displayPayment(Payment payment) {
        if (payment == null) {
            System.out.println("❌ Payment object is null");
            return;
        }

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("📥 REAL PAYMENT DATA FROM RAZORPAY API");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        System.out.println("Payment ID: " + payment.get("id"));
        System.out.println("Status: " + payment.get("status"));

        System.out.println("═══════════════════════════════════════════════════════════════\n");
    }
}
