---
name: Validate Global Search and Cart Consistency
description: Implement end-to-end validation to ensure data consistency between Global Search results and Cart contents.
---

# Validate Global Search and Cart Consistency

## Description
Implement a robust validation mechanism to ensure that the data returned by the Global Search API matches exactly with the data present in the Cart API response. This ensures data integrity throughout the user flow.

## Acceptance Criteria
- [ ] **Global Search Validation**:
    - [ ] Added validation to check if a test is available in the searched location (`x.data[0].locations`).
    - [ ] Added validation for test status (must be `ACTIVE`).
    - [ ] Added validation for price (must be non-negative).
    - [ ] Extracted and stored additional fields: `rewards_percentage`, `b2b_price`, `diseases`, `organ`.
- [ ] **Cart Validation**:
    - [ ] Validated that items in the Cart response match the tests found in Global Search.
    - [ ] Verified `Test ID` / `Product ID`.
    - [ ] Verified `Rewards Percentage` matches.
    - [ ] Verified `B2B Price` matches.
    - [ ] Verified associated `Disease` matches.
    - [ ] Verified associated `Organ` matches.
- [ ] **Package Validation**:
    - [ ] Included "Full Body Health Checkup" in the search scope to validate package content handling.

## Implementation Details
1.  **Modified `GlobalSearchHelper.java`**:
    - Updated `extractAndStoreTests` to validate location availability against the searched location ID.
    - Added warnings for non-ACTIVE status and invalid prices.
    - Enhanced data storage to include `rewards_percentage`, `b2b_price`, `diseases`, and `organ`.

2.  **Modified `GlobalSearchAPITest.java`**:
    - Added "Full Body Health Checkup" to the `testsToSearch` list.
    - Added logs to display the newly validated fields for verification.

3.  **Modified `COD_02_GetCartTest.java`**:
    - Implemented logic to retrieve `test_details` from the Get Cart response.
    - Added a validation loop to compare each cart item against the stored Global Search data.
    - Added specific assertions/logs for Rewards, B2B Price, Disease, and Organ.

## Technical Notes
-   **Files Affected**:
    -   `src/main/java/com/mryoda/diagnostics/api/utils/GlobalSearchHelper.java`
    -   `src/test/java/com/mryoda/diagnostics/api/tests/tests_packages/GlobalSearchAPITest.java`
    -   `src/test/java/com/mryoda/diagnostics/api/tests/cart/COD_02_GetCartTest.java`

## verification
-   Run `testng_member_cod_modular.xml` and verify the console logs for:
    -   `✅ Location Validation: Test is available...`
    -   `✅ Found Test in Cart...`
    -   `✅ Rewards Percentage Matched...`
    -   `✅ B2B Price Matched...`
