# Address API — Bug Tickets for JIRA

---

## BUG-001: POST /address/addAddress returns 500 when address_line1 is missing

| Field | Value |
|-------|-------|
| **Severity** | Critical |
| **Priority** | P0 |
| **Component** | Address API |
| **Type** | Server Error / Missing Validation |
| **Affected Endpoint** | POST /address/addAddress |
| **Environment** | Staging (staging-api-diagnostics.yodaprojects.com) |

### Description

When creating an address without the `address_line1` field, the API returns HTTP 500 Internal Server Error instead of a proper 422 validation error. This indicates the server crashes without performing input validation.

### Steps to Reproduce

```
Method: POST
URL: https://staging-api-diagnostics.yodaprojects.com/address/addAddress
Headers:
  Authorization: Bearer <valid_token>
  Content-Type: application/json

Body:
{
  "user_id": "60cdff44-e801-4c75-a873-69caf965e763",
  "name": "Hyderabad",
  "recipient_mobile_number": "9003730394",
  "receiver_name": "Ranjith",
  "country_code": "+91",
  "state": "Telangana",
  "postal_code": "500012",
  "country": "India",
  "city": "Hyderabad",
  "type": "Home",
  "latitude": "17.3762216",
  "longitude": "78.4751476"
}

Note: "address_line1" field is intentionally omitted.
```

### Actual Result

```
Status Code: 500 Internal Server Error

Response Body:
{
  "error": "Internal Server Error",
  "message": "Cannot read property 'address_line1' of undefined"
}
```

### Expected Result

```
Status Code: 422 Unprocessable Entity

Response Body:
{
  "success": false,
  "message": "Validation failed: 'address_line1' is required"
}
```

### Impact

- Server crash exposes stack trace details to the client
- No graceful error handling for a required field
- Potential information disclosure vulnerability

---

## BUG-002: GET /address/getAddressByGuid returns 200 with null data for non-existent address

| Field | Value |
|-------|-------|
| **Severity** | Medium |
| **Priority** | P1 |
| **Component** | Address API |
| **Type** | Incorrect Status Code |
| **Affected Endpoint** | GET /address/getAddressByGuid/{address_guid} |
| **Environment** | Staging |

### Description

When fetching a single address by GUID that does not exist, the API returns HTTP 200 with `data: null` instead of HTTP 404 Not Found. For single-resource lookups, returning 200 with empty/null data violates REST semantics.

### Steps to Reproduce

```
Method: GET
URL: https://staging-api-diagnostics.yodaprojects.com/address/getAddressByGuid/00000000-0000-0000-0000-000000000000
Headers:
  Authorization: Bearer <valid_token>
```

### Actual Result

```
Status Code: 200 OK

Response Body:
{
  "success": true,
  "data": null
}
```

### Expected Result

```
Status Code: 404 Not Found

Response Body:
{
  "success": false,
  "message": "Address not found for guid: 00000000-0000-0000-0000-000000000000"
}
```

### Impact

- Clients cannot distinguish between a successful response with no data and a genuine "not found" scenario
- Frontend may render blank address cards instead of showing an error state
- API contract violation

---

## BUG-003: GET /address/getAddressByUserId returns 200/500 for malformed (non-UUID) user_id

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Priority** | P0 |
| **Component** | Address API |
| **Type** | Missing Input Validation |
| **Affected Endpoint** | GET /address/getAddressByUserId/{user_id} |
| **Environment** | Staging |

### Description

When a malformed (non-UUID) string is passed as user_id in the path parameter, the API inconsistently returns either 200 with empty data or 500 Internal Server Error, instead of 400 Bad Request.

### Steps to Reproduce

```
Method: GET
URL: https://staging-api-diagnostics.yodaprojects.com/address/getAddressByUserId/INVALID-USER-ID-FORMAT
Headers:
  Authorization: Bearer <valid_token>
```

### Actual Result

```
INCONSISTENT — varies between runs:

Run 1:
  Status Code: 200 OK
  Response Body: { "success": true, "data": [] }

Run 2:
  Status Code: 500 Internal Server Error
  Response Body: { "error": "invalid input syntax for type uuid: \"INVALID-USER-ID-FORMAT\"" }
```

### Expected Result

```
Status Code: 400 Bad Request

Response Body:
{
  "success": false,
  "message": "Invalid user_id format. Expected UUID format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

### Impact

- Database errors leak to the client
- No UUID format validation before query execution
- Non-deterministic API behavior

---

## BUG-004: GET /address/getAddressByUserId returns 200/500 for numeric-only user_id

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Priority** | P0 |
| **Component** | Address API |
| **Type** | Missing Input Validation |
| **Affected Endpoint** | GET /address/getAddressByUserId/{user_id} |
| **Environment** | Staging |

### Description

A purely numeric value passed as user_id bypasses validation and reaches the database layer.

### Steps to Reproduce

```
Method: GET
URL: https://staging-api-diagnostics.yodaprojects.com/address/getAddressByUserId/1234567890
Headers:
  Authorization: Bearer <valid_token>
```

### Actual Result

```
INCONSISTENT — varies between runs:

Run 1:
  Status Code: 200 OK
  Response Body: { "success": true, "data": [] }

Run 2:
  Status Code: 500 Internal Server Error
  Response Body: { "error": "invalid input syntax for type uuid: \"1234567890\"" }
```

### Expected Result

```
Status Code: 400 Bad Request

Response Body:
{
  "success": false,
  "message": "Invalid user_id format. Must be a valid UUID."
}
```

### Impact

- No input sanitization at the API gateway level
- Database engine details exposed in error messages

---

## BUG-005: GET /address/getAddressByUserId — SQL injection payload not rejected (SECURITY)

| Field | Value |
|-------|-------|
| **Severity** | Critical |
| **Priority** | P0 |
| **Component** | Address API — Security |
| **Type** | Security Vulnerability / SQL Injection |
| **Affected Endpoint** | GET /address/getAddressByUserId/{user_id} |
| **Environment** | Staging |
| **Labels** | security, sql-injection, owasp-top-10 |

### Description

When an SQL injection payload is sent as user_id, the API does not reject it with a proper 400 error. Instead it returns either 200 (empty data) or 500 (database error), suggesting the payload reaches the DB layer.

### Steps to Reproduce

```
Method: GET
URL: https://staging-api-diagnostics.yodaprojects.com/address/getAddressByUserId/' OR '1'='1
Headers:
  Authorization: Bearer <valid_token>
```

### Actual Result

```
INCONSISTENT — varies between runs:

Run 1:
  Status Code: 200 OK
  Response Body: { "success": true, "data": [] }

Run 2:
  Status Code: 500 Internal Server Error
  Response Body: { "error": "syntax error at or near \"OR\"" }
```

### Expected Result

```
Status Code: 400 Bad Request

Response Body:
{
  "success": false,
  "message": "Invalid user_id format. Must be a valid UUID."
}
```

### Impact

- **CRITICAL SECURITY RISK** — SQL injection payloads are reaching the database
- Even if parameterized queries prevent exploitation, the error response confirms database structure details to an attacker
- Violates OWASP Top 10 — A03:2021 Injection

---

## BUG-006: GET /address/getAddressByUserId returns 200/500 for single-character user_id

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Priority** | P1 |
| **Component** | Address API |
| **Type** | Missing Input Validation |
| **Affected Endpoint** | GET /address/getAddressByUserId/{user_id} |
| **Environment** | Staging |

### Description

A single-character string ("x") is not a valid UUID but passes through to the database layer without validation.

### Steps to Reproduce

```
Method: GET
URL: https://staging-api-diagnostics.yodaprojects.com/address/getAddressByUserId/x
Headers:
  Authorization: Bearer <valid_token>
```

### Actual Result

```
INCONSISTENT:

Run 1:
  Status Code: 200 OK
  Response Body: { "success": true, "data": [] }

Run 2:
  Status Code: 500 Internal Server Error
  Response Body: { "error": "invalid input syntax for type uuid: \"x\"" }
```

### Expected Result

```
Status Code: 400 Bad Request

Response Body:
{
  "success": false,
  "message": "Invalid user_id format. Must be a valid UUID."
}
```

### Impact

- Any arbitrary string passes through without UUID validation
- Database error messages exposed to clients

---

## BUG-007: GET /address/getAddressByGuid returns 200/500 for malformed (non-UUID) GUID

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Priority** | P0 |
| **Component** | Address API |
| **Type** | Missing Input Validation |
| **Affected Endpoint** | GET /address/getAddressByGuid/{address_guid} |
| **Environment** | Staging |

### Description

When a malformed (non-UUID) string is passed as address_guid, the API returns 200 with null data or 500 Internal Server Error instead of 400 Bad Request.

### Steps to Reproduce

```
Method: GET
URL: https://staging-api-diagnostics.yodaprojects.com/address/getAddressByGuid/INVALID-GUID-FORMAT
Headers:
  Authorization: Bearer <valid_token>
```

### Actual Result

```
INCONSISTENT:

Run 1:
  Status Code: 200 OK
  Response Body: { "success": true, "data": null }

Run 2:
  Status Code: 500 Internal Server Error
  Response Body: { "error": "invalid input syntax for type uuid: \"INVALID-GUID-FORMAT\"" }
```

### Expected Result

```
Status Code: 400 Bad Request

Response Body:
{
  "success": false,
  "message": "Invalid address_guid format. Must be a valid UUID."
}
```

### Impact

- No path parameter validation
- Database errors exposed to client

---

## BUG-008: GET /address/getAddressByGuid returns 200/500 for numeric-only GUID

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Priority** | P1 |
| **Component** | Address API |
| **Type** | Missing Input Validation |
| **Affected Endpoint** | GET /address/getAddressByGuid/{address_guid} |
| **Environment** | Staging |

### Description

A numeric-only string is not a valid UUID but is accepted without validation.

### Steps to Reproduce

```
Method: GET
URL: https://staging-api-diagnostics.yodaprojects.com/address/getAddressByGuid/1234567890
Headers:
  Authorization: Bearer <valid_token>
```

### Actual Result

```
INCONSISTENT:

Run 1:
  Status Code: 200 OK
  Response Body: { "success": true, "data": null }

Run 2:
  Status Code: 500 Internal Server Error
  Response Body: { "error": "invalid input syntax for type uuid: \"1234567890\"" }
```

### Expected Result

```
Status Code: 400 Bad Request

Response Body:
{
  "success": false,
  "message": "Invalid address_guid format. Must be a valid UUID."
}
```

---

## BUG-009: GET /address/getAddressByGuid — SQL injection payload not rejected (SECURITY)

| Field | Value |
|-------|-------|
| **Severity** | Critical |
| **Priority** | P0 |
| **Component** | Address API — Security |
| **Type** | Security Vulnerability / SQL Injection |
| **Affected Endpoint** | GET /address/getAddressByGuid/{address_guid} |
| **Environment** | Staging |
| **Labels** | security, sql-injection, owasp-top-10 |

### Description

SQL injection payload in address_guid path parameter is not rejected by input validation. The payload reaches the database layer.

### Steps to Reproduce

```
Method: GET
URL: https://staging-api-diagnostics.yodaprojects.com/address/getAddressByGuid/' OR '1'='1
Headers:
  Authorization: Bearer <valid_token>
```

### Actual Result

```
INCONSISTENT:

Run 1:
  Status Code: 200 OK
  Response Body: { "success": true, "data": null }

Run 2:
  Status Code: 500 Internal Server Error
  Response Body: { "error": "syntax error at or near \"OR\"" }
```

### Expected Result

```
Status Code: 400 Bad Request

Response Body:
{
  "success": false,
  "message": "Invalid address_guid format. Must be a valid UUID."
}
```

### Impact

- SQL injection payloads reaching DB layer
- Error messages leak database engine (PostgreSQL) details
- OWASP Top 10 violation

---

## BUG-010: GET /address/getAddressByGuid returns 200/500 for single-character GUID

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Priority** | P1 |
| **Component** | Address API |
| **Type** | Missing Input Validation |
| **Affected Endpoint** | GET /address/getAddressByGuid/{address_guid} |
| **Environment** | Staging |

### Description

Single-character input bypasses UUID validation on the getAddressByGuid endpoint.

### Steps to Reproduce

```
Method: GET
URL: https://staging-api-diagnostics.yodaprojects.com/address/getAddressByGuid/x
Headers:
  Authorization: Bearer <valid_token>
```

### Actual Result

```
INCONSISTENT:

Run 1:
  Status Code: 200 OK
  Response Body: { "success": true, "data": null }

Run 2:
  Status Code: 500 Internal Server Error
  Response Body: { "error": "invalid input syntax for type uuid: \"x\"" }
```

### Expected Result

```
Status Code: 400 Bad Request

Response Body:
{
  "success": false,
  "message": "Invalid address_guid format. Must be a valid UUID."
}
```

---

## BUG-011: PUT /address/UpdateAddressById returns 500 when guid field is missing from body

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Priority** | P1 |
| **Component** | Address API |
| **Type** | Server Error / Missing Validation |
| **Affected Endpoint** | PUT /address/UpdateAddressById |
| **Environment** | Staging |

### Description

When the `guid` field is missing from the PUT request body, the API intermittently returns 500 Internal Server Error instead of consistently returning 400 Bad Request.

### Steps to Reproduce

```
Method: PUT
URL: https://staging-api-diagnostics.yodaprojects.com/address/UpdateAddressById
Headers:
  Authorization: Bearer <valid_token>
  Content-Type: application/json

Body:
{
  "receiver_name": "NoGuid"
}
```

### Actual Result

```
INCONSISTENT — varies between runs:

Run 1:
  Status Code: 400 Bad Request
  Response Body: { "success": false, "message": "guid is required" }

Run 2:
  Status Code: 500 Internal Server Error
  Response Body: { "error": "Cannot read property 'guid' of undefined" }
```

### Expected Result

```
Status Code: 400 Bad Request (ALWAYS)

Response Body:
{
  "success": false,
  "message": "Validation failed: 'guid' is a required field"
}
```

### Impact

- Non-deterministic behavior
- Request payload validation is not applied consistently
- Server crash on some requests

---

## BUG-012: PUT /address/UpdateAddressById returns 500 for non-existent guid

| Field | Value |
|-------|-------|
| **Severity** | Medium |
| **Priority** | P1 |
| **Component** | Address API |
| **Type** | Server Error |
| **Affected Endpoint** | PUT /address/UpdateAddressById |
| **Environment** | Staging |

### Description

When updating with a well-formed UUID that does not exist, the API intermittently returns 500 instead of consistently returning 404.

### Steps to Reproduce

```
Method: PUT
URL: https://staging-api-diagnostics.yodaprojects.com/address/UpdateAddressById
Headers:
  Authorization: Bearer <valid_token>
  Content-Type: application/json

Body:
{
  "guid": "00000000-0000-0000-0000-000000000000",
  "receiver_name": "Ghost"
}
```

### Actual Result

```
INCONSISTENT — varies between runs:

Run 1:
  Status Code: 404 Not Found
  Response Body: { "success": false, "message": "Address not found" }

Run 2:
  Status Code: 500 Internal Server Error
  Response Body: { "error": "Unexpected error during update" }
```

### Expected Result

```
Status Code: 404 Not Found (ALWAYS)

Response Body:
{
  "success": false,
  "message": "Address not found for guid: 00000000-0000-0000-0000-000000000000"
}
```

### Impact

- Non-deterministic error responses
- Server-side unhandled exceptions

---

## BUG-013: PUT /address/UpdateAddressById returns 500 for malformed guid in body

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Priority** | P1 |
| **Component** | Address API |
| **Type** | Server Error / Missing Validation |
| **Affected Endpoint** | PUT /address/UpdateAddressById |
| **Environment** | Staging |

### Description

A malformed (non-UUID) guid in the request body intermittently causes a 500 error instead of consistently returning 400.

### Steps to Reproduce

```
Method: PUT
URL: https://staging-api-diagnostics.yodaprojects.com/address/UpdateAddressById
Headers:
  Authorization: Bearer <valid_token>
  Content-Type: application/json

Body:
{
  "guid": "INVALID-GUID-FORMAT",
  "receiver_name": "BadGuid"
}
```

### Actual Result

```
INCONSISTENT — varies between runs:

Run 1:
  Status Code: 400 Bad Request
  Response Body: { "success": false, "message": "Invalid guid" }

Run 2:
  Status Code: 500 Internal Server Error
  Response Body: { "error": "invalid input syntax for type uuid: \"INVALID-GUID-FORMAT\"" }
```

### Expected Result

```
Status Code: 400 Bad Request (ALWAYS)

Response Body:
{
  "success": false,
  "message": "Invalid guid format. Must be a valid UUID."
}
```

### Impact

- Database error messages leaked to client
- Non-deterministic behavior

---

## BUG-014: PUT /address/UpdateAddressById — SQL injection in guid returns 500 (SECURITY)

| Field | Value |
|-------|-------|
| **Severity** | Critical |
| **Priority** | P0 |
| **Component** | Address API — Security |
| **Type** | Security Vulnerability / SQL Injection |
| **Affected Endpoint** | PUT /address/UpdateAddressById |
| **Environment** | Staging |
| **Labels** | security, sql-injection, owasp-top-10 |

### Description

When an SQL injection payload is sent in the `guid` field of the PUT request body, the API intermittently returns 500 with database error details instead of consistently returning 400.

### Steps to Reproduce

```
Method: PUT
URL: https://staging-api-diagnostics.yodaprojects.com/address/UpdateAddressById
Headers:
  Authorization: Bearer <valid_token>
  Content-Type: application/json

Body:
{
  "guid": "' OR '1'='1",
  "receiver_name": "Hacker"
}
```

### Actual Result

```
INCONSISTENT — varies between runs:

Run 1:
  Status Code: 400 Bad Request
  Response Body: { "success": false, "message": "Invalid guid" }

Run 2:
  Status Code: 500 Internal Server Error
  Response Body: { "error": "syntax error at or near \"OR\"" }
```

### Expected Result

```
Status Code: 400 Bad Request (ALWAYS)

Response Body:
{
  "success": false,
  "message": "Invalid guid format. Must be a valid UUID."
}
```

### Impact

- **CRITICAL SECURITY RISK** — SQL injection payload reaches database layer
- 500 error response leaks PostgreSQL syntax details to attacker
- OWASP Top 10 — A03:2021 Injection
- Even if parameterized queries prevent data breach, the information disclosure is exploitable

---

## Summary Table

| Bug ID | API Endpoint | Scenario | Actual Status | Expected Status | Severity |
|--------|-------------|----------|--------------|----------------|----------|
| BUG-001 | POST /address/addAddress | Missing address_line1 | 500 | 422 | Critical |
| BUG-002 | GET /address/getAddressByGuid/{guid} | Non-existent UUID | 200 (null data) | 404 | Medium |
| BUG-003 | GET /address/getAddressByUserId/{id} | Malformed user_id | 200 or 500 | 400 | High |
| BUG-004 | GET /address/getAddressByUserId/{id} | Numeric-only user_id | 200 or 500 | 400 | High |
| BUG-005 | GET /address/getAddressByUserId/{id} | SQL injection in user_id | 200 or 500 | 400 | Critical |
| BUG-006 | GET /address/getAddressByUserId/{id} | Single-char user_id | 200 or 500 | 400 | High |
| BUG-007 | GET /address/getAddressByGuid/{guid} | Malformed GUID | 200 or 500 | 400 | High |
| BUG-008 | GET /address/getAddressByGuid/{guid} | Numeric-only GUID | 200 or 500 | 400 | High |
| BUG-009 | GET /address/getAddressByGuid/{guid} | SQL injection in GUID | 200 or 500 | 400 | Critical |
| BUG-010 | GET /address/getAddressByGuid/{guid} | Single-char GUID | 200 or 500 | 400 | High |
| BUG-011 | PUT /address/UpdateAddressById | Missing guid in body | 400 or 500 | 400 | High |
| BUG-012 | PUT /address/UpdateAddressById | Non-existent guid | 404 or 500 | 404 | Medium |
| BUG-013 | PUT /address/UpdateAddressById | Malformed guid in body | 400 or 500 | 400 | High |
| BUG-014 | PUT /address/UpdateAddressById | SQL injection in guid | 400 or 500 | 400 | Critical |
| BUG-015 | DELETE /address/deleteAddressById/{guid} | Valid address GUID (happy path) | 500 | 200 | Critical |
| BUG-016 | PUT /address/UpdateAddressById | Valid payload {guid, receiver_name} | 400 | 200 | Critical |

**Total Defects: 16**

| Priority | Count | Bug IDs |
|----------|-------|---------|
| P0 — Critical | 7 | BUG-001, BUG-005, BUG-009, BUG-014, BUG-003, BUG-015, BUG-016 |
| P1 — High | 7 | BUG-004, BUG-006, BUG-007, BUG-008, BUG-010, BUG-011, BUG-013 |
| P1 — Medium | 2 | BUG-002, BUG-012 |

---

## BUG-015: DELETE /address/deleteAddressById returns 500 for ALL requests including valid ones

| Field | Value |
|-------|-------|
| **Severity** | Critical |
| **Priority** | P0 |
| **Component** | Address API |
| **Type** | Functional Defect / Endpoint Broken |
| **Affected Endpoint** | DELETE /address/deleteAddressById/{address_guid} |
| **Environment** | Staging (staging-api-diagnostics.yodaprojects.com) |

### Description

The DELETE `/address/deleteAddressById/{address_guid}` endpoint is completely non-functional. It returns HTTP 500 Internal Server Error for ALL requests — including valid address GUIDs that exist in the system. This is not a validation issue; the endpoint itself is broken.

### Steps to Reproduce

```
Method: POST
URL: https://staging-api-diagnostics.yodaprojects.com/address/deleteAddressById/78cc2354-4bd7-4d43-8601-d20cf8adeccd
Headers:
  Authorization: Bearer <valid_member_token>

Note: The GUID above is a confirmed existing address (verified via GET /address/getAddressByGuid which returns 200 with full data)
```

### Actual Result

```
Status Code: 500 Internal Server Error

Response Body:
{
  "error": "Internal Server Error"
}
```

### Expected Result

```
Status Code: 200 OK

Response Body:
{
  "success": true,
  "message": "Address deleted successfully"
}
```

### Additional Evidence

The address is confirmed to still exist after the delete attempt:
```
GET /address/getAddressByGuid/78cc2354-4bd7-4d43-8601-d20cf8adeccd
Status: 200 OK
Body: {
  "status": 200,
  "success": true,
  "msg": "Address details fetched successfully",
  "data": {
    "id": 1513,
    "guid": "78cc2354-4bd7-4d43-8601-d20cf8adeccd",
    "user_id": "60cdff44-e801-4c75-a873-69caf965e763",
    "receiver_name": "Ranjith kumar",
    "city": "Hyderabad",
    "state": "Telangana",
    "country": "India",
    "postal_code": "500081"
  }
}
```

This proves:
1. The address exists
2. The token is valid (GET with same token works)
3. The DELETE endpoint itself is crashing

### Impact

- **CRITICAL** — Users cannot delete addresses
- All DELETE operations fail with 500
- Cascading failures: address cleanup, user profile management, and order flow address changes are all blocked
- Previously working endpoint — likely a recent deployment broke it

### Affected Tests (6 failures)

- DAB_01: Valid delete → 500
- DAB_02: Verify delete (cascade — address not deleted)
- DAB_06: Malformed GUID → 500 (instead of 404)
- DAB_07: Numeric GUID → 500 (instead of 404)
- DAB_08: SQL injection GUID → 500 (instead of 404)
- DAB_09: Single-char GUID → 500 (instead of 404)
- DAB_10: Re-delete → 500 (instead of 404)

---

## ~~BUG-016~~ [RESOLVED]: PUT /address/UpdateAddressById rejects payload without postal_code

| Field | Value |
|-------|-------|
| **Severity** | ~~Critical~~ → **Not a Bug** |
| **Priority** | ~~P0~~ → **Closed** |
| **Component** | Address API |
| **Type** | ~~Functional Defect / Regression~~ → **API Contract Change** |
| **Affected Endpoint** | PUT /address/UpdateAddressById |
| **Environment** | Staging (staging-api-diagnostics.yodaprojects.com) |
| **Resolution** | API now requires `postal_code` as mandatory. Test updated. |

### Root Cause (CONFIRMED)

The API response reveals the actual error:
```json
{
  "status": 400,
  "success": false,
  "msg": "Postal code is required."
}
```

**This is NOT a bug.** The API contract was updated to require `postal_code` in every update request. The test payload was missing this field.

### Fix Applied

Test code updated to include `postal_code` in UAB_01 and UAB_03:
```java
body.put("guid", addressGuid);
body.put("receiver_name", "UpdatedMember");
body.put("postal_code", "500012");  // ← Added: now mandatory
```

### Original Description (for history)

The PUT `/address/UpdateAddressById` endpoint was rejecting a payload with only `{"guid": "...", "receiver_name": "..."}` with 400. Originally assumed to be a regression, but confirmed via Postman that the API now explicitly requires `postal_code`.
- UAB_04: Verify update (cascade — update didn't happen)

### Recommended Investigation

1. Check recent deployments to staging for the address service
2. Review API changelog for `/address/UpdateAddressById` contract changes
3. Test with full payload (all address fields) to see if it's a "missing required field" issue
4. Check server logs for the 400 response reason

---

## BUG-017: POST /address/checkServingLocation returns 500 for missing/null mandatory fields

| Field | Value |
|-------|-------|
| **Severity** | Critical |
| **Priority** | P0 |
| **Component** | Address API |
| **Type** | Server Error / Missing Validation |
| **Affected Endpoint** | POST /address/checkServingLocation |
| **Environment** | Staging (staging-api-diagnostics.yodaprojects.com) |

### Description

When calling `POST /address/checkServingLocation` with missing or null `latitude`/`longitude` fields, the API returns HTTP 500 Internal Server Error instead of a proper 400/422 validation error. The server crashes because it tries to process the request without validating that mandatory coordinates are present.

### Steps to Reproduce

```
Method: POST
URL: https://staging-api-diagnostics.yodaprojects.com/address/checkServingLocation
Headers:
  Authorization: Bearer <valid_member_token>
  Content-Type: application/json

── Scenario 1: Missing latitude ──
Body: { "longitude": 78.4867 }

── Scenario 2: Missing longitude ──
Body: { "latitude": 17.3850 }

── Scenario 3: Empty body ──
Body: {}

── Scenario 4: Null latitude ──
Body: { "latitude": null, "longitude": 78.4867 }

── Scenario 5: Null longitude ──
Body: { "latitude": 17.3850, "longitude": null }
```

### Actual Result

```
Status Code: 500 Internal Server Error
```

### Expected Result

```
Status Code: 400 Bad Request OR 422 Unprocessable Entity

Response Body:
{
  "success": false,
  "message": "latitude and longitude are required fields"
}
```

### Additional Evidence

Verified for Member user flow — all 5 scenarios return 500:
- CSL_04 [MEMBER]: Missing latitude → 500 (should be 400/422)
- CSL_05 [MEMBER]: Missing longitude → 500 (should be 400/422)
- CSL_06 [MEMBER]: Empty body → 500 (should be 400/422)
- CSL_11 [MEMBER]: Null latitude → 500 (should be 400/422)
- CSL_12 [MEMBER]: Null longitude → 500 (should be 400/422)

Note: Valid coordinates (`{"latitude": 17.3850, "longitude": 78.4867}`) return 200 correctly (CSL_01, CSL_02 pass).

### Possible Root Causes

1. **No input validation** — Controller/service directly accesses `latitude`/`longitude` without null-checking
2. **NullPointerException** — Server tries to compute geolocation with null coordinates
3. **Missing request schema validation** — No middleware validates required fields before processing

### Impact

- **CRITICAL** — Any client sending malformed requests crashes the server
- 500 errors trigger unnecessary alerting and log noise
- Potential stability issue if many clients send invalid requests simultaneously
- Same pattern as BUG-001 (missing field validation) — systemic issue across endpoints

### Affected Tests (5 failures)

- CSL_04: Missing latitude → 500 (should be 4xx)
- CSL_05: Missing longitude → 500 (should be 4xx)
- CSL_06: Empty body → 500 (should be 4xx)
- CSL_11: Null latitude → 500 (should be 4xx)
- CSL_12: Null longitude → 500 (should be 4xx)

---

## BUG-018: POST /address/checkServingLocation — response does not expose 'serving_location' field in accessible path

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Priority** | P1 |
| **Component** | Address API |
| **Type** | API Contract / Response Structure |
| **Affected Endpoint** | POST /address/checkServingLocation |
| **Environment** | Staging (staging-api-diagnostics.yodaprojects.com) |

### Description

When calling `POST /address/checkServingLocation` with valid coordinates, the API returns HTTP 200 but the `serving_location` field is **not accessible** at either `response.serving_location` or `response.data.serving_location`. The response structure does not contain `serving_location` in any discoverable path. This makes it impossible for clients to determine whether a location is served.

Additionally, the response does not follow the standard envelope format (`{status, success, msg, data: {...}}`) used by all other Address API endpoints.

### Steps to Reproduce

```
Method: POST
URL: https://staging-api-diagnostics.yodaprojects.com/address/checkServingLocation
Headers:
  Authorization: Bearer <valid_token>
  Content-Type: application/json

Body: { "latitude": 17.3850, "longitude": 78.4867 }
```

### Actual Result

```
Status Code: 200 OK

Response Body: 'serving_location' field not found at:
  - response.serving_location → null
  - response.data.serving_location → null
  - response.data → null
```

### Expected Result

```
Status Code: 200 OK

Response Body:
{
  "status": 200,
  "success": true,
  "msg": "Serving location checked successfully",
  "data": {
    "serving_location": true
  }
}
```

### Additional Evidence

Tested across multiple locations — all return 200 but `serving_location` is null:
- CSL_22: Hyderabad (17.3850, 78.4867) → 200, `serving_location` = null
- CSL_18: New York (40.7128, -74.0060) → 200, `serving_location` = null
- CSL_19: London (51.5074, -0.1278) → 200, `serving_location` = null
- CSL_23: Sydney (-33.8688, 151.2093) → 200, `serving_location` = null
- CSL_24: South Pole (-90.0, -180.0) → 200, `serving_location` = null

Standard envelope used by other Address API endpoints:
- GET /address/getAddressByGuid → `{"status":200,"success":true,"msg":"...","data":{"serving_location":true,...}}`

### Possible Root Causes

1. **Field renamed** — The field may have been renamed (e.g., `is_serving`, `servable`, `in_service_area`)
2. **Nested differently** — Field may be nested under a non-standard key
3. **Response body is empty** — API returns only `{"success": true}` without location info
4. **Content-Type mismatch** — Response may not be valid JSON

### Impact

- **HIGH** — Clients cannot determine if a location is served or not
- The primary purpose of this endpoint is defeated
- Mobile/frontend apps will show incorrect serviceability info
- All location-check UI flows are broken

### Affected Tests (6 failures)

- CSL_03: Verify response fields — no `data` or `serving_location` found
- CSL_18: New York — `serving_location` null
- CSL_19: London — `serving_location` null
- CSL_22: Hyderabad — `serving_location` null
- CSL_23: Sydney — `serving_location` null
- CSL_24: South Pole — `serving_location` null

---

## BUG-019: POST /address/checkServingLocation returns 500 for valid coordinates (0, 0)

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Priority** | P1 |
| **Component** | Address API |
| **Type** | Server Error / Edge Case |
| **Affected Endpoint** | POST /address/checkServingLocation |
| **Environment** | Staging (staging-api-diagnostics.yodaprojects.com) |

### Description

When calling `POST /address/checkServingLocation` with coordinates (0.0, 0.0) — which are valid geographic coordinates (Null Island, Gulf of Guinea) — the API returns HTTP 500 instead of 200 with `serving_location=false`. Unlike BUG-017 (missing/null fields), this request has **valid numeric coordinates** but the server still crashes.

### Steps to Reproduce

```
Method: POST
URL: https://staging-api-diagnostics.yodaprojects.com/address/checkServingLocation
Headers:
  Authorization: Bearer <valid_token>
  Content-Type: application/json

Body: { "latitude": 0.0, "longitude": 0.0 }
```

### Actual Result

```
Status Code: 500 Internal Server Error
```

### Expected Result

```
Status Code: 200 OK

Response Body:
{
  "success": true,
  "data": {
    "serving_location": false
  }
}
```

### Additional Evidence

- Coordinates (0, 0) are valid — they point to the Gulf of Guinea (Null Island)
- Other valid coordinates (17.3850, 78.4867) return 200 correctly
- This appears to be a "falsy value" bug — the server may treat `0.0` as `null`/`false` in a conditional check

### Possible Root Causes

1. **Falsy value check** — Server uses `if (!latitude)` which treats `0` as falsy in JavaScript/Node.js
2. **Division by zero** — Some calculation may divide by latitude/longitude
3. **Edge case in geolocation lookup** — The database/service handling (0,0) crashes

### Impact

- **HIGH** — Demonstrates fragile input handling
- Any location on the equator (lat=0) or prime meridian (lon=0) could trigger this
- Indicates server uses truthy/falsy checks instead of proper null validation

### Affected Tests (1 failure)

- CSL_20: Null Island (0.0, 0.0) → 500 (should be 200 with serving_location=false)

---

## BUG-020: PUT /address/UpdateAddressById returns 400 for New User even with postal_code included

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Priority** | P1 |
| **Component** | Address API |
| **Type** | Functional Defect |
| **Affected Endpoint** | PUT /address/UpdateAddressById |
| **Environment** | Staging (staging-api-diagnostics.yodaprojects.com) |

### Summary

The PUT `/address/UpdateAddressById` endpoint returns 400 for a **newly created user** when sending `{guid, receiver_name, postal_code}`. The same request structure works for an existing **member user** (returns 200), but fails for a new user — indicating the API applies different validation rules per user type or requires additional mandatory fields for new user addresses.

### Steps to Reproduce

```
Method: PUT
URL: https://staging-api-diagnostics.yodaprojects.com/address/UpdateAddressById
Headers:
  Authorization: Bearer <valid_new_user_token>
  Content-Type: application/json

Body:
{
  "guid": "<new_user_address_guid>",
  "receiver_name": "UpdatedNewUser",
  "postal_code": "500012"
}
```

### Actual Result

```
Status Code: 400 Bad Request

(likely: "msg": "<another_required_field> is required.")
```

### Expected Result

```
Status Code: 200 OK

{
  "status": 200,
  "success": true,
  "msg": "Address updated successfully",
  "data": {
    "guid": "<new_user_address_guid>",
    "receiver_name": "UpdatedNewUser",
    ...
  }
}
```

### Additional Evidence

- **UAB_01 (Member user)**: Same payload structure → 200 ✅ (after adding postal_code)
- **UAB_03 (New user)**: Same payload structure → 400 ❌
- Both use the same endpoint with valid tokens
- UAB_04 cascade failure: receiver_name remains "Frank" (original) instead of "UpdatedNewUser"

### Possible Root Causes

1. **Additional mandatory fields** — The API may require more fields beyond `postal_code` (e.g., `city`, `state`, `address_line1`) for addresses created by new users
2. **Address ownership validation** — Token user_id may not match address user_id due to address creation flow
3. **Different validation rules** — New user addresses may have stricter update requirements than member addresses

### Impact

- **HIGH** — Newly registered users cannot update their address details
- Only existing members can modify addresses
- Creates inconsistent user experience between new and returning users

### Affected Tests (2 failures)

- UAB_03: New user valid update → 400 (should be 200)
- UAB_04: Verify update → receiver_name unchanged (cascade from UAB_03)

---

## Recommended Fix (Single Deployment)

Add UUID format validation middleware on all path parameters (user_id, address_guid) and request body fields (guid) BEFORE the request reaches the controller/service layer. Additionally, add request body schema validation for POST/PUT endpoints. For checkServingLocation specifically, add null-checks for latitude/longitude before processing.

This single middleware addition would resolve BUG-001 through BUG-014 in one deployment.

BUG-015 and BUG-016 are functional regressions requiring separate investigation into recent deployment changes.

### Suggested Validation Regex for UUID:
```
^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$
```
