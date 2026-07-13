# Jira Tickets — MrYoda Diagnostics API Automation
**Date:** 2026-06-18  
**Project:** MrYoda Diagnostics Staging API  
**Base URL:** https://staging-api-diagnostics.yodaprojects.com

## Summary — All 5 APIs

| # | Method | Endpoint | Description | Automation Status |
|---|---|---|---|---|
| 1 | POST | /tests/getTestBySlug | Get Test by Slug | Pending |
| 2 | POST | /tests/getAllPackages | Get All Packages | Pending |
| 3 | GET | /tests/getPackageById/{id} | Get Package by ID | Pending |
| 4 | GET | /tests/getPackageBySlug/{slug} | Get Package by Slug | COMPLETED — 60/60 PASS |
| 5 | POST | /tests/getlocations | Get Center Locations | COMPLETED — 30/30 PASS |

---

## TICKET 1

**Summary:** API Automation — `POST /tests/getTestBySlug` Validation Suite

**Issue Type:** Task  
**Priority:** High  
**Labels:** API-Automation, Regression, MrYoda-Diagnostics  
**Status:** To Do

**Description:**

Create an automated test suite for the POST /tests/getTestBySlug endpoint on the MrYoda Diagnostics Staging API.

| Field | Value |
|---|---|
| Base URL | https://staging-api-diagnostics.yodaprojects.com |
| Endpoint | POST /tests/getTestBySlug |
| Method | POST |
| Test Suite File | GetTestBySlugValidationTest.java |
| Suite XML | test-suites/testng_gettestbyslug_validation.xml |

**Planned Test Coverage:**

| Range | Area | Description |
|---|---|---|
| TC01–TC10 | Functional | 200, success=true, data object, msg, Content-Type |
| TC11–TC20 | Field Validation | _id, title, slug, price, description, category fields |
| TC21–TC30 | Negative | Invalid slug, empty slug, missing body, wrong slug format |
| TC31–TC40 | Security | SQL injection, XSS, NoSQL injection, no stack trace leak |
| TC41–TC45 | Performance | Response time < 3s, repeated call stability |
| TC46–TC50 | Chaining | slug and _id consistency between getAllTests and getTestBySlug |

**Run Command:**
```
mvn test "-DsuiteXmlFile=test-suites/testng_gettestbyslug_validation.xml"
```

**Acceptance Criteria:**
- [ ] Test suite file GetTestBySlugValidationTest.java created
- [ ] Suite XML file created
- [ ] All test cases pass on staging
- [ ] Extent HTML report generated under target/ExtentReport/
- [ ] Expected vs Actual status codes shown correctly for negative tests

---

## TICKET 2

**Summary:** API Automation — `POST /tests/getAllPackages` Validation Suite

**Issue Type:** Task  
**Priority:** High  
**Labels:** API-Automation, Regression, MrYoda-Diagnostics  
**Status:** To Do

**Description:**

Create an automated test suite for the POST /tests/getAllPackages endpoint on the MrYoda Diagnostics Staging API.

Note: This endpoint is currently used as a @BeforeClass setup dependency inside GetPackageBySlugValidationTest to extract valid slugs and _ids. A dedicated standalone suite is needed.

| Field | Value |
|---|---|
| Base URL | https://staging-api-diagnostics.yodaprojects.com |
| Endpoint | POST /tests/getAllPackages |
| Method | POST |
| Currently Used In | GetPackageBySlugValidationTest.java → setupValidSlugs() |
| Test Suite File | GetAllPackagesValidationTest.java |
| Suite XML | test-suites/testng_getallpackages_validation.xml |

**Planned Test Coverage:**

| Range | Area | Description |
|---|---|---|
| TC01–TC10 | Functional | 200, success=true, data list non-empty, msg, Content-Type |
| TC11–TC20 | Field Validation | _id, title, slug, price, tests array present in each package |
| TC21–TC30 | Schema | All required fields present across all packages, no null slugs |
| TC31–TC40 | Negative | Invalid body, malformed JSON, extra fields |
| TC41–TC50 | Data Integrity | No duplicate _ids, no duplicate slugs, count consistency |

**Run Command:**
```
mvn test "-DsuiteXmlFile=test-suites/testng_getallpackages_validation.xml"
```

**Acceptance Criteria:**
- [ ] Test suite file GetAllPackagesValidationTest.java created
- [ ] Suite XML file created
- [ ] All test cases pass on staging
- [ ] Extent HTML report generated under target/ExtentReport/

---

## TICKET 3

**Summary:** API Automation — `GET /tests/getPackageById/{id}` Validation Suite

**Issue Type:** Task  
**Priority:** High  
**Labels:** API-Automation, Regression, MrYoda-Diagnostics  
**Status:** To Do

**Description:**

Create an automated test suite for the GET /tests/getPackageById/{id} endpoint on the MrYoda Diagnostics Staging API.

| Field | Value |
|---|---|
| Base URL | https://staging-api-diagnostics.yodaprojects.com |
| Endpoint | GET /tests/getPackageById/{id} |
| Method | GET |
| Setup | @BeforeClass calls POST /tests/getAllPackages to extract valid _ids |
| Test Suite File | GetPackageByIdValidationTest.java |
| Suite XML | test-suites/testng_getpackagebyid_validation.xml |

**Planned Test Coverage:**

| Range | Area | Description |
|---|---|---|
| TC01–TC10 | Functional | 200, success=true, data matches requested _id, msg, Content-Type |
| TC11–TC20 | Field Validation | _id, title, slug, price, tests array, all required fields |
| TC21–TC30 | Negative | Invalid id, wrong ObjectId format, random 24-char hex, empty id |
| TC31–TC40 | Security | SQL injection, XSS payload in id path, no stack trace leak |
| TC41–TC45 | Performance | Response time < 3s, repeated call stability |
| TC46–TC50 | Chaining | _id in getPackageById response matches _id from getAllPackages |

**Run Command:**
```
mvn test "-DsuiteXmlFile=test-suites/testng_getpackagebyid_validation.xml"
```

**Acceptance Criteria:**
- [ ] Test suite file GetPackageByIdValidationTest.java created
- [ ] Suite XML file created
- [ ] All test cases pass on staging
- [ ] Extent HTML report generated under target/ExtentReport/
- [ ] Expected vs Actual status codes shown correctly for negative tests

---

## TICKET 4

**Summary:** API Automation — `GET /tests/getPackageBySlug/{slug}` Validation Suite

**Issue Type:** Task  
**Priority:** High  
**Labels:** API-Automation, Regression, MrYoda-Diagnostics  
**Status:** DONE

**Description:**

Comprehensive automated test suite implemented and passing for the GET /tests/getPackageBySlug/{slug} endpoint on the MrYoda Diagnostics Staging API.

| Field | Value |
|---|---|
| Base URL | https://staging-api-diagnostics.yodaprojects.com |
| Endpoint | GET /tests/getPackageBySlug/{slug} |
| Method | GET |
| Test Suite File | GetPackageBySlugValidationTest.java |
| Suite XML | test-suites/testng_getpackagebyslug_validation.xml |
| Total Test Cases | 60 |
| Result | ALL 60 PASS |

**Test Coverage:**

| Range | Area | Description |
|---|---|---|
| TC01–TC10 | Functional | 200 response, success flag, data fields, response shape |
| TC11–TC20 | Field Validation | _id, title, slug, price, tests array |
| TC21–TC35 | Negative | Invalid slug, empty slug, special chars, numeric slug |
| TC36–TC50 | Security | SQL injection, XSS, NoSQL injection, no stack trace leak |
| TC51–TC55 | Performance | Response time < 3s, repeated call stability |
| TC56–TC60 | Chaining | slug and _id consistency between getAllPackages and getPackageBySlug |

**Setup:** @BeforeClass calls POST /tests/getAllPackages to extract valid slugs.  
**Report:** Extent Report with REQUEST → EXPECTED → ACTUAL → RESULT per test.

**Run Command:**
```
mvn test "-DsuiteXmlFile=test-suites/testng_getpackagebyslug_validation.xml"
```

**Acceptance Criteria:**
- [x] 60/60 test cases pass on staging
- [x] Extent HTML report generated under target/ExtentReport/
- [x] Expected vs Actual status codes shown correctly for negative tests (range-based: 4xx 400-499)
- [x] Suite runs via Maven command above

---

## TICKET 5

**Summary:** API Automation — `POST /tests/getlocations` Validation Suite

**Issue Type:** Task  
**Priority:** High  
**Labels:** API-Automation, Regression, MrYoda-Diagnostics  
**Status:** DONE

**Description:**

Positive + schema automated test suite implemented and passing for the POST /tests/getlocations endpoint on the MrYoda Diagnostics Staging API.

> **Note:** This is a public read-only endpoint — it returns HTTP 200 for all inputs (no auth required, ignores extra body fields). Negative/security tests excluded by design.

| Field | Value |
|---|---|
| Base URL | https://staging-api-diagnostics.yodaprojects.com |
| Endpoint | POST /tests/getlocations |
| Method | POST |
| Request Body | Empty JSON {} |
| Headers | accept: */*, Content-Type: application/json |
| Test Suite File | GetLocationsValidationTest.java |
| Suite XML | test-suites/testng_getlocations_validation.xml |
| Total Test Cases | 30 |
| Result | ALL 30 PASS |
| Staging Data | 7 locations returned |

**Test Coverage:**

| Range | Area | Description |
|---|---|---|
| TC01–TC08 | Functional | 200, success=true, status field, data list, msg, Content-Type, idempotency, response time < 3s |
| TC09–TC20 | Schema Validation | _id, title, slug, city+state, address, status, pincode 6-digit format, latitude/longitude numeric + range, google_map_location_url, mobile, is_serving_radiology boolean |
| TC21–TC30 | Data Integrity | No duplicate _ids, no duplicate slugs, at least 1 active location, ObjectId 24-hex format, URL-safe slugs, min required schema across all 7 locations, city/state non-empty, consistent count + titles across calls |

**Run Command:**
```
mvn test "-DsuiteXmlFile=test-suites/testng_getlocations_validation.xml"
```

**Acceptance Criteria:**
- [x] 30/30 test cases pass on staging
- [x] Extent HTML report generated under target/ExtentReport/
- [x] Suite runs via Maven command above

---

## Framework Notes

| Item | Detail |
|---|---|
| Language | Java |
| Framework | TestNG + RestAssured |
| Reports | Extent Reports (HTML) |
| Report Path | target/ExtentReport/MrYoda_Automation_Report.html |
| Maven Command | mvn test "-DsuiteXmlFile=test-suites/{suite-file}.xml" |
| Negative Status Handling | Range-based: setExpectedStatusRange(400, 499) shows 4xx (400-499) in report |


**Issue Type:** Task  
**Priority:** High  
**Labels:** API-Automation, Regression, MrYoda-Diagnostics

**Description:**

Implemented a comprehensive automated test suite for the GET /tests/getPackageBySlug/{slug} endpoint on the MrYoda Diagnostics Staging API.

| Field | Value |
|---|---|
| Base URL | https://staging-api-diagnostics.yodaprojects.com |
| Endpoint | GET /tests/getPackageBySlug/{slug} |
| Test Suite File | GetPackageBySlugValidationTest.java |
| Suite XML | test-suites/testng_getpackagebyslug_validation.xml |
| Total Test Cases | 60 |
| Status | ALL 60 PASS |

**Test Coverage:**

| Range | Area | Description |
|---|---|---|
| TC01–TC10 | Functional | 200 response, success flag, data fields, response shape |
| TC11–TC20 | Field Validation | _id, title, slug, price, tests array |
| TC21–TC35 | Negative | Invalid slug, empty slug, special chars, numeric slug |
| TC36–TC50 | Security | SQL injection, XSS, NoSQL injection, no stack trace leak |
| TC51–TC55 | Performance | Response time < 3s, repeated call stability |
| TC56–TC60 | Chaining | slug and _id consistency between getAllPackages → getPackageBySlug |

**Setup:** @BeforeClass calls POST /tests/getAllPackages to extract valid slugs.  
**Report:** Extent Report with REQUEST → EXPECTED → ACTUAL → RESULT per test.

**Run Command:**
```
mvn test "-DsuiteXmlFile=test-suites/testng_getpackagebyslug_validation.xml"
```

**Acceptance Criteria:**
- [x] 60/60 test cases pass on staging
- [x] Extent HTML report generated under target/ExtentReport/
- [x] Expected vs Actual status codes shown correctly for negative tests (range-based: 4xx 400-499)
- [x] Suite runs via Maven command above

---

## TICKET 2

**Summary:** API Automation — `POST /tests/getlocations` Validation Suite

**Issue Type:** Task  
**Priority:** High  
**Labels:** API-Automation, Regression, MrYoda-Diagnostics

**Description:**

Implemented a positive + schema automated test suite for the POST /tests/getlocations endpoint on the MrYoda Diagnostics Staging API.

> **Note:** This is a public read-only endpoint — it returns HTTP 200 for all inputs (no auth required, ignores extra body fields). Negative/security tests excluded by design.

| Field | Value |
|---|---|
| Base URL | https://staging-api-diagnostics.yodaprojects.com |
| Endpoint | POST /tests/getlocations |
| Request Body | Empty JSON {} |
| Headers | accept: */*, Content-Type: application/json |
| Test Suite File | GetLocationsValidationTest.java |
| Suite XML | test-suites/testng_getlocations_validation.xml |
| Total Test Cases | 30 |
| Status | ALL 30 PASS |
| Staging Data | 7 locations returned |

**Test Coverage:**

| Range | Area | Description |
|---|---|---|
| TC01–TC08 | Functional | 200, success=true, status field, data list, msg, Content-Type, idempotency, response time < 3s |
| TC09–TC20 | Schema Validation | _id, title, slug, city+state, address, status, pincode 6-digit format, latitude/longitude numeric + range, google_map_location_url, mobile, is_serving_radiology boolean |
| TC21–TC30 | Data Integrity | No duplicate _ids, no duplicate slugs, at least 1 active location, ObjectId 24-hex format, URL-safe slugs, minimum required schema across all 7 locations, city/state non-empty, consistent count + titles across calls, full location data print |

**Run Command:**
```
mvn test "-DsuiteXmlFile=test-suites/testng_getlocations_validation.xml"
```

**Acceptance Criteria:**
- [x] 30/30 test cases pass on staging
- [x] Extent HTML report generated under target/ExtentReport/
- [x] Suite runs via Maven command above

---

## TICKET 3

**Summary:** API Automation — `POST /tests/getAllPackages` used as test setup dependency

**Issue Type:** Sub-task  
**Priority:** Medium  
**Labels:** API-Automation, MrYoda-Diagnostics

**Description:**

POST /tests/getAllPackages is currently used as a @BeforeClass setup call inside GetPackageBySlugValidationTest to extract valid slugs and _ids for chaining tests.

| Field | Value |
|---|---|
| Endpoint | POST /tests/getAllPackages |
| Used In | GetPackageBySlugValidationTest.java → setupValidSlugs() |
| Purpose | Extracts validSlug, secondSlug, randomSlug, validPackageId, validPackageName |

A dedicated standalone test suite for this endpoint is pending.

**Acceptance Criteria:**
- [ ] Create GetAllPackagesValidationTest.java with dedicated positive + schema suite
- [ ] Add test-suites/testng_getallpackages_validation.xml

---

## Pending Test Suites — Next to Implement

| # | Endpoint | Method | Status |
|---|---|---|---|
| 1 | /tests/getAllPackages | POST | Pending |
| 2 | /tests/getAllTests | POST | Pending |
| 3 | /tests/getTestBySlug | POST | Pending |
| 4 | /tests/getPackageById/{id} | GET | Pending |

---

## Framework Notes

| Item | Detail |
|---|---|
| Language | Java |
| Framework | TestNG + RestAssured |
| Reports | Extent Reports (HTML) |
| Report Path | target/ExtentReport/MrYoda_Automation_Report.html |
| Maven Command | mvn test "-DsuiteXmlFile=test-suites/{suite-file}.xml" |
| Negative Status Handling | Range-based: setExpectedStatusRange(400, 499) shows 4xx (400-499) in report |
