# Quick Reference: Centralized Failure Logging

## At a Glance

**Where:** `logs/COD_15_17_Failures.log` (centralized log for all COD failures)

**What's Logged:** 
- ❌ COD_17: Report URL missing, PDF extraction failures, incomplete sync
- ❌ COD_15: Payment approval failures with admin token & payment details  
- ❌ UI Steps: Approval button failures, test entry errors, visit processing failures

## Components & Their Logging

### 1. COD_17_ReportGenerationTest
**Failure Logging Method:** `logCODFailure(testName, failureType, errorMessage, exception)`

```java
// When report URL is null
logCODFailure("COD_17_ReportGeneration", "REPORT_URL_NULL", 
    "Report generation URL not available for visit: " + visitNumber,
    new Exception("Backend report generation lag detected"));

// When sync incomplete
logCODFailure("COD_17_ReportGeneration", "INCOMPLETE_SYNC",
    "Report sync incomplete: " + synced + "/" + total + " tests synced",
    new Exception("Backend sync lag exhausted"));

// When PDF extraction fails
logCODFailure("COD_17_ReportGeneration", "PDF_EXTRACTION_FAILED",
    "PDF text extraction returned null or empty",
    new Exception("PDF extraction failed"));
```

### 2. CreateOrderCODAPITest  
**Failure Logging Method:** `logFailureToCommonLog(testName, failureType, errorMessage, details)`

```java
// When payment approval fails
String failureDetails = "Payment ID: " + paymentId + " | Status: " + statusCode + 
                       " | Admin Token Valid: " + tokenValid + 
                       " | Payable Amount: ₹" + amount;
logFailureToCommonLog("COD_15_ApprovePayment", "PAYMENT_APPROVAL_FAILED",
    "Approve Payment API returned non-200/201 status", failureDetails);
```

### 3. CodItDose (UI Automation)
**Failure Logging Method:** `logUIFailure(stepName, failureType, errorMessage)`

```java
// When approval click fails
logUIFailure("COD_Approval", "APPROVE_BUTTON_CLICKED_FAILED",
    "Could not confirm approve button click after 3 attempts");

// When visit processing fails
logUIFailure("COD_EntryValues", "VISIT_PROCESSING_FAILURE",
    "Error processing visit " + visitId + ": " + exceptionType);

// When no visits processed
logUIFailure("COD_EntryValues", "NO_VISITS_PROCESSED",
    "Failed to process any visits. SIN: " + sinNo);
```

## Reading Failure Logs

### Sample Log Entry:
```
[2024-01-15 10:30:45.123] TEST: COD_17_ReportGeneration | TYPE: REPORT_URL_NULL | ERROR: Report generation URL not available for visit: V001 | CAUSE: Exception - Backend lag
```

### Breaking It Down:
- `[2024-01-15 10:30:45.123]` = When it happened
- `TEST: COD_17_ReportGeneration` = Which component
- `TYPE: REPORT_URL_NULL` = Category of failure
- `ERROR: ...` = What went wrong
- `CAUSE: ...` = Why it happened

## Failure Types by Component

### COD_17 Report Generation
| Type | Meaning | Action |
|------|---------|--------|
| REPORT_URL_NULL | Report not ready | Backend lag, wait longer |
| INCOMPLETE_SYNC | Tests not synced | API delay, extend retries |
| PDF_EXTRACTION_FAILED | PDF parsing failed | S3 issue or PDF corrupted |

### COD_15 Payment Approval  
| Type | Meaning | Action |
|------|---------|--------|
| PAYMENT_APPROVAL_FAILED | API returned non-200/201 | Check admin token or payment data |

### UI Automation
| Type | Meaning | Action |
|------|---------|--------|
| APPROVE_BUTTON_CLICKED_FAILED | Click failed after retries | Check element visibility |
| VISIT_PROCESSING_FAILURE | Visit processing error | Check page state or locators |
| NO_VISITS_PROCESSED | No visits completed | Check SIN extraction/search |

## How to Use Failure Logs

### Check Recent Failures:
```bash
# On Windows PowerShell
Get-Content logs/COD_15_17_Failures.log -Tail 20
```

### Filter by Failure Type:
```bash
# Find all PDF extraction failures
Select-String "PDF_EXTRACTION_FAILED" logs/COD_15_17_Failures.log
```

### Find Payment Approval Failures:
```bash
# Find all payment approval issues
Select-String "PAYMENT_APPROVAL_FAILED" logs/COD_15_17_Failures.log
```

### Count Failure Types:
```bash
# See how many of each failure type
Get-Content logs/COD_15_17_Failures.log | 
    Select-String "TYPE:" -AllMatches | 
    Group-Object { $_.Matches[0].Value }
```

## Integration Points

### ✅ Automatically Captured By:
1. `testGetReportAndVerifyPDF()` - All PDF & sync failures
2. `callApprovePaymentAPI()` - Payment approval failures
3. `i_click_on_the_approve_button()` - Approval UI failures
4. `i_enter_the_value_of_the_tests()` - Test entry failures

### ✅ Logged To:
- File: `logs/COD_15_17_Failures.log`
- Console: Via `LoggerUtil.error()` or `System.err.println()`
- Both locations simultaneously

## Troubleshooting

### Q: Why is my log empty?
**A:** Tests need to fail to generate log entries. Run a test with expected failures.

### Q: Can I clear old failures?
**A:** Yes, delete `logs/COD_15_17_Failures.log` and it will be recreated on next failure.

### Q: How often is the log written?
**A:** The log is appended to immediately when any failure occurs, so you can monitor it in real-time.

### Q: What if FileWriter fails?
**A:** Errors writing to the log are printed to console. Check file permissions if logging fails.

## Performance Impact

✅ **Minimal:** 
- Failure logging only writes during failures (not on success)
- Each failure adds ~150-300 bytes to log file
- I/O is fast (FileWriter with append mode)

**Example:** 100 test failures = ~30-50 KB log file

---

**Created:** Implementation complete, ready for testing
**Last Updated:** 2024-01-15
**Status:** ✅ Deployed and compiled
