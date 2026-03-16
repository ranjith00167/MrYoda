# Quick Start - Hybrid + Cancellation Suite

## What Was Created

✅ **Combined Test Suite:** `testng_single_member_hybrid_cancellation.xml`

**Two-Phase Flow:**
- **Phase 1:** UI Hybrid Automation (Feature file) → Order Creation
- **Phase 2:** API Cancellation Flow (Test class) → Uses order from Phase 1

## How It Works

```
Phase 1: Feature File Execution (UI)
    └─► Creates order via UI
        └─► Stores Order ID in RequestContext
            └─► Available to Phase 2

Phase 2: Cancellation Test (API)
    └─► Detects Phase 1 Order ID
        └─► Uses it for cancellation flow
            └─► Admin approves cancellation
                └─► Verifies order status = "Cancelled"
```

## Run Commands

### Option 1: Run Full Suite (RECOMMENDED)

```bash
cd c:\Users\RANJITH\MrYoda
mvn test -DsuiteXmlFile="test-suites\testng_single_member_hybrid_cancellation.xml"
```

**Note:** 
- Use backslashes `\` for Windows file paths (or forward slashes will work too)
- Wrap the path in quotes if it contains spaces
- The parameter is `-DsuiteXmlFile=` (singular, not `suiteXmlFiles`)

**What happens:**
1. Phase 1 runs: Order created via UI (5-10 minutes)
2. Phase 2 runs: Cancellation flow executed via API (2-3 minutes)
3. Complete end-to-end cancellation validated

### Option 2: Run Cancellation Only (Standalone)

```bash
# Useful if you already have an order ID
mvn test -DsuiteXmlFile="test-suites\testng_single_member_cancellation.xml"
```

### Option 3: Run Specific Test Step

```bash
# Run approval step only
mvn test -Dtest=SingleMemberCancellationFlowTest#step05_ApproveCancellation
```

## Key Files

| File | Purpose | Phase |
|------|---------|-------|
| `testng_single_member_hybrid_cancellation.xml` | Combined suite | 1 + 2 |
| `testng_single_member_cancellation.xml` | Cancellation only | 2 |
| `TC_11_PayOnline_HybridFlow.feature` | Order creation | 1 |
| `SingleMemberCancellationFlowTest.java` | Cancellation flow | 2 |

## Configuration Needed

### config.properties

```properties
# Member
member.mobile=9003730394
member.password=1234

# Admin
admin.main.identifier=admin@example.com
admin.main.password=admin_password

# API
diagnostics.base.url=https://staging-api-diagnostics.yodaprojects.com
```

## Expected Flow

```
┌─────────────────────────────────────────────────────────┐
│ PHASE 1: ORDER CREATION (UI)                            │
├─────────────────────────────────────────────────────────┤
│ ✅ Login                                                │
│ ✅ Select Tests                                         │
│ ✅ Add to Cart                                          │
│ ✅ Checkout                                             │
│ ✅ Pay Online (Razorpay)                                │
│ ✅ Order Created: order_ABC123                          │
│ ✅ Stored in RequestContext                             │
└─────────────────────────────────────────────────────────┘
              │
              │ Order ID passed
              ▼
┌─────────────────────────────────────────────────────────┐
│ PHASE 2: CANCELLATION (API)                             │
├─────────────────────────────────────────────────────────┤
│ ✅ Member Login                                         │
│ ✅ Admin Login (Get admin GUID)                         │
│ ✅ Get Order from RequestContext                        │
│ ✅ Call Cashback API                                    │
│ ✅ Approve Cancellation                                 │
│ ✅ Verify Order Status = "Cancelled"                    │
│ ✅ Verify Payment Details                               │
│ ✅ Verify Transactions                                  │
│ ✅ Summary Report                                       │
└─────────────────────────────────────────────────────────┘
```

## Smart Order ID Handling

The cancellation test automatically:

1. **Checks** if order exists in RequestContext (from Phase 1)
2. **If YES** → Uses Phase 1 order ID
3. **If NO** → Creates new order or uses mock

This means:
- ✅ Run together: Phase 2 uses Phase 1 order
- ✅ Run separately: Phase 2 creates its own order
- ✅ Full flexibility for different test scenarios

## Example Console Output

### Start of Phase 1
```
==========================================================
      PHASE 1: UI HYBRID ORDER CREATION
==========================================================
Feature: Place lab orders and complete payment via UI

✅ Login successful
✅ Test selection complete
✅ Cart populated
✅ Checkout completed
✅ Payment processed
✅ Order created: order_ABC123XYZ
```

### Start of Phase 2
```
==========================================================
      STEP 3: GET/CREATE ORDER
==========================================================
✅ Order ID found from Phase 1 (Hybrid Automation)
   Order ID: order_ABC123XYZ
   Source: RequestContext (Phase 1 hybrid flow)
   Using existing order for cancellation flow

==========================================================
      STEP 5: APPROVE CANCELLED ORDER
==========================================================
✅ Cancellation Approved
```

### Final Summary
```
==========================================================
      CANCELLATION FLOW SUMMARY
==========================================================
✅ ALL TESTS PASSED - SINGLE MEMBER CANCELLATION FLOW

   Order Details:
      Order ID: order_ABC123XYZ
      Admin GUID: admin-guid-12345

   Flow Completed:
      ✅ Order Creation (Phase 1)
      ✅ Member Login (Phase 2)
      ✅ Admin Login (Phase 2)
      ✅ Cancellation Approval (Phase 2)
      ✅ Status Verification (Phase 2)
      ✅ Payment Verification (Phase 2)
      ✅ Transaction Verification (Phase 2)
```

## Troubleshooting

### Phase 1 Fails?
- Check credentials in config.properties
- Verify UI elements are accessible
- Check network connectivity

### Phase 2 Fails?
- Verify admin credentials
- Check order exists from Phase 1
- Review admin GUID extraction

### Order ID Not Passed?
- Check logs for "Order ID found from Phase 1"
- Verify Phase 1 completes before Phase 2
- Ensure RequestContext is not cleared between phases

## Files Created

```
✅ test-suites/testng_single_member_hybrid_cancellation.xml
   └─ Combined Phase 1 + Phase 2 suite

✅ Modified: SingleMemberCancellationFlowTest.java
   └─ Enhanced to detect Phase 1 order ID

✅ HYBRID_CANCELLATION_COMPLETE_GUIDE.md
   └─ Detailed architecture guide

✅ HYBRID_CANCELLATION_QUICK_START.md (this file)
   └─ Quick reference
```

## Command Reference

### Run Full Hybrid + Cancellation Suite
```bash
mvn test -DsuiteXmlFile="test-suites\testng_single_member_hybrid_cancellation.xml"
```

### Run Cancellation Only
```bash
mvn test -DsuiteXmlFile="test-suites\testng_single_member_cancellation.xml"
```

### Run Specific Step Only
```bash
mvn test -Dtest=SingleMemberCancellationFlowTest#step05_ApproveCancellation
```

## Next Steps

1. ✅ Ensure config.properties is updated
2. ✅ Copy and paste the correct command from above
3. ✅ Review test output
4. ✅ Verify cancellation completed successfully

## Support

- Detailed guide: [HYBRID_CANCELLATION_COMPLETE_GUIDE.md](HYBRID_CANCELLATION_COMPLETE_GUIDE.md)
- Cancellation API docs: [SINGLE_MEMBER_CANCELLATION_FLOW.md](docs/SINGLE_MEMBER_CANCELLATION_FLOW.md)
- Quick start (cancellation): [SINGLE_MEMBER_CANCELLATION_QUICK_START.md](SINGLE_MEMBER_CANCELLATION_QUICK_START.md)

---

**Compilation Status:** ✅ All code compiles successfully
