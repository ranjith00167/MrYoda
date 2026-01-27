package com.mryoda.diagnostics.api.services;

import com.mryoda.diagnostics.api.config.ConfigLoader;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Payment;
import com.razorpay.Utils;
import org.json.JSONObject;

/**
 * Razorpay Service - Real Razorpay Integration
 */
public class RazorpayService {

    private RazorpayClient razorpayClient;
    private String keyId;
    private String keySecret;

    public RazorpayService() {
        try {
            this.keyId = ConfigLoader.getConfig().razorpayKey();
            this.keySecret = ConfigLoader.getConfig().razorpaySecret();
            this.razorpayClient = new RazorpayClient(keyId, keySecret);
            System.out.println("✅ RazorpayService initialized with credentials");
        } catch (RazorpayException e) {
            System.err.println("❌ Failed to initialize Razorpay client: " + e.getMessage());
            throw new RuntimeException("Razorpay initialization failed", e);
        }
    }

    public String getKeyId() {
        return this.keyId;
    }

    public Order createOrder(int amountInPaise, String currency, String receiptId,
            String userId, String mobile, String slotGuid) throws RazorpayException {

        System.out.println("Creating Order in Razorpay...");

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", receiptId);

        JSONObject notes = new JSONObject();
        notes.put("user_id", userId);
        notes.put("mobile", mobile);
        notes.put("slot_guid", slotGuid);
        orderRequest.put("notes", notes);

        Order order = razorpayClient.orders.create(orderRequest);
        System.out.println("Order Created: " + order.get("id"));

        return order;
    }

    public Payment fetchPaymentDetails(String paymentId) throws RazorpayException {
        Payment payment = com.mryoda.diagnostics.api.utils.RazorpayFetchUtil.fetchPaymentWithClient(
                razorpayClient,
                paymentId);
        com.mryoda.diagnostics.api.utils.RazorpayFetchUtil.displayPaymentSummary(payment);
        return payment;
    }

    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", orderId);
            attributes.put("razorpay_payment_id", paymentId);
            attributes.put("razorpay_signature", signature);

            Utils.verifyPaymentSignature(attributes, keySecret);
            System.out.println("✅ Signature Verified Successfully!");
            return true;
        } catch (RazorpayException e) {
            System.err.println("❌ Signature Verification Failed!");
            return false;
        }
    }

    public String generateTestSignature(String orderId, String paymentId) {
        try {
            String message = orderId + "|" + paymentId;

            javax.crypto.Mac sha256Hmac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                    keySecret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            System.err.println("❌ Error generating test signature: " + e.getMessage());
            return null;
        }
    }

    public static class PaymentVerificationResult {
        private boolean success;
        private String message;
        private String paymentId;
        private String orderId;
        private String paymentMode;
        private int amount;
        private boolean isCod;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getPaymentId() {
            return paymentId;
        }

        public void setPaymentId(String paymentId) {
            this.paymentId = paymentId;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getPaymentMode() {
            return paymentMode;
        }

        public void setPaymentMode(String paymentMode) {
            this.paymentMode = paymentMode;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public boolean isIsCod() {
            return isCod;
        }

        public void setIsCod(boolean isCod) {
            this.isCod = isCod;
        }
    }
}
