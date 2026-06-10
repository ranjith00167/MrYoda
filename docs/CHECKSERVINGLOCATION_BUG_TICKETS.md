# checkServingLocation API — Bug Tickets for JIRA

**Endpoint:** POST /address/checkServingLocation  
**Base URL:** https://staging-api-diagnostics.yodaprojects.com  
**Total Bugs:** 3  
**Total Test Failures:** 12  

---

## CSL-BUG-001: Server returns 500 when latitude/longitude is missing or null (no input validation)

| Field | Value |
|-------|-------|
| **Severity** | Critical |
| **Priority** | P0 |
| **Component** | Address API — checkServingLocation |
| **Type** | Server Error / Missing Validation |
| **Affected Endpoint** | POST /address/checkServingLocation |
| **Environment** | Staging |
| **Test Failures** | 5 |

### Summary

The API crashes with HTTP 500 when mandatory fields (`latitude`, `longitude`) are missing, omitted, or explicitly set to null. No input validation exists — the server directly processes the request and throws an unhandled exception.

### Steps to Reproduce

```
Method: POST
URL: https://staging-api-diagnostics.yodaprojects.com/address/checkServingLocation
Headers:
  Authorization: Bearer <valid_member_token>
  Content-Type: application/json
```

**Scenario 1 — Missing latitude:**
```json
{ "longitude": 78.4867 }
```
→ Response: 500

**Scenario 2 — Missing longitude:**
```json
{ "latitude": 17.3850 }
```
→ Response: 500

**Scenario 3 — Empty body:**
```json
{}
```
→ Response: 500

**Scenario 4 — Null latitude:**
```json
{ "latitude": null, "longitude": 78.4867 }
```
→ Response: 500

**Scenario 5 — Null longitude:**
```json
{ "latitude": 17.3850, "longitude": null }
```
→ Response: 500

### Actual Result

```
HTTP 500 Internal Server Error
```

### Expected Result

```
HTTP 400 Bad Request OR 422 Unprocessable Entity

{
  "success": false,
  "message": "latitude and longitude are required fields"
}
```

### Affected Tests

| Test ID | Scenario | Expected | Actual |
|---------|----------|----------|--------|
| CSL_04 | Missing latitude | 4xx | 500 |
| CSL_05 | Missing longitude | 4xx | 500 |
| CSL_06 | Empty body | 4xx | 500 |
| CSL_11 | Null latitude | 4xx | 500 |
| CSL_12 | Null longitude | 4xx | 500 |

### Root Cause (Probable)

Server-side code directly accesses `request.body.latitude` / `request.body.longitude` without checking for null/undefined. This triggers a NullPointerException or TypeError during geolocation computation.

### Recommended Fix

```javascript
// Add validation before processing
if (latitude === null || latitude === undefined || longitude === null || longitude === undefined) {
  return res.status(400).json({
    success: false,
    message: "latitude and longitude are required numeric fields"
  });
}
```

### Impact

- Any malformed client request crashes the server
- 500 errors pollute error monitoring/alerting systems
- Stability risk under load if multiple malformed requests arrive
- Same systemic issue as POST /address/addAddress missing address_line1

---

## CSL-BUG-002: Response does not expose 'serving_location' field — clients cannot determine serviceability

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Priority** | P1 |
| **Component** | Address API — checkServingLocation |
| **Type** | API Contract / Response Structure |
| **Affected Endpoint** | POST /address/checkServingLocation |
| **Environment** | Staging |
| **Test Failures** | 6 |

### Summary

When calling the endpoint with valid coordinates, the API returns HTTP 200 but the `serving_location` boolean field is **not accessible** at any standard JSON path. It is not found at `response.serving_location`, `response.data.serving_location`, or `response.data`. This completely defeats the purpose of the endpoint — clients cannot determine whether a location is served.

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
HTTP 200 OK

Response body does NOT contain 'serving_location' at expected paths:
  - $.serving_location → null
  - $.data.serving_location → null
  - $.data → null
```

### Expected Result

```
HTTP 200 OK

{
  "status": 200,
  "success": true,
  "msg": "Serving location checked successfully",
  "data": {
    "serving_location": true
  }
}
```

### Affected Tests

| Test ID | Location | Coordinates | Expected | Actual |
|---------|----------|-------------|----------|--------|
| CSL_03 | (field verification) | 17.3850, 78.4867 | `data` or `serving_location` present | Neither found |
| CSL_18 | New York | 40.7128, -74.0060 | `serving_location = false` | null |
| CSL_19 | London | 51.5074, -0.1278 | `serving_location = false` | null |
| CSL_22 | Hyderabad | 17.3850, 78.4867 | `serving_location = true` | null |
| CSL_23 | Sydney | -33.8688, 151.2093 | `serving_location = false` | null |
| CSL_24 | South Pole | -90.0, -180.0 | `serving_location = false` | null |

### Comparison with Other Endpoints

| Endpoint | Response Format |
|----------|----------------|
| GET /address/getAddressByGuid | `{"status":200,"success":true,"msg":"...","data":{"serving_location":true,...}}` |
| POST /address/addAddress | `{"status":201,"success":true,"msg":"...","data":{...}}` |
| POST /address/checkServingLocation | ❌ Does NOT return `serving_location` in any path |

### Root Cause (Probable)

1. **Field renamed** — May have been changed to `is_serving`, `serviceable`, or `in_service_area`
2. **Response body different** — May only return `{"success": true}` without the boolean
3. **Nested under unexpected key** — Field may be at a non-standard path
4. **Serialization bug** — Field exists in backend but not serialized to response

### Recommended Fix

1. Include `serving_location` (boolean) in response body under standard `data` wrapper
2. Follow existing envelope: `{status, success, msg, data: {serving_location: <bool>}}`
3. Document the response contract in API docs

### Impact

- **HIGH** — The endpoint's primary function is broken
- Mobile/web apps cannot show "We deliver here" / "We don't serve this area"
- All location-check UI flows return incorrect/missing data
- API is functionally useless despite returning 200

---

## CSL-BUG-003: Server returns 500 for valid coordinates (0.0, 0.0) — falsy value handling bug

| Field | Value |
|-------|-------|
| **Severity** | High |
| **Priority** | P1 |
| **Component** | Address API — checkServingLocation |
| **Type** | Server Error / Edge Case |
| **Affected Endpoint** | POST /address/checkServingLocation |
| **Environment** | Staging |
| **Test Failures** | 1 |

### Summary

When calling the endpoint with coordinates `(0.0, 0.0)` — which are perfectly valid geographic coordinates representing Null Island in the Gulf of Guinea — the API returns HTTP 500. Unlike CSL-BUG-001 (missing/null fields), this request contains **valid numeric values** but the server still crashes.

This strongly suggests the server uses a truthy/falsy check (`if (!latitude)`) which treats `0` as false in JavaScript.

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
HTTP 500 Internal Server Error
```

### Expected Result

```
HTTP 200 OK

{
  "status": 200,
  "success": true,
  "data": {
    "serving_location": false
  }
}
```

### Affected Tests

| Test ID | Location | Coordinates | Expected | Actual |
|---------|----------|-------------|----------|--------|
| CSL_20 | Null Island (Gulf of Guinea) | 0.0, 0.0 | 200 | 500 |

### Root Cause (Probable)

Server-side validation uses falsy check instead of explicit null check:

```javascript
// ❌ BUG: This treats 0.0 as missing
if (!latitude || !longitude) {
  throw new Error("Missing coordinates");
}

// ✅ FIX: Proper null/undefined check
if (latitude === null || latitude === undefined || longitude === null || longitude === undefined) {
  return res.status(400).json({ success: false, message: "coordinates required" });
}
```

### Proof

- `{ "latitude": 17.3850, "longitude": 78.4867 }` → 200 ✅ (truthy values work)
- `{ "latitude": 0.0, "longitude": 0.0 }` → 500 ❌ (0 is falsy in JS)
- `{ "latitude": null, "longitude": 78.4867 }` → 500 ❌ (null is falsy)
- Missing fields → 500 ❌ (undefined is falsy)

All four crash scenarios are explained by a single `if (!latitude || !longitude)` check.

### Recommended Fix

Replace truthy checks with explicit type validation:

```javascript
if (typeof latitude !== 'number' || typeof longitude !== 'number') {
  return res.status(400).json({
    success: false,
    message: "latitude and longitude must be numeric values"
  });
}
```

### Impact

- **HIGH** — Any location on the equator (lat=0) or prime meridian (lon=0) is unreachable
- Affects real cities: São Tomé (0.18°N), Quito (0.18°S), Libreville (0.39°N), Accra (5.5°N, 0.2°W)
- Demonstrates that CSL-BUG-001 and CSL-BUG-003 share the same root cause (falsy check)
- Single fix resolves both bugs

---

## Summary Table

| Bug ID | Severity | Failures | Issue |
|--------|----------|----------|-------|
| CSL-BUG-001 | P0 Critical | 5 | Missing/null fields → 500 (no validation) |
| CSL-BUG-002 | P1 High | 6 | `serving_location` not in response (API contract broken) |
| CSL-BUG-003 | P1 High | 1 | Valid coordinates (0,0) → 500 (falsy value bug) |
| **TOTAL** | | **12** | |

---

## Recommended Fix (All 3 Bugs — Single PR)

```javascript
// 1. Input validation (fixes CSL-BUG-001 + CSL-BUG-003)
const { latitude, longitude } = req.body;
if (typeof latitude !== 'number' || typeof longitude !== 'number') {
  return res.status(400).json({
    success: false,
    message: "latitude and longitude must be numeric values"
  });
}

// 2. Standard response envelope (fixes CSL-BUG-002)
const isServed = await checkIfLocationServed(latitude, longitude);
return res.status(200).json({
  status: 200,
  success: true,
  msg: "Serving location checked successfully",
  data: {
    serving_location: isServed
  }
});
```
