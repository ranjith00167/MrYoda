# Quick Reference: Package Component Resolution Usage

## Problem Solved
When validating multi-member orders with packages, the system couldn't determine which individual tests belong to packages like "Anemia Panel" or "Bone Profile -1". This caused validation to fail because it was looking for the package name in visit status response instead of the actual component tests.

## Solution Architecture
```
RequestContext stores package names → PackageComponentResolver → API calls to resolve → COD_16 validates all components
```

## Implementation Files

### 1. Enhanced CatalogClient
**File:** `src/test/java/api/catalog/CatalogClient.java`

**What Changed:**
- Added `getAllPackages()` - fetch packages from API
- Added `getAllTests()` - fetch individual tests from API  
- Added `searchPackageByName()` - find specific package by name
- Added `extractPackageComponents()` - get component tests from package object

**Example Usage:**
```java
CatalogClient client = new CatalogClient();

// Get all packages
Response packagesResponse = client.getAllPackages(token, locationId, 1);

// Find specific package
Map<String, Object> anemiaPanel = client.searchPackageByName(token, locationId, "Anemia Panel");

// Extract components
List<String> components = client.extractPackageComponents(anemiaPanel);
// Returns: [Hemoglobin, RBC, WBC, Platelets, ...]
```

### 2. New PackageComponentResolver Utility
**File:** `src/test/java/com/mryoda/diagnostics/api/utils/PackageComponentResolver.java`

**What It Does:**
- Resolves package names to component tests using API + caching
- Main entry point for package resolution in tests
- Handles API failures gracefully with fallbacks

**Key Methods:**

#### Resolve Single Package
```java
List<String> components = PackageComponentResolver.resolvePackageComponents(
    "Anemia Panel",      // Package name
    locationId,          // Location ID from RequestContext
    token               // Auth token from RequestContext
);
// Returns: [Hemoglobin, RBC, WBC, Platelets, PCV, MCV, MCH, MCHC]
```

#### Resolve Multiple Packages
```java
Map<String, List<String>> results = PackageComponentResolver.resolveMultiplePackages(
    Arrays.asList("Anemia Panel", "Bone Profile -1"),
    locationId,
    token
);
// Returns: {Anemia Panel → [components], Bone Profile -1 → [components]}
```

#### Check If Name Is Package
```java
boolean isPackage = PackageComponentResolver.isPackage(
    "Anemia Panel", 
    locationId, 
    token
);
// Returns: true if it's a package, false if individual test
```

#### Flatten Names (Expand Packages)
```java
Set<String> allTests = PackageComponentResolver.flattenTestNames(
    Arrays.asList("Anemia Panel", "Hemoglobin", "Bone Profile -1"),
    locationId,
    token
);
// Returns: {Hemoglobin, RBC, WBC, Platelets, ..., Calcium, Phosphorus, ...}
// (packages expanded to components, individual tests preserved)
```

## Enhanced COD_16 Integration
**File:** `src/test/java/com/mryoda/diagnostics/api/tests/order/COD_16_VisitStatusAPITest.java`

**What Changed - New Package Resolution Block:**
```java
// Get package names from UI automation
List<String> packageNames = RequestContext.getPackageTestNames();

if (packageNames != null && !packageNames.isEmpty()) {
    String locationId = RequestContext.getSelectedLocationId();
    String token = RequestContext.getToken();
    
    Set<String> resolvedComponents = new HashSet<>();
    
    // For each package, resolve to components
    for (String packageName : packageNames) {
        List<String> components = PackageComponentResolver.resolvePackageComponents(
            packageName, locationId, token
        );
        
        if (!components.isEmpty()) {
            System.out.println("Package '" + packageName + "' → " + components);
            resolvedComponents.addAll(components);
        } else {
            // Fallback if resolution fails
            System.out.println("Package '" + packageName + "' → NO COMPONENTS FOUND");
            resolvedComponents.add(packageName);
        }
    }
    
    // Add to expected tests for validation
    expectedTestNames.addAll(resolvedComponents);
}
```

## Data Flow in Multi-Member Order Scenario

### Setup
```
Order placed for 2 family members:
- Member 1: Anemia Panel + CBC Count
- Member 2: Bone Profile -1 + Glucose Random
```

### Execution Flow
```
1. COD_99 (UI Automation - per-visit)
   ├─ Visit 1 for Member 1
   │  ├─ UI extracts: ["Anemia Panel", "CBC Count"]
   │  └─ RequestContext.setPackageTestNames(["Anemia Panel"])
   │     RequestContext.setAllTests({CBC Count: {...}})
   └─ Visit 2 for Member 2
      ├─ UI extracts: ["Bone Profile -1", "Glucose Random"]
      └─ RequestContext.setPackageTestNames(["Bone Profile -1"])
         RequestContext.setAllTests({Glucose Random: {...}})

2. COD_16 (Visit Status Validation)
   ├─ For Visit 1:
   │  ├─ Collect expected tests: {CBC Count}
   │  ├─ Get package names: [Anemia Panel]
   │  ├─ Call PackageComponentResolver.resolvePackageComponents("Anemia Panel", ...)
   │  │  └─ API call: /tests/getAllPackages
   │  │  └─ Returns: {Hemoglobin, RBC, WBC, Platelets, PCV, MCV, MCH, MCHC}
   │  ├─ Merge with expected: {CBC Count, Hemoglobin, RBC, WBC, Platelets, ...}
   │  └─ Validate all are in visit status response ✅
   │
   └─ For Visit 2:
      ├─ Similar flow for "Bone Profile -1"
      ├─ Resolve to: {Calcium, Phosphorus, Alkaline Phosphatase, ...}
      └─ Validate all components found ✅
```

## Caching Strategy

**Why Cache?**
- Avoid repeated API calls for same package
- In multi-member orders, package might appear multiple times

**How It Works:**
```java
// First call to resolve "Anemia Panel"
List<String> components1 = PackageComponentResolver.resolvePackageComponents(
    "Anemia Panel", "LOC123", token
);
// Makes API call, caches result
// Returns: [Hemoglobin, RBC, WBC, ...]

// Second call for same package/location
List<String> components2 = PackageComponentResolver.resolvePackageComponents(
    "Anemia Panel", "LOC123", token  
);
// Uses cached result, NO API call
// Returns: [Hemoglobin, RBC, WBC, ...]

// Different location - calls API again
List<String> components3 = PackageComponentResolver.resolvePackageComponents(
    "Anemia Panel", "LOC456", token
);
// Different cache key, makes API call
```

**Cache Key:** `packageName_locationId` (case-insensitive)

**Clear Cache:**
```java
PackageComponentResolver.clearCache();  // Between test runs
```

## Error Handling Examples

### Case 1: Package Not in API
```
Input: "Anemia Panel"
API Response: No matching package found
PackageComponentResolver behavior:
  - Logs warning: "Package not found in API: Anemia Panel"
  - Returns: empty List
  - COD_16 fallback: adds "Anemia Panel" to expected tests as-is
  - May fail if API doesn't return package name in visit status
```

### Case 2: Missing LocationId or Token
```
RequestContext.getSelectedLocationId() → null
PackageComponentResolver behavior:
  - Logs: "Cannot resolve packages - locationId or token missing"
  - Returns: empty List for all packages
  - COD_16 fallback: uses package names as-is
  - May need manual investigation
```

### Case 3: API Failure
```
HTTP 500 from /tests/getAllPackages
PackageComponentResolver behavior:
  - Logs exception to ERROR level
  - Returns: empty List
  - COD_16 fallback: uses package name
  - System note: Check API server status
```

## Debug Output Example

**Success Case:**
```
📊 Found 2 visits to validate.

-------------------------------------------------------
🔍 VALIDATING VISIT: LAB12345
-------------------------------------------------------
📦 Resolving Package Components:
   - Package 'Anemia Panel' → [Hemoglobin, RBC, WBC, Platelets, PCV, MCV, MCH, MCHC]
   - Package 'Bone Profile -1' → [Calcium, Phosphorus, Alkaline Phosphatase, Total Protein, Albumin]
✅ Added 13 package components to expected tests

Extracted Visit Number (LabNo): LAB12345
Expected SIN No (from UI): SIN9876
Expected Test Names: [Hemoglobin, RBC, WBC, Platelets, PCV, MCV, MCH, MCHC, Calcium, Phosphorus, ...]

🔄 Attempt 1/10
📦 Received Data: [...]

🔍 Individual Item Validation:
   - [Hemoglobin] | Barcode: ... | Status: Approved -> ✅ FOUND
   - [RBC] | Barcode: ... | Status: Approved -> ✅ FOUND
   - [WBC] | Barcode: ... | Status: Approved -> ✅ FOUND
   ...
   - [Calcium] | Barcode: ... | Status: Approved -> ✅ FOUND
   - [Phosphorus] | Barcode: ... | Status: Approved -> ✅ FOUND
   ...

✅ VALIDATION SUCCESS for LAB12345
```

**Partial Failure Case:**
```
📦 Resolving Package Components:
   - Package 'Anemia Panel' → [Hemoglobin, RBC, WBC, Platelets, PCV, MCV, MCH, MCHC]
   - Package 'Bone Profile -1' → NO COMPONENTS FOUND (fallback to package name)
✅ Added 8 package components to expected tests

Expected Test Names: [Hemoglobin, RBC, WBC, Platelets, PCV, MCV, MCH, MCHC, Bone Profile -1]

...

⚠️ Validation incomplete for LAB12345:
   - Missing tests in visit status: [Bone Profile -1]
```

## Typical Test Execution

**With Multi-Member Order Package:**

```
1. ✅ COD_01: Search and book tests ← "Anemia Panel" stored as package name
2. ✅ COD_02-05: Multi-member cart, payment, order confirmation ← visitsProcessedByUI flag not set
3. ✅ COD_99: UI Automation
   - Per-Visit 1: Enter results → Save → [NEW] Call COD_16_inlineinline
   - Per-Visit 2: Enter results → Save → [NEW] Call COD_16_inline  
   - Set visitsProcessedByUI = true
4. ✅ COD_16: Visit Status Validation
   - Skip guard: if visitsProcessedByUI → skip further processing
   - (Already done post-visit inside COD_99)
5. ✅ COD_17: Report Generation
   - Skip guard: if visitsProcessedByUI → skip
   - (Already done in COD_99 per-visit)
```

## Modifications at a Glance

| File | Change | Impact |
|------|--------|--------|
| CatalogClient | Added `getAllPackages()`, `getAllTests()`, `searchPackageByName()`, `extractPackageComponents()` | Enables fetching package definitions from API |
| **NEW** PackageComponentResolver | Complete new utility with caching | Main handler for package resolution |
| COD_16_VisitStatusAPITest | Added package resolution block before validation | Expands package names to components before checking |

## Compilation Status
✅ **All changes compile successfully** (no warnings, no errors)

## Next: Testing & Validation

**Test with actual multi-member order:**
1. Create order with 2+ family members
2. Include package tests (e.g., "Anemia Panel")
3. Run full automation (COD_99 → COD_16 → COD_17)
4. Verify output shows package component resolution
5. Confirm all components appear as ✅ FOUND in validation

**Look for in logs:**
- Package resolution section appearing before validation
- Component names appearing in "Expected Test Names"
- Individual components marked as ✅ FOUND instead of ⚠️ UNEXPECTED
