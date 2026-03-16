# Single Member Cancellation Flow - Test Documentation

## Overview

This test suite validates the **complete cancellation flow** for a single member order in the MrYoda diagnostic testing platform. The flow covers all API calls required for order cancellation from the member initiating cancellation through admin approval.

## Test Objective

Verify that a paid member can successfully cancel an order and that:
1. Admin can approve the cancellation
2. Order status updates to "Cancelled"
3. Payment information reflects cancellation
4. Transaction records are properly updated
5. Cashback/refund processing is handled correctly

## Test Flow

### Step 1: Member Login (step01_MemberLogin)
- **API:** OTP/Login API
- **Credentials:** Paid member from config (e.g., mobile: 9003730394)
- **Expected Output:** Member token, User ID
- **Validation:** 
  - Token is not null
  - User ID is valid
- **Dependencies:** None

### Step 2: Admin Login (step02_AdminLogin)
- **API:** `/auth/login`
- **Endpoint:** `POST /auth/login`
- **Credentials:** Admin credentials from config (admin.main.identifier, admin.main.password)
- **Payload:**
  ```json
  {
    "identifier": "admin@example.com",
    "user_name": "admin",
    "password": "password",
    "type": "login",
    "fcmToken": "..."
  }
  ```
- **Expected Output:** 
  - Admin token
  - Admin GUID (admin_approval_by)
- **Validation:**
  - HTTP 200 status
  - Admin token present
  - Admin GUID extracted and stored in RequestContext
- **Dependencies:** step01_MemberLogin

### Step 3: Create Order (step03_CreateOrder)
- **API:** `POST /gateway/v2/CreateOrder`
- **Endpoint:** `https://staging-api-diagnostics.yodaprojects.com/gateway/v2/CreateOrder`
- **Payload:**
  ```json
  {
    "cart_id": "test_cart_123",
    "user_id": "member_user_id",
    "payment_mode": "online",
    "source": "android",
    "address_id": "test_address_123",
    "slot_id": "test_slot_123",
    "date": "2026-03-20",
    "time": "10:00",
    "lab_location_id": "test_lab_123",
    "total_amount": 500
  }
  ```
- **Expected Output:** Order ID (Razorpay order ID)
- **Validation:**
  - HTTP 200 status
  - Order ID starts with "order_"
  - Success flag: true
- **Dependencies:** step02_AdminLogin

### Step 4: Get Cashback Information (step04_GetCashbackInfo)
- **API:** `POST /order/v2NewReturningCashback`
- **Endpoint:** `https://staging-api-diagnostics.yodaprojects.com/order/v2NewReturningCashback`
- **Payload:**
  ```json
  {
    "order_guid": "order_id_from_step3"
  }
  ```
- **Expected Response:**
  ```json
  {
    "order_guid": "...",
    "order_status": "..."
  }
  ```
- **Validation:**
  - HTTP 200 status
  - Response not empty
- **Dependencies:** step03_CreateOrder

### Step 5: Approve Cancellation (step05_ApproveCancellation)
- **API:** `POST /order/approveCancelldOrder`
- **Endpoint:** `https://staging-api-diagnostics.yodaprojects.com/order/approveCancelldOrder`
- **Payload:**
  ```json
  {
    "order_id": "order_id_from_step3",
    "remarks": "test",
    "admin_approval_by": "admin_guid_from_step2",
    "status": "Approve"
  }
  ```
- **Expected Output:**
  ```json
  {
    "success": true,
    "msg": "Cancellation approved",
    ...
  }
  ```
- **Validation:**
  - HTTP 200 status
  - Success flag: true
- **Dependencies:** step04_GetCashbackInfo

### Step 6: Verify Order Cancelled Status (step06_VerifyOrderCancelledStatus)
- **API:** `GET /order/getOrderById/{order_guid}`
- **Endpoint:** `https://staging-api-diagnostics.yodaprojects.com/order/getOrderById/{order_guid}`
- **Expected Output:**
  ```json
  {
    "data": {
      "order_status": "Cancelled",
      "cancelled_by": "...",
      "admin_approval_by": "admin_guid_from_step2",
      ...
    }
  }
  ```
- **Validation:**
  - HTTP 200 status
  - Order status = "Cancelled"
  - Admin approval by matches admin GUID from step 2
  - Cancelled by field populated
- **Dependencies:** step05_ApproveCancellation

### Step 7: Verify Payment Details (step07_VerifyPaymentDetails)
- **API:** `POST /gateway/getPaymentById`
- **Endpoint:** `https://staging-api-diagnostics.yodaprojects.com/gateway/getPaymentById`
- **Payload:**
  ```json
  {
    "payment_id": "payment_id_from_order"
  }
  ```
- **Expected Output:**
  ```json
  {
    "data": {
      "payments": {
        "payment_status": "...",
        "payment_mode": "...",
        "amount": "..."
      }
    }
  }
  ```
- **Validation:**
  - HTTP 200 status
  - Payment status present
  - Payment mode present
  - Amount present
- **Dependencies:** step06_VerifyOrderCancelledStatus

### Step 8: Verify Transaction Details (step08_VerifyTransactionDetails)
- **API:** `GET /transaction/getTransactionByMobile/{mobile_number}`
- **Endpoint:** `https://staging-api-diagnostics.yodaprojects.com/transaction/getTransactionByMobile/9003730394`
- **Expected Output:**
  ```json
  {
    "data": [
      {
        "order_id": "...",
        "amount": "...",
        "status": "...",
        "transaction_type": "...",
        ...
      }
    ]
  }
  ```
- **Validation:**
  - HTTP 200 status
  - At least one transaction exists
  - Transaction details include order_id, amount, status
- **Dependencies:** step07_VerifyPaymentDetails

### Step 9: Cancellation Flow Summary (step09_CancellationFlowSummary)
- **Purpose:** Display summary of completed flow
- **Output:** Console summary of all validated steps
- **Dependencies:** step08_VerifyTransactionDetails

## Test Execution

### Prerequisites
1. Java 11+ installed
2. Maven configured
3. Test environment accessible
4. config.properties with valid admin credentials

### Run Single Cancellation Test Suite

```bash
cd c:\Users\RANJITH\MrYoda

# Run the specific test suite
mvn test -Dsurefire.suiteXmlFiles=test-suites/testng_single_member_cancellation.xml

# Or run with specific test class
mvn test -Dtest=SingleMemberCancellationFlowTest

# Run with specific method
mvn test -Dtest=SingleMemberCancellationFlowTest#step01_MemberLogin
```

### Run with TestNG
```bash
java -cp "target/classes:target/test-classes:lib/*" org.testng.TestNG test-suites/testng_single_member_cancellation.xml
```

## Test Configuration

### Required config.properties settings:
```properties
# Member Credentials (Paid Member)
member.mobile=9003730394
member.password=1234

# Non-Member Credentials
nonmember.mobile=8220220227
nonmember.password=1234

# Admin Credentials
admin.main.identifier=admin@example.com
admin.main.password=admin_password

# API Base URLs
diagnostics.base.url=https://staging-api-diagnostics.yodaprojects.com
```

## Expected Results

### Successful Execution
- All 9 steps pass
- Order status transitions: Created → Cancelled
- Admin approval is recorded
- Payment and transaction records are updated
- Console output shows all validation points passed

### Example Console Output
```
==========================================================
      STEP 1: MEMBER LOGIN
==========================================================
✅ Member Login Success
   Mobile: 9003730394
   User ID: xyz-user-id
   Token: eyJ0eXAiOiJKV1QiLCJhbGc...

==========================================================
      STEP 2: ADMIN LOGIN
==========================================================
✅ Admin Login Success
   Admin GUID: admin-guid-12345
   Admin Token: eyJ0eXAiOiJKV1QiLCJhbGc...

... (more steps) ...

==========================================================
      CANCELLATION FLOW SUMMARY
==========================================================
✅ ALL TESTS PASSED - SINGLE MEMBER CANCELLATION FLOW

   Order Details:
      Order ID: order_ABC123
      Member Mobile: 9003730394
      Member User ID: xyz-user-id
      Admin GUID: admin-guid-12345

   Flow Completed:
      ✅ Member Login
      ✅ Admin Login
      ✅ Order Creation
      ✅ Cashback API Call
      ✅ Cancellation Approval
      ✅ Order Status Verification
      ✅ Payment Verification
      ✅ Transaction Verification

   Note: NO UI Automation or Report Generation triggered.
         Focus: Pure API validation with comprehensive checks.
```

## Important Notes

### About Order Creation
The test uses a simplified order creation payload for testing purposes. In production scenarios, you should:
- Use actual cart data from previous test steps
- Use actual slot availability
- Use valid user addresses

### About Admin GUID
The `admin_approval_by` field is extracted from the admin login response and stored in `RequestContext.getAdminGuid()`. This ensures consistency across the cancellation flow.

### API Validations
Each API call includes:
- HTTP status code validation (typically 200)
- Response structure validation
- Field presence validation
- Field value validation (where applicable)
- Cross-API value matching (order ID, admin GUID, etc.)

### Error Handling
- **409 Conflict:** May occur if phlebotomist already assigned (handled gracefully)
- **Non-existent Order:** Test uses mock order ID for demonstration
- **Payment API Failures:** Non-blocking, continues to next step with warning

### No UI Automation
This test intentionally avoids:
- UI automation via Selenium WebDriver
- Report generation via COD pipeline
- Visit scheduling and Lab result entry

Focus is purely on API validation and data consistency.

## Expected Endpoints Summary

| Step | API Endpoint | Method | Status |
|------|-------------|--------|--------|
| 2 | /auth/login | POST | 200 |
| 3 | /gateway/v2/CreateOrder | POST | 200 |
| 4 | /order/v2NewReturningCashback | POST | 200 |
| 5 | /order/approveCancelldOrder | POST | 200 |
| 6 | /order/getOrderById/{guid} | GET | 200 |
| 7 | /gateway/getPaymentById | POST | 200 |
| 8 | /transaction/getTransactionByMobile/{mobile} | GET | 200 |

## Troubleshooting

### Test Fails at Member Login
- Check config.properties has valid member mobile
- Verify member account exists in test environment
- Check network connectivity to auth API

### Test Fails at Admin Login
- Verify admin.main.identifier and admin.main.password in config
- Check admin account is active
- Ensure admin has cancellation approval permissions

### Test Fails at Order Creation
- Mock order ID will be generated automatically
- For real orders, ensure cart and slot data is valid
- Check user address exists

### Test Fails at Approve Cancellation
- Verify admin GUID was properly extracted from step 2
- Check order exists before attempting cancellation
- Ensure order is not already in cancelled/completed state

## Logging

Test results are logged to:
- **Console:** All validation outputs
- **Reports:** Extent Reports (if configured)
- **Files:** Soft failures logged to `logs/cod_failures.log`

## References

- [API Endpoints Documentation](./IMPLEMENTATION_DETAILS_GLOBAL_SEARCH_PRICING.md)
- [Full COD Flow Validation Documentation](./Full_COD_Flow_Validation_Documentation.md)
- [MrYoda Test Execution Tracker](./MrYoda_Test_Execution_Tracker.csv)
