# UpdateAddressById API — Bug Ticket for JIRA

**Endpoint:** PUT /address/UpdateAddressById  
**Base URL:** https://staging-api-diagnostics.yodaprojects.com  
**Total Bugs:** 1  
**Total Test Failures:** 2  

---

## UAB-BUG-001: PUT /address/UpdateAddressById returns 400 for New User despite including all known mandatory fields

| Field | Value |
|-------|-------|
| **Summary** | Update Address API rejects valid payload for newly created users but accepts same structure for existing members |
| **Severity** | High |
| **Priority** | P1 |
| **Component** | Address API — UpdateAddressById |
| **Type** | Functional Defect |
| **Affected Endpoint** | PUT /address/UpdateAddressById |
| **Environment** | Staging (staging-api-diagnostics.yodaprojects.com) |
| **Reporter** | QA Automation |
| **Assignee** | Backend Team |
| **Labels** | api-defect, address-service, regression |

---

### Description

The `PUT /address/UpdateAddressById` endpoint returns **400 Bad Request** when a newly created user attempts to update their address, even though the payload includes all known mandatory fields (`guid`, `receiver_name`, `postal_code`). 

The **same request structure works for an existing member user** (returns 200), but fails for a new user — indicating the API either:
- Requires additional mandatory fields not documented/returned in the error message
- Applies different validation rules based on user type
- Has an address ownership/token mismatch issue for new users

---

### Steps to Reproduce

**Pre-condition:**
1. Create a new user via `POST /users/addUser`
2. Generate token for new user via OTP flow
3. Add an address for new user via `POST /address/addAddress` → capture `address_guid`

**Test Step:**
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

---

### Actual Result

```
HTTP 400 Bad Request

{
  "status": 400,
  "success": false,
  "msg": "<validation_error_message>"
}
```

---

### Expected Result

```
HTTP 200 OK

{
  "status": 200,
  "success": true,
  "msg": "Address updated successfully",
  "data": {
    "guid": "<new_user_address_guid>",
    "receiver_name": "UpdatedNewUser",
    "postal_code": "500012",
    ...
  }
}
```

---

### Comparison: Member vs New User (Same Payload Structure)

| User Type | Payload | Result |
|-----------|---------|--------|
| **Member** (existing) | `{"guid": "<member_addr_guid>", "receiver_name": "UpdatedMember", "postal_code": "500012"}` | ✅ **200 OK** |
| **New User** (freshly created) | `{"guid": "<new_user_addr_guid>", "receiver_name": "UpdatedNewUser", "postal_code": "500012"}` | ❌ **400 Bad Request** |

Both requests:
- Use valid Bearer tokens for their respective users
- Target addresses owned by the authenticated user
- Include `guid`, `receiver_name`, and `postal_code`

---

### Cascade Impact

Because the update fails (UAB_03), the subsequent verification test (UAB_04) also fails:

```
GET /address/getAddressByGuid/<new_user_address_guid>

Expected: receiver_name = "UpdatedNewUser"
Actual:   receiver_name = "Frank" (unchanged — original value from creation)
```

---

### Affected Tests

| Test ID | Description | Expected | Actual | Type |
|---------|-------------|----------|--------|------|
| UAB_03 | New user: update receiver_name | 200 | **400** | Direct failure |
| UAB_04 | New user: verify receiver_name updated via GET | "UpdatedNewUser" | "Frank" | Cascade failure |

---

### Possible Root Causes

1. **Additional mandatory fields** — The API may require more fields for new user addresses (e.g., `city`, `state`, `address_line1`, `country`) that are not required for member addresses
2. **Address ownership mismatch** — The new user's token `user_id` may not match the address's `user_id` due to the creation flow
3. **Different validation schema** — New users may have stricter update requirements than existing members
4. **Address state issue** — New user's address may be in "pending" state that doesn't allow updates
5. **Missing `user_id` in payload** — API may require explicit `user_id` field for non-member users

---

### Suggested Investigation

1. **Check the full 400 response body** — What specific field is the API complaining about?
2. **Try with full payload** — Send all address fields (guid, receiver_name, postal_code, city, state, country, address_line1, type, latitude, longitude) for new user
3. **Compare DB records** — Check if member address and new user address have different schemas/states
4. **Check API logs** — Look for the validation rule that triggers the 400
5. **Test with member token on new user's address** — Rule out token-based validation

### Workaround (for testing)

Send complete address payload with all fields populated when updating a new user's address.

---

### Acceptance Criteria

- [ ] PUT /address/UpdateAddressById returns 200 for new user with `{guid, receiver_name, postal_code}`
- [ ] Same validation rules apply regardless of user type (member vs new user)
- [ ] Error response clearly states which field is missing/invalid
- [ ] API documentation updated with complete list of mandatory fields
