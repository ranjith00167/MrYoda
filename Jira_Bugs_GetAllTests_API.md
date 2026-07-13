
# JIRA BUG TICKETS - GetAllTests API (POST /tests/getAllTests)

---

## TICKET 1: Non-existent location ignored

**Summary:** [BUG] Location filter ignored for non-existent ObjectId — returns all results instead of empty
**Priority:** Medium | **TCs:** TC12, TC39, TC59

---

### TC12 — Yodaara OTT with non-existent location

**Request:**
```
POST /tests/getAllTests
```
**Payload:**
```json
{"page":1,"limit":20,"yodaara_ott":true,"location":"000000000000000000000000"}
```
**Expected:** Status 200, total=0, data=[]
**Actual:** Status 200, total=many, data=[...all yodaara tests returned]
**Test Result:** FAIL (Bug confirmed)

---

### TC39 — Pharmacogenomics with non-existent location

**Request:**
```
POST /tests/getAllTests
```
**Payload:**
```json
{"page":1,"limit":20,"pharmacogenomics":true,"location":"000000000000000000000000"}
```
**Expected:** Status 200, total=0, data=[]
**Actual:** Status 200, total=many, data=[...all pharma tests returned]
**Test Result:** FAIL (Bug confirmed)

---

### TC59 — DNADecoder with non-existent location

**Request:**
```
POST /tests/getAllTests
```
**Payload:**
```json
{"page":1,"limit":20,"dnadecoder":true,"location":"000000000000000000000000"}
```
**Expected:** Status 200, total=0, data=[]
**Actual:** Status 200, total=26, data=[...26 dnadecoder tests returned]
**Test Result:** FAIL (Bug confirmed)

---

## TICKET 2: 500 crash on invalid pagination

**Summary:** [BUG] Server crashes with 500 for invalid pagination values — no input validation
**Priority:** High | **TCs:** TC19, TC20, TC40, TC60, TC76

---

### TC19 — Yodaara OTT with page=0

**Request:**
```
POST /tests/getAllTests
```
**Payload:**
```json
{"page":0,"limit":20,"yodaara_ott":true,"location":"64870066842708a0d5ae6c77"}
```
**Expected:** Status 400, message="page must be >= 1"
**Actual:** Status 500 Internal Server Error
**Test Result:** FAIL (Bug confirmed)

---

### TC20 — Yodaara OTT with limit=0

**Request:**
```
POST /tests/getAllTests
```
**Payload:**
```json
{"page":1,"limit":0,"yodaara_ott":true,"location":"64870066842708a0d5ae6c77"}
```
**Expected:** Status 400, message="limit must be >= 1"
**Actual:** Status 500 Internal Server Error
**Test Result:** FAIL (Bug confirmed)

---

### TC40 — Pharmacogenomics with page=0

**Request:**
```
POST /tests/getAllTests
```
**Payload:**
```json
{"page":0,"limit":20,"pharmacogenomics":true,"location":"64870066842708a0d5ae6c77"}
```
**Expected:** Status 400, message="page must be >= 1"
**Actual:** Status 500 Internal Server Error
**Test Result:** FAIL (Bug confirmed)

---

### TC60 — DNADecoder with page=0

**Request:**
```
POST /tests/getAllTests
```
**Payload:**
```json
{"page":0,"limit":20,"dnadecoder":true,"popular":false}
```
**Expected:** Status 400, message="page must be >= 1"
**Actual:** Status 500 Internal Server Error
**Test Result:** FAIL (Bug confirmed)

---

### TC76 — Yodaara OTT with page=-1

**Request:**
```
POST /tests/getAllTests
```
**Payload:**
```json
{"page":-1,"limit":20,"yodaara_ott":true,"location":"64870066842708a0d5ae6c77"}
```
**Expected:** Status 400, message="page must be >= 1"
**Actual:** Status 500 Internal Server Error
**Test Result:** FAIL (Bug confirmed)

---

## TICKET 3: Pagination overlap (duplicates across pages)

**Summary:** [BUG] Page 2 returns 4 duplicate records from Page 1 — broken skip/offset
**Priority:** High | **TCs:** TC27

---

### TC27 — Pharmacogenomics page 1 vs page 2 overlap

**Request 1:**
```
POST /tests/getAllTests
```
**Payload 1:**
```json
{"page":1,"limit":5,"pharmacogenomics":true,"location":"64870066842708a0d5ae6c77"}
```
**Request 2:**
```
POST /tests/getAllTests
```
**Payload 2:**
```json
{"page":2,"limit":5,"pharmacogenomics":true,"location":"64870066842708a0d5ae6c77"}
```
**Expected:** Page 2 has 0 overlapping records with page 1
**Actual:** 4 out of 5 records on page 2 are duplicates from page 1
**Test Result:** FAIL (Bug confirmed)

---

## TICKET 4: SQL Injection & XSS cause 500 + info leak

**Summary:** [BUG][SECURITY] Malicious input in location crashes server with 500 and leaks DB details
**Priority:** Critical | **TCs:** TC72, TC73

---

### TC72 — SQL Injection in location field

**Request:**
```
POST /tests/getAllTests
```
**Payload:**
```json
{"page":1,"limit":20,"yodaara_ott":true,"location":"'; DROP TABLE tests; --"}
```
**Expected:** Status 400, message="Invalid location ID format"
**Actual:** Status 500, response={"success":false,"message":"input must be a 24 character hex string, 12 byte Uint8Array, or an integer"}
**Test Result:** FAIL (Bug confirmed)

---

### TC73 — XSS script tag in location field

**Request:**
```
POST /tests/getAllTests
```
**Payload:**
```json
{"page":1,"limit":20,"yodaara_ott":true,"location":"<script>alert('xss')</script>"}
```
**Expected:** Status 400, message="Invalid location ID format"
**Actual:** Status 500, response={"success":false,"message":"input must be a 24 character hex string, 12 byte Uint8Array, or an integer"}
**Test Result:** FAIL (Bug confirmed)

---

## SUMMARY

| TC | Ticket | Priority | Payload Key | Expected | Actual |
|----|--------|----------|-------------|----------|--------|
| TC12 | 1 | Medium | yodaara_ott + fake location | total=0 | Returns all |
| TC39 | 1 | Medium | pharmacogenomics + fake location | total=0 | Returns all |
| TC59 | 1 | Medium | dnadecoder + fake location | total=0 | Returns 26 |
| TC19 | 2 | High | page=0 (yodaara) | 400 | 500 |
| TC20 | 2 | High | limit=0 (yodaara) | 400 | 500 |
| TC40 | 2 | High | page=0 (pharma) | 400 | 500 |
| TC60 | 2 | High | page=0 (dna) | 400 | 500 |
| TC76 | 2 | High | page=-1 (yodaara) | 400 | 500 |
| TC27 | 3 | High | page1 vs page2 overlap | 0 dups | 4 dups |
| TC72 | 4 | Critical | SQL injection | 400 | 500+leak |
| TC73 | 4 | Critical | XSS payload | 400 | 500+leak |

