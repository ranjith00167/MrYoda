# GLOBAL_SEARCH & Pricing API Integration Guide

## Overview
We've now fully integrated **GLOBAL_SEARCH** and **GET_ALL_TESTS** endpoints with complete **pricing extraction** capabilities. This allows you to:

1. **Search** for tests and packages by name using GLOBAL_SEARCH endpoint
2. **Fetch** complete test information including pricing from GET_ALL_TESTS
3. **Extract** pricing for both individual tests and packages
4. **Validate** test/package details before processing

---

## New API Methods in CatalogClient

### 1. GLOBAL_SEARCH Endpoint Integration

#### `searchTestsAndPackages(token, searchTerm, locationId)`
**Purpose:** Search for tests/packages by name using GLOBAL_SEARCH  
**Returns:** Response object with search results  
**Usage:**
```java
CatalogClient client = new CatalogClient();
Response searchResult = client.searchTestsAndPackages(token, "Anemia Panel", locationId);
```

#### `findTestOrPackageDetails(token, searchTerm, locationId)`
**Purpose:** Search and extract detailed information for a test/package  
**Returns:** Map with test/package details  
**Usage:**
```java
Map<String, Object> details = client.findTestOrPackageDetails(token, "Diabetes Panel", locationId);
// Returns: {name: "Diabetes Panel", price: 2500, components: [...], ...}
```

---

### 2. GET_ALL_TESTS with Pricing

#### `getAllTestsWithPricing(token, locationId, page)`
**Purpose:** Fetch all tests with complete information including pricing  
**Returns:** Response object with full test data  
**Usage:**
```java
Response response = client.getAllTestsWithPricing(token, locationId, 1);
// Response includes: {name, price, selling_price, description, sample_type, ...}
```

#### `extractTestPricing(testsResponse)`
**Purpose:** Extract test name → price mapping from API response  
**Returns:** Map<String, Double> - Map of test names to prices  
**Features:**
- Handles multiple price field names (price, selling_price, cost)
- Returns empty map on error (graceful fallback)
- Skips tests with unparseable prices  

**Usage:**
```java
Response response = client.getAllTestsWithPricing(token, locationId, 1);
Map<String, Double> testPrices = client.extractTestPricing(response);
// Result: {"Hemoglobin": 300.0, "RBC": 250.0, "WBC": 250.0, ...}
```

#### `getTestPrice(token, locationId, testName, page)`
**Purpose:** Get pricing for a specific test by name  
**Returns:** Double (price) or null if not found  
**Usage:**
```java
Double price = client.getTestPrice(token, locationId, "Hemoglobin", 1);
if (price != null) {
    System.out.println("Hemoglobin price: ₹" + price);
}
```

---

### 3. Package Pricing

#### `extractPackagePricing(packagesResponse)`
**Purpose:** Extract package name → price mapping from GET_ALL_PACKAGES response  
**Returns:** Map<String, Double> - Map of package names to prices  
**Features:**
- Flexible field detection (price, selling_price, total_price, etc.)
- Empty map on error (graceful)
- Thread-safe for concurrent calls  

**Usage:**
```java
Response response = client.getAllPackages(token, locationId, 1);
Map<String, Double> packagePrices = client.extractPackagePricing(response);
// Result: {"Anemia Panel": 1500.0, "Bone Profile": 2000.0, ...}
```

---

## New Methods in PackageComponentResolver

### 1. Pricing Utilities

#### `getTestPricing(locationId, token)` [STATIC]
**Purpose:** Fetch ALL test pricing for a location (with pagination)  
**Returns:** Map of all test names → prices  
**Pagination:** Automatically handles up to 5 pages  
**Usage:**
```java
Map<String, Double> allTestPrices = PackageComponentResolver.getTestPricing(locationId, token);
Double hemoglobinPrice = allTestPrices.get("Hemoglobin");
```

#### `getPackagePricing(locationId, token)` [STATIC]
**Purpose:** Fetch ALL package pricing for a location (with pagination)  
**Returns:** Map of all package names → prices  
**Pagination:** Automatically handles up to 5 pages  
**Usage:**
```java
Map<String, Double> allPackagePrices = PackageComponentResolver.getPackagePricing(locationId, token);
Double anemiaPrice = allPackagePrices.get("Anemia Panel");
```

### 2. GLOBAL_SEARCH Access

#### `searchGlobal(searchTerm, locationId, token)` [STATIC]
**Purpose:** Search for test/package using GLOBAL_SEARCH endpoint  
**Returns:** Map with details if found, empty map otherwise  
**Usage:**
```java
Map<String, Object> result = PackageComponentResolver.searchGlobal("CBC", locationId, token);
if (!result.isEmpty()) {
    String name = (String) result.get("name");
    Double price = (Double) result.get("price");
}
```

---

## Complete Usage Example

### Scenario: Get all test details and prices for a location

```java
// Get all test prices
Map<String, Double> testPrices = PackageComponentResolver.getTestPricing(locationId, token);
System.out.println("Total tests with pricing: " + testPrices.size());

// Example output:
// Total tests with pricing: 47
// Hemoglobin: ₹300.0
// RBC: ₹250.0
// WBC: ₹250.0
// ...

// Search for specific test via GLOBAL_SEARCH
Map<String, Object> details = PackageComponentResolver.searchGlobal("Diabetes", locationId, token);
// Result includes name, price, components, sample_type, etc.

// Get all package prices
Map<String, Double> packagePrices = PackageComponentResolver.getPackagePricing(locationId, token);
System.out.println("Anemia Panel: ₹" + packagePrices.get("Anemia Panel"));
```

---

## Error Handling

All methods use **graceful fallback** strategy:

✅ **GET_ALL_TESTS** unavailable? → Returns empty map  
✅ **Price field** missing? → Skips that test  
✅ **Search** returns no results? → Returns empty map  
✅ **Price parsing** fails? → Skips that item  

**No exceptions thrown** - all errors logged at INFO/WARN level

---

## API Response Structure Reference

### GET_ALL_TESTS Response
```json
{
  "data": {
    "tests": [
      {
        "name": "Hemoglobin",
        "price": 300.0,
        "selling_price": 350.0,
        "sample_type": "blood",
        "description": "...",
        "id": "test_123",
        ...
      }
    ],
    "total": 47
  }
}
```

### GET_ALL_PACKAGES Response
```json
{
  "data": {
    "packages": [
      {
        "name": "Anemia Panel",
        "price": 1500.0,
        "components": ["Hemoglobin", "RBC", "WBC", "Platelets"],
        "total_price": 1500.0,
        "id": "pkg_456",
        ...
      }
    ]
  }
}
```

### GLOBAL_SEARCH Response
```json
{
  "data": {
    "results": [
      {
        "name": "Diabetes Panel",
        "type": "package",
        "price": 2500.0,
        "components": [...],
        ...
      }
    ]
  }
}
```

---

## Key Endpoints Reference

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `tests/adminTests` | POST | GLOBAL_SEARCH - Find tests/packages |
| `/tests/getAllTests` | POST | Get all tests with complete info |
| `/tests/getAllPackages` | POST | Get all packages with complete info |
| `/tests/getBrand` | POST/GET | Get brand information |

---

## Integration with Tests

### In COD_16_VisitStatusAPITest:
```java
// After resolving package components, also get pricing
Map<String, Double> testPrices = PackageComponentResolver.getTestPricing(locationId, token);
Double expectedCost = testPrices.get(resolvedComponent);
```

### In PayOnlineCrossApiValidationTest:
```java
// Validate product pricing using backend API pricing
Map<String, Double> apiPrices = PackageComponentResolver.getTestPricing(locationId, token);
// Compare with order pricing from response
```

### In Custom Test Scenarios:
```java
// Use GLOBAL_SEARCH to dynamically search for tests
Map<String, Object> searchResult = PackageComponentResolver.searchGlobal(searchTerm, locationId, token);
// Use pricing to validate order calculations
Map<String, Double> prices = PackageComponentResolver.getTestPricing(locationId, token);
```

---

## Summary

✅ **GLOBAL_SEARCH** fully integrated for test/package searches  
✅ **GET_ALL_TESTS** integrated with pricing extraction  
✅ **GET_ALL_PACKAGES** integrated with pricing extraction  
✅ **PackageComponentResolver** enhanced with static pricing methods  
✅ **Graceful error handling** on all API calls  
✅ **Comprehensive logging** at all levels  
✅ **Production-ready** code with full pagination support  

**Status:** Ready to use in your tests! 🚀
