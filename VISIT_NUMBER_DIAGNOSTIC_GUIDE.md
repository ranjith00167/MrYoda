# Visit Number Not Entering UI - Diagnostic Guide

## Current Status
✅ **Code compiled successfully**  
✅ **Enhanced debugging added to both API and UI layers**

## Issue Description
Visit numbers are not being passed from the **API flow** to the **UI automation flow** in the integrated test suite (`testng_member_cod_modular.xml`).

## Root Causes Investigation

### Potential Cause #1: API Flow Not Completing Before UI Flow
**Symptom**: Visit number is NULL when UI tries to retrieve it  
**Why**: The UI automation step executes before the API Payment Approval completes

**Solution**: Verify test execution order in TestNG XML
```xml
<!-- API tests MUST execute before UI tests -->
<test name="API Flow">
    <classes>
        <class name="...COD_01_LoginTest"/>
        <!-- ... other API tests ...-->
        <class name="...COD_08_ApprovePaymentTest"/> <!-- THIS MUST COMPLETE FIRST -->
    </classes>
</test>

<test name="UI Flow">
    <classes>
        <class name="...UI_SampleCollectionTest"/> <!-- Uses visit number -->
    </classes>
</test>
```

### Potential Cause #2: Visit Number Extraction Failure
**Symptom**: API logs show "🚨 FATAL: Could not extract ANY Visit Numbers!"  
**Why**: The API response structure doesn't match extraction logic

**What to check**:
1. Look for this log in API output:
   ```
   📊 Extracting and Mapping ALL Visit Numbers for ALL Orders...
   ```
2. Check if you see:
   ```
   ✅ Mapped Order: xxx → Visit: MYD12345
   ```
3. If NOT present, the extraction failed

**Solution**: The new multi-order extraction logic should handle this. Check API response structure.

### Potential Cause #3: Separate JVM Instances (MOST LIKELY)
**Symptom**: RequestContext has data in API logs but is empty in UI logs  
**Why**: TestNG might be running API and UI tests in separate JVM processes

**How to check**:
Run the test and look for these logs:

**In API logs** (near end of Payment Approval):
```
📊 Extracting and Mapping ALL Visit Numbers for ALL Orders...
   ✅ Mapped Order: guid-123 → Visit: MYD12345
   ✅ FINAL VISIT NUMBER (Primary): MYD12345
```

**In UI logs** (when entering visit number):
```
🔍 UI AUTOMATION: Retrieving Visit Number from RequestContext
📊 RequestContext State:
   • Primary Visit Number: null  <-- If this is NULL, separate JVM!
   • All Visit Numbers List: []
```

**Solution**: Ensure TestNG runs in a single JVM

Check `testng_member_cod_modular.xml`:
```xml
<!-- Ensure this is NOT set, or set to 1 -->
<suite name="Suite" parallel="false" thread-count="1">
```

### Potential Cause #4: RequestContext Not Being Populated
**Symptom**: API logs don't show visit number storage  
**Why**: `setCurrentVisitNumbers()` or `setVisitNumber()` not being called

**What to look for in API logs**:
```
>>> STORED VISIT NUMBER: MYD12345
```

If this line is MISSING, the RequestContext is not being populated.

## New Debug Output Explained

### API Side (CreateOrderCODAPITest.java)
When `callApprovePaymentAPI()` runs, you'll now see:
```
📊 Extracting and Mapping ALL Visit Numbers for ALL Orders...
   📋 Total Orders to Process: 2
   🔍 Processing data as List with 2 items...
      ✅ Mapped Order: order-guid-1 → Visit: MYD12345
      ✅ Mapped Order: order-guid-2 → Visit: MYD12346
   ✅ FINAL VISIT NUMBER (Primary): MYD12345
   ✅ ALL VISIT NUMBERS: [MYD12345, MYD12346]
   ✅ ORDER → VISIT MAPPING:
      • Order: order-guid-1 → Visit: MYD12345
      • Order: order-guid-2 → Visit: MYD12346
```

### UI Side (CodItDose.java)
When `i_enter_the_visit_number()` runs, you'll now see:
```
🔍 UI AUTOMATION: Retrieving Visit Number from RequestContext
📊 RequestContext State:
   • Primary Visit Number: MYD12345
   • All Visit Numbers List: [MYD12345, MYD12346]
   • Visit Numbers Count: 2
   • Order IDs: [order-guid-1, order-guid-2]
   • Payment ID: payment-guid-xyz
   • Order-Visit Map Size: 2
   • Order-Visit Mappings:
      - Order: order-guid-1 → Visit: MYD12345
      - Order: order-guid-2 → Visit: MYD12346
✅ Visit Number Retrieved: MYD12345
📝 Entering Visit Number into UI search field...
✅ Visit Number entered successfully
```

## Diagnostic Steps

### Step 1: Run the Test
```bash
cd c:\Users\RANJITH\MrYoda
mvn test "-DsuiteXmlFile=testng_member_cod_modular.xml"
```

### Step 2: Check Console Output
Look for the debug sections mentioned above.

### Step 3: Identify the Issue

**Scenario A**: API logs show visit numbers extracted, UI logs show NULL
→ **Separate JVM issue** - Fix TestNG configuration

**Scenario B**: API logs show "Could not extract ANY Visit Numbers"
→ **Extraction failure** - Check API response structure

**Scenario C**: API logs don't reach the extraction step
→ **API flow failed before Payment Approval** - Check earlier test failures

### Step 4: Apply the Fix

**For Separate JVM Issue**:
Edit `testng_member_cod_modular.xml`:
```xml
<suite name="Member COD ModularSuite" parallel="false">
  <!-- Ensure one test thread -->
  <test name="Complete COD Flow">
    <!-- All tests in ONE test block -->
  </test>
</suite>
```

**For Extraction Failure**:
Check the API response body in logs and verify the structure matches our extraction logic.

## Test Files Modified

1. **CreateOrderCODAPITest.java**
   - `callApprovePaymentAPI()` - Multi-order visit extraction
   - `extractAllVisitsFromResponse()` - NEW
   - `extractVisitFromMap()` - NEW
   - `extractSingleVisitFromResponse()` - NEW

2. **CodItDose.java**
   - `i_enter_the_visit_number()` - Enhanced debug logging

3. **RequestContext.java**
   - Already has all required methods (lines 44-92)

## Next Steps

1. ✅ Code is compiled and ready
2. 🔄 Run the test suite
3. 📋 Collect the debug output
4. 🔍 Share the console logs showing:
   - API: "📊 Extracting and Mapping..." section
   - UI: "🔍 UI AUTOMATION: Retrieving..." section
5. 🛠️ We'll identify the exact issue from the logs

## Expected Successful Output

**API Side**:
```
✅ Payment Approved Successfully
📊 Extracting and Mapping ALL Visit Numbers for ALL Orders...
   ✅ FINAL VISIT NUMBER (Primary): MYD12345
```

**UI Side**:
```
🔍 UI AUTOMATION: Retrieving Visit Number from RequestContext
   • Primary Visit Number: MYD12345  <-- Must NOT be null
✅ Visit Number Retrieved: MYD12345
✅ Visit Number entered successfully
```

If you see both sections with valid visit numbers, the issue is **FIXED** ✅
