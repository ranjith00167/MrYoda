# Package Component Resolution Implementation

## Overview
This document describes the implementation of package component resolution to fix the issue where package names (like "Anemia Panel") in visit status validation were not being resolved to their individual component tests.

## Problem Statement
**Root Cause:** When a multi-member order includes a package (e.g., "Anemia Panel"), the UI stores the package name. However, during visit status validation (COD_16), the system couldn't verify that all component tests within the package were approved because it didn't know what tests were in "Anemia Panel".

**Impact:** Validation would fail with error like:
```
Validation incomplete for MYD11837: Missing tests in visit status: [Anemia Panel, Bone Profile -1]
```

## Solution Architecture

### 1. Enhanced CatalogClient (api.catalog.CatalogClient)
**Purpose:** Added methods to fetch package catalogs and resolve package definitions

**New Methods:**
- `getAllPackages(String token, String locationId)` - Fetch all packages for a location
- `getAllTests(String token, String locationId)` - Fetch all individual tests  
- `searchPackageByName(String token, String locationId, String packageName)` - Find specific package by name
- `extractPackageComponents(Map<String, Object> packageMap)` - Extract component test names from package object

**Implementation Details:**
- Uses pagination (limit=100 per page) to handle large catalogs
- Searches up to 5 pages by default
- Handles multiple field name variations for components (components, tests, package_tests, items)
- Handles both String and Map objects in component lists

### 2. New PackageComponentResolver Utility (com.mryoda.diagnostics.api.utils.PackageComponentResolver)
**Purpose:** Main utility for resolving package names to component tests with caching

**Key Features:**
- **Caching:** Avoids repeated API calls for same package
- **Multi-Package Support:** Can resolve multiple packages in one call
- **Package Detection:** Can identify if a name is a package vs individual test
- **Test Flattening:** Expands packages while preserving individual tests
- **Price Lookup:** Can retrieve package pricing

**Public Methods:**
```java
// Resolve single package
static List<String> resolvePackageComponents(String packageName, String locationId, String token)

// Resolve multiple packages
static Map<String, List<String>> resolveMultiplePackages(Collection<String> packageNames, 
                                                         String locationId, String token)

// Check if name is a package
static boolean isPackage(String testName, String locationId, String token)

// Get package price
static double getPackagePrice(String packageName, String locationId, String token)

// Flatten test names (expand packages to components)
static Set<String> flattenTestNames(Collection<String> testNames, String locationId, String token)

// Clear cache
static void clearCache()
```

### 3. Enhanced COD_16_VisitStatusAPITest (com.mryoda.diagnostics.api.tests.order.COD_16_VisitStatusAPITest)
**Purpose:** Integrated package resolution into visit status validation

**Changes:**
- Added imports: `PackageComponentResolver`, `ConfigLoader`
- Enhanced test method to resolve package names before validation
- Logs package resolution details for debugging
- Fallback mechanism: if package resolution fails, uses package name as-is

**Integration Flow:**
1. Collect expected test names from RequestContext (individual tests)
2. Get package names from RequestContext
3. For each package name:
   - Call `PackageComponentResolver.resolvePackageComponents()`
   - Get back List<String> of component test names
4. Add all resolved components to expectedTestNames set
5. Validate that all expected tests (individual + package components) are in visit status response

## Data Flow

```
UI Automation (COD_99)
    ↓
UI extracts test/package names via GlobalSearchHelper
    ↓
RequestContext stores:
  - Individual tests in getAllTests()
  - Package names in getPackageTestNames()
    ↓
COD_16 Visit Validation
    ↓
[NEW] PackageComponentResolver.resolvePackageComponents()
    ↓
CatalogClient.getAllPackages()  →  API: /tests/getAllPackages
    ↓
Search for package by name
    ↓
Extract component tests
    ↓
Merge with expected test names
    ↓
Validate all components are in visit status response
    ↓
✅ Validation Success (all Anemia Panel tests approved)
   or
❌ Validation Failure (missing component test)
```

## API Endpoints Used

### Existing Endpoints (Defined in CatalogEndpoints)
- `GET_LOCATION` = "/tests/getlocations" - Get location list
- `GLOBAL_SEARCH` = "tests/adminTests" - Search for tests/packages by name
- `GET_ALL_TESTS` = "/tests/getAllTests" - Get all individual tests for location
- `GET_FETAL_MEDICINE_TESTS` = "/tests/getFetalMedicineTests" - Specialized test category

### New Endpoints (Now Used)
- `GET_ALL_PACKAGES` = "/tests/getAllPackages" - **Get all packages for location (with components)**
- `GET_SAMPLE_TYPE` = "/tests/getSampleType" - Get sample types

## Caching Strategy

**Purpose:** Reduce API calls for frequently resolved packages

**Implementation:**
- Static HashMap: `componentCache<String, List<String>>`
- Cache key: `packageName_locationId`
- Cache cleared on: manual call to `clearCache()` or after test suite completion

**Benefit:** For multi-member orders with same package ordered by multiple members, second member's resolution uses cached result.

## Error Handling

**Scenario 1: Package Not Found**
```
PackageComponentResolver: Package not found in API: Anemia Panel
→ Returns empty List
→ Fallback: Use package name as-is in validation
→ May still fail if API doesn't return package name in visit status
```

**Scenario 2: API Call Fails**
```
PackageComponentResolver: Error resolving package...
→ Logs exception with package name
→ Returns empty List
→ Continues with fallback
```

**Scenario 3: Missing Token/LocationId**
```
Cannot resolve packages - locationId or token missing
→ Message logged to user
→ Uses package names as-is
→ May need manual intervention
```

## Integration Checklist

- [x] CatalogClient enhanced with getAllPackages() method
- [x] CatalogClient enhanced with getAllTests() method  
- [x] CatalogClient has searchPackageByName() for finding packages
- [x] CatalogClient has extractPackageComponents() for component extraction
- [x] PackageComponentResolver utility created with caching
- [x] COD_16 imports updated to include PackageComponentResolver
- [x] COD_16 test method enhanced to call package resolver
- [x] Package resolution happens before validation
- [x] Fallback logic implemented for failed resolutions
- [x] Compiled successfully without errors

## Testing Validation

**Test Case:** Multi-member order with packages

**Setup:**
```
Order for 2 family members
Member 1 books: Anemia Panel + Individual Tests
Member 2 books: Bone Profile -1 + Individual Tests
```

**Expected Flow:**
1. UI extracts package names and stores in RequestContext.setPackageTestNames()
2. COD_16 resolves:
   - "Anemia Panel" → [Hemoglobin, RBC, WBC, Platelets, ...]
   - "Bone Profile -1" → [Calcium, Phosphorus, Alkaline Phosphatase, ...]
3. Validation checks ALL component tests are in visit status response
4. Result: ✅ All tests approved → Test passes

**Debug Output Example:**
```
📦 Resolving Package Components:
  - Package 'Anemia Panel' → [Hemoglobin, RBC, WBC, Platelets, PCV, MCV, MCH, MCHC]
  - Package 'Bone Profile -1' → [Calcium, Phosphorus, Alkaline Phosphatase, Total Protein]
✅ Added 12 package components to expected tests

Individual Item Validation:
  - [Hemoglobin] | Barcode: ... | Status: Approved -> ✅ FOUND
  - [RBC] | Barcode: ... | Status: Approved -> ✅ FOUND
  ...
✅ VALIDATION SUCCESS for VISIT123
```

## Future Enhancements

1. **Brand/Group Filtering:** Use `getBrand()` API to filter packages by brand/group
2. **Dynamic Component Cache:** Invalidate cache based on API response timestamps
3. **Package Pricing Validation:** Add check that package MRP matches component sum
4. **Disease Group Resolution:** Resolve packages by disease category
5. **Multi-Language Support:** Handle package names in different languages

## Files Modified

1. `src/test/java/api/catalog/CatalogClient.java` - Enhanced with package APIs
2. `src/test/java/com/mryoda/diagnostics/api/tests/order/COD_16_VisitStatusAPITest.java` - Integrated package resolution

## Files Created

1. `src/test/java/com/mryoda/diagnostics/api/utils/PackageComponentResolver.java` - New utility for package resolution

## Dependencies

- **CatalogClient:** Requires RequestBuilder, RestAssured for API calls
- **PackageComponentResolver:** Depends on CatalogClient for getAllPackages()
- **COD_16:** Depends on PackageComponentResolver and RequestContext

## Compilation Status

✅ **All changes compiled successfully with -q flag (no warnings or errors)**

## Next Steps

1. Run full test suite with multi-member orders containing packages
2. Verify package component resolution in logs
3. Validate that "Anemia Panel" and "Bone Profile -1" are correctly resolved
4. Monitor for any API response structure variations
5. Fine-tune component field name detection if needed
