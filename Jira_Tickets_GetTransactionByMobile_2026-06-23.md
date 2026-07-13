================================================================================
JIRA TICKET 1: MOBILE NUMBER INPUT VALIDATION MISSING
================================================================================

Project:    MrYoda
Type:       Bug
Priority:   Critical
Component:  Membership API
Labels:     api-bug, input-validation, security
Sprint:     Current

Summary: GET /getTransactionByMobile/{mobile} - No mobile number validation (accepts any string, returns 200)

Description:
The API endpoint GET /membership/transaction/getTransactionByMobile/{mobileNumber} performs ZERO input validation on the mobile number path parameter. Any string - including alphabets, special characters, SQL injection payloads, empty values, and numbers outside the valid 8-13 digit range - is accepted with HTTP 200 and returns total: 0.

Expected Behavior:
- API should validate mobile number is numeric, 8-13 digits
- Invalid input should return HTTP 400 with appropriate error message

Actual Behavior:
- API returns HTTP 200 with total: 0 for ALL invalid inputs

Affected Test Cases (14 failures):
| TC   | Input                  | Expected | Actual |
|------|------------------------|----------|--------|
| TC14 | invalid_mobile         | 400      | 200    |
| TC15 | 1234567 (7 digits)     | 400      | 200    |
| TC16 | 12345678901234 (14 digits) | 400  | 200    |
| TC17 | abcdefghij             | 400      | 200    |
| TC18 | !@#$%^&*()            | 400      | 200    |
| TC19 | 805 647 7884 (spaces)  | 400      | 200    |
| TC20 | +918056477884 (country code) | 400 | 200   |
| TC21 | (empty string)         | 400      | 200    |
| TC22 | null (literal)         | 400      | 200    |
| TC23 | 8056477.884 (decimal)  | 400      | 200    |
| TC66 | abc123                 | 400      | 200    |
| TC68 | (empty)                | 400      | 200    |
| TC69 | null                   | 400      | 200    |
| TC70 | @#$%^&*()!            | 400      | 200    |
| TC71 | ABCDEFGHIJ             | 400      | 200    |
| TC72 | 805 647 7884           | 400      | 200    |
| TC96 | xyz123                 | 400      | 200    |

Security Impact:
- Enables enumeration attacks without rate limiting feedback
- No protection against malformed/malicious input at the API layer
- Relies solely on database lookup (no fail-fast behavior)

Steps to Reproduce:
curl -X GET "https://staging-api-diagnostics.yodaprojects.com/membership/transaction/getTransactionByMobile/INVALID_INPUT" -H "Authorization: Bearer <valid_token>"
Response: {"status":200,"success":true,"msg":"Transaction fetched successfully","total":0,"data":[]}

Suggested Fix:
Add request validation middleware:
1. Mobile is numeric only (no alphabets, special chars, spaces)
2. Mobile length is between 8-13 digits
3. No country code prefix (+91, 0, etc.)
4. Return 400: {"status":400,"success":false,"msg":"Invalid mobile number format"}

Environment: Staging
API: GET /membership/transaction/getTransactionByMobile/{mobileNumber}

================================================================================
JIRA TICKET 2: SQL INJECTION INPUT NOT REJECTED
================================================================================

Project:    MrYoda
Type:       Bug
Priority:   High
Component:  Membership API
Labels:     api-bug, security, input-validation
Sprint:     Current

Summary: GET /getTransactionByMobile/{mobile} - SQL injection payload returns 200 instead of 400

Description:
The API accepts SQL injection payloads like ' OR 1=1 -- without rejecting the input. While parameterized queries prevent actual data leakage (returns total: 0), the API should reject obviously malicious input at the validation layer.

Expected Behavior:
- API should return HTTP 400 for input containing SQL keywords/patterns
- Input validation should reject non-numeric characters before reaching the database

Actual Behavior:
- Returns HTTP 200 with total: 0
- No input sanitization or rejection

Affected Test Case:
| TC   | Input          | Expected | Actual |
|------|----------------|----------|--------|
| TC74 | ' OR 1=1 --    | 400      | 200    |

Note: Data is NOT leaked (parameterized queries work), but the lack of input rejection is a defense-in-depth failure.

Steps to Reproduce:
curl -X GET "https://staging-api-diagnostics.yodaprojects.com/membership/transaction/getTransactionByMobile/'%20OR%201%3D1%20--" -H "Authorization: Bearer <valid_token>"
Response: {"status":200,"success":true,"total":0,"data":[]}

Environment: Staging
API: GET /membership/transaction/getTransactionByMobile/{mobileNumber}

================================================================================
JIRA TICKET 3: RESPONSE DATA COUNT DOES NOT MATCH TOTAL
================================================================================

Project:    MrYoda
Type:       Bug
Priority:   Medium
Component:  Membership API
Labels:     api-bug, pagination, data-integrity
Sprint:     Current

Summary: GET /getTransactionByMobile/{mobile} - Response returns total: 1546 but only 10 records in data[]

Description:
The API response contains total: 1546 indicating total available records, but the data[] array only contains 10 records. The response also shows limit: 1546 and page: 1, suggesting it intends to return all records but silently truncates to 10.

Expected Behavior (Option A - Return All):
- If limit: 1546 and total: 1546, then data[] should contain all 1546 records

Expected Behavior (Option B - Paginate Correctly):
- If returning only 10, set limit: 10 in the response
- Provide accurate page, limit, and total_pages for proper pagination

Actual Behavior:
Response JSON:
{
  "total": 1546,
  "page": 1,
  "limit": 1546,
  "total_pages": 1,
  "data": [/* only 10 records */]
}

Affected Test Cases:
| TC   | Assertion          | Expected | Actual |
|------|--------------------|----------|--------|
| TC27 | data.size == total | 1546     | 10     |
| TC85 | data.size == total | 1546     | 10     |

Steps to Reproduce:
curl -X GET "https://staging-api-diagnostics.yodaprojects.com/membership/transaction/getTransactionByMobile/8056477884" -H "Authorization: Bearer <valid_token>"
Check: response.total = 1546, response.data.length = 10

Environment: Staging
API: GET /membership/transaction/getTransactionByMobile/{mobileNumber}

================================================================================
JIRA TICKET 4: TRANSACTION ID (trnsc_id) EMPTY FOR SOME RECORDS
================================================================================

Project:    MrYoda
Type:       Bug
Priority:   Medium
Component:  Membership API
Labels:     api-bug, data-integrity
Sprint:     Current

Summary: GET /getTransactionByMobile/{mobile} - trnsc_id is empty string for some transaction records

Description:
Some transaction records returned by the API have trnsc_id: "" (empty string). Transaction ID is a mandatory business field that should always be populated for completed transactions.

Expected Behavior:
- Every transaction record should have a non-empty trnsc_id
- If a transaction is created, it must have a unique identifier

Actual Behavior:
- Multiple records have "trnsc_id": ""
- These appear to be transactions where payment_mode is also null

Sample Record with Empty trnsc_id:
{
  "Guid": "83df55e5-d8ed-418e-b0da-3c62b3c7e9b6",
  "trnsc_id": "",
  "order_id": "01KTRRCG3TZ77Z4E2R4ZEQVYV4",
  "status": "Cancelled",
  "reference_code": "MY26AAA4505"
}

Affected Test Cases:
| TC    | Assertion          | Expected         | Actual |
|-------|--------------------|------------------|--------|
| TC57  | trnsc_id not empty | non-empty string | ""     |
| TC101 | trnsc_id not empty | non-empty string | ""     |

Questions for Dev Team:
1. Is trnsc_id generated by a payment gateway? If so, is it expected to be empty for cancelled/non-gateway transactions?
2. Should the API exclude these records or populate a system-generated ID?

Environment: Staging
API: GET /membership/transaction/getTransactionByMobile/{mobileNumber}

================================================================================
JIRA TICKET 5: API TIMEOUT ON CONSECUTIVE REQUESTS
================================================================================

Project:    MrYoda
Type:       Bug
Priority:   Low
Component:  Membership API
Labels:     api-bug, performance
Sprint:     Current

Summary: GET /getTransactionByMobile/{mobile} - Connection timeout (30s) on TC106 after ~100 API calls

Description:
Test case TC106 failed with a connection timeout after the test suite had already executed ~100 API calls. This suggests potential connection pool exhaustion or server-side rate limiting without proper error response.

Expected Behavior:
- API should respond within reasonable time (< 5s) or return 429 (rate limited)

Actual Behavior:
- java.net.SocketTimeoutException after 30s wait
- Occurred at RequestBuilder.getWithoutStatusCheck()

Affected Test Case:
| TC    | Issue                       |
|-------|-----------------------------|
| TC106 | Connection timeout after 30s |

Environment: Staging
API: GET /membership/transaction/getTransactionByMobile/{mobileNumber}

================================================================================
SUMMARY TABLE
================================================================================

| Ticket | Priority | Issue                                    | Failures |
|--------|----------|------------------------------------------|----------|
| 1      | Critical | No mobile number validation              | 14       |
| 2      | High     | SQL injection not rejected               | 1        |
| 3      | Medium   | Pagination mismatch (total vs data.size) | 2        |
| 4      | Medium   | Empty trnsc_id                           | 2        |
| 5      | Low      | Connection timeout                       | 1        |
| TOTAL  |          |                                          | 23       |

================================================================================
