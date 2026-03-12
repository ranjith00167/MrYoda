# Package Component Resolution - Developer Guide

## For Test Writers & Framework Developers

This guide explains how to use the package resolution APIs in your own tests or step definitions.

## Quick Start: Resolve a Package

```java
import com.mryoda.diagnostics.api.utils.PackageComponentResolver;
import com.mryoda.diagnostics.api.utils.RequestContext;

// Get location and token from RequestContext
String locationId = RequestContext.getSelectedLocationId();
String token = RequestContext.getToken();

// Resolve package to components
List<String> components = PackageComponentResolver.resolvePackageComponents(
    "Anemia Panel",  // package name you want to resolve
    locationId,      // where package is available
    token           // authorization
);

System.out.println("Package components: " + components);
// Output: [Hemoglobin, RBC, WBC, Platelets, PCV, MCV, MCH, MCHC]
```

## Common Patterns

### Pattern 1: Validate Expected Tests Include Package Components
```java
// Collect expected tests
Set<String> expectedTests = new HashSet<>(RequestContext.getAllTests().keySet());

// Add package components
String packageName = "Anemia Panel";
List<String> components = PackageComponentResolver.resolvePackageComponents(
    packageName, 
    RequestContext.getSelectedLocationId(),
    RequestContext.getToken()
);
expectedTests.addAll(components);

// Now validate response contains all expected tests
verifyAllTestsPresent(expectedTests, responseItems);
```

### Pattern 2: Detect and Handle Packages
```java
List<String> testNames = RequestContext.getPackageTestNames();

for (String testName : testNames) {
    // Check if it's a package or individual test
    if (PackageComponentResolver.isPackage(testName, locationId, token)) {
        System.out.println(testName + " is a PACKAGE");
        
        // Resolve to components
        List<String> components = PackageComponentResolver.resolvePackageComponents(
            testName, locationId, token
        );
        // Process components...
    } else {
        System.out.println(testName + " is an INDIVIDUAL TEST");
        // Process individual test...
    }
}
```

### Pattern 3: Flatten All Test Names (Expand Packages)
```java
List<String> bookingList = RequestContext.getBookedTestNames();
String locationId = RequestContext.getSelectedLocationId();
String token = RequestContext.getToken();

// Flatten: packages → components, keep individual tests
Set<String> allIndividualTests = PackageComponentResolver.flattenTestNames(
    bookingList, 
    locationId, 
    token
);

// Now allIndividualTests contains only individual test names
for (String test : allIndividualTests) {
    // Each one is a leaf test, not a package
    validateTest(test);
}
```

### Pattern 4: Get Package Price
```java
String packageName = "Anemia Panel";
double price = PackageComponentResolver.getPackagePrice(
    packageName,
    RequestContext.getSelectedLocationId(),
    RequestContext.getToken()
);

Assert.assertEquals(price, expectedPrice, "Package price mismatch");
```

### Pattern 5: Resolve Multiple Packages at Once
```java
List<String> packageNames = RequestContext.getPackageTestNames();
Map<String, List<String>> packageMap = PackageComponentResolver.resolveMultiplePackages(
    packageNames,
    RequestContext.getSelectedLocationId(),
    RequestContext.getToken()
);

// Now packageMap contains: {package1 → [components], package2 → [components], ...}
for (Map.Entry<String, List<String>> entry : packageMap.entrySet()) {
    String package = entry.getKey();
    List<String> components = entry.getValue();
    System.out.println(package + " has " + components.size() + " components");
}
```

## In Step Definitions (Cucumber)

### Step Definition Example
```java
@Given("I resolve package {string} to its components")
public void resolvePackage(String packageName) {
    List<String> components = PackageComponentResolver.resolvePackageComponents(
        packageName,
        RequestContext.getSelectedLocationId(),
        RequestContext.getToken()
    );
    
    if (components.isEmpty()) {
        System.out.println("⚠️ Package not resolved: " + packageName);
    } else {
        System.out.println("✅ Package '" + packageName + "' resolved to: " + components);
        RequestContext.setAllTests(components); // Store for validation
    }
}

@Then("all package components should be in visit status")
public void validatePackageComponents() {
    List<String> packageNames = RequestContext.getPackageTestNames();
    String locationId = RequestContext.getSelectedLocationId();
    String token = RequestContext.getToken();
    
    Set<String> expectedComponents = new HashSet<>();
    for (String packageName : packageNames) {
        List<String> components = PackageComponentResolver.resolvePackageComponents(
            packageName, locationId, token
        );
        expectedComponents.addAll(components);
    }
    
    // Get actual components from visit status response
    List<String> actualComponents = getComponentsFromVisitStatus();
    
    // Verify
    for (String component : expectedComponents) {
        Assert.assertTrue(
            actualComponents.contains(component),
            "Component '" + component + "' not found in visit status"
        );
    }
}
```

## In API Validation Tests

### Example: Payment Validation with Packages
```java
@Test
public void validatePaymentWithPackages() {
    // Get all packages
    Set<String> expandedTests = PackageComponentResolver.flattenTestNames(
        RequestContext.getPackageTestNames(),
        RequestContext.getSelectedLocationId(),
        RequestContext.getToken()
    );
    
    // Count components
    int componentCount = expandedTests.size();
    
    // Add individual tests
    componentCount += RequestContext.getAllTests().size();
    
    // Verify payment was calculated for all components (not just packages)
    Response paymentResponse = getPaymentData();
    List<Map> lineItems = paymentResponse.jsonPath().getList("lineitems");
    
    Assert.assertEquals(
        lineItems.size(), 
        componentCount,
        "Payment line items should include all package components"
    );
}
```

## Error Handling

### Handling Resolution Failures
```java
List<String> components = PackageComponentResolver.resolvePackageComponents(
    packageName, locationId, token
);

if (components.isEmpty()) {
    // Option 1: Use package name as fallback
    System.out.println("⚠️ Could not resolve package, using name: " + packageName);
    components = Arrays.asList(packageName);
    
} else {
    // Option 2: Log success
    System.out.println("✅ Resolved " + packageName + " to " + components.size() + " components");
}
```

### Handling Missing Context
```java
String locationId = RequestContext.getSelectedLocationId();
String token = RequestContext.getToken();

if (locationId == null || token == null) {
    System.out.println("⚠️ Cannot resolve packages: missing locationId or token");
    System.out.println("   locationId: " + locationId);
    System.out.println("   token: " + (token != null ? "present" : "null"));
    // Fall back to package names as-is
} else {
    // Proceed with package resolution
    List<String> components = PackageComponentResolver.resolvePackageComponents(
        packageName, locationId, token
    );
}
```

## Testing the Resolver

```java
@Test
public void testPackageResolution() {
    String locationId = "LOC123";
    String token = "auth_token_here";
    
    // Resolve known package
    List<String> components = PackageComponentResolver.resolvePackageComponents(
        "Anemia Panel",
        locationId,
        token
    );
    
    // Verify
    Assert.assertFalse(components.isEmpty(), "Should resolve Anemia Panel");
    Assert.assertTrue(
        components.contains("Hemoglobin"),
        "Anemia Panel should contain Hemoglobin"
    );
}

@Test
public void testPackageNotFound() {
    String locationId = "LOC123";
    String token = "auth_token_here";
    
    // Try to resolve non-existent package
    List<String> components = PackageComponentResolver.resolvePackageComponents(
        "NonExistent Package XYZ",
        locationId,
        token
    );
    
    // Should return empty
    Assert.assertTrue(components.isEmpty(), "Unknown package should return empty list");
}

@Test
public void testCaching() {
    String locationId = "LOC123";
    String token = "auth_token_here";
    
    // First call - hits API
    long start1 = System.currentTimeMillis();
    List<String> components1 = PackageComponentResolver.resolvePackageComponents(
        "Anemia Panel", locationId, token
    );
    long duration1 = System.currentTimeMillis() - start1;
    
    // Second call - should be cached
    long start2 = System.currentTimeMillis();
    List<String> components2 = PackageComponentResolver.resolvePackageComponents(
        "Anemia Panel", locationId, token
    );
    long duration2 = System.currentTimeMillis() - start2;
    
    // Second call should be much faster
    Assert.assertEquals(components1, components2, "Components should be identical");
    Assert.assertTrue(duration2 < duration1, "Cached call should be faster");
    System.out.println("API call: " + duration1 + "ms, Cached call: " + duration2 + "ms");
}
```

## Performance Considerations

### Cache Hit Ratio
```java
// High cache hit scenario
for (String packageName : Arrays.asList("Anemia Panel", "Anemia Panel", "Anemia Panel")) {
    // All 3 calls use same cache key
    List<String> components = PackageComponentResolver.resolvePackageComponents(
        packageName,
        locationId,  // Same location
        token
    );
}
// Result: 1 API call, 3 uses

// Low cache hit scenario  
for (String packageName : Arrays.asList("Anemia Panel", "Bone Profile", "Lipid Panel")) {
    // Each has different cache key
    List<String> components = PackageComponentResolver.resolvePackageComponents(
        packageName,
        locationId,  // Different locations for each
        token
    );
}
// Result: 3 API calls
```

### When to Clear Cache
```java
// Between test runs
@BeforeClass
public void setup() {
    PackageComponentResolver.clearCache();  // Start fresh
}

// After catalog updates
@After
public void cleanup() {
    PackageComponentResolver.clearCache();  // Clean up
}

// If API behavior changes mid-test
if (catalogUpdated) {
    PackageComponentResolver.clearCache();
    // Retry resolution
}
```

## Troubleshooting

### Issue: Package Not Found
```
Log: "PackageComponentResolver: Package not found in API: Anemia Panel"
→ Check: Does "Anemia Panel" exist in catalog for location?
→ Check: Is locationId correct?
→ Check: Is API endpoint /tests/getAllPackages responding?
```

### Issue: Empty Components
```
Log: "PackageComponentResolver: Found 0 components for package 'Anemia Panel'"
→ Check: API response contains different field name for components
→ Fix: Update extractPackageComponents() to handle new field name
```

### Issue: Missing Authorization
```
Log: "PackageComponentResolver: Error resolving package: Anemia Panel"
→ Check: Is token valid? (expiration, revocation)
→ Check: Does token have permission to access test catalogs?
```

### Issue: Performance Slow
```
Symptom: Test takes unexpectedly long
→ Check: Are you clearing cache between calls?
→ Check: Are you resolving same package with different locationIds?
→ Optimization: Use resolveMultiplePackages() instead of loop
```

## Best Practices

1. **Resolve Once, Use Many Times**
```java
// ❌ Bad: Resolves "Anemia Panel" multiple times
for (Item item : items) {
    List<String> components = PackageComponentResolver.resolvePackageComponents(...);
}

// ✅ Good: Resolve once, reuse
List<String> anemiaComponents = PackageComponentResolver.resolvePackageComponents(...);
for (Item item : items) {
    if (item.matches(anemiaComponents)) {
        // Process
    }
}
```

2. **Group Resolutions**
```java
// ❌ Inefficient: Multiple individual calls
PackageComponentResolver.resolvePackageComponents("Anemia Panel", ...);
PackageComponentResolver.resolvePackageComponents("Bone Profile", ...);  
PackageComponentResolver.resolvePackageComponents("Lipid Panel", ...);

// ✅ Efficient: Single batch call
Map<String, List<String>> all = PackageComponentResolver.resolveMultiplePackages(
    Arrays.asList("Anemia Panel", "Bone Profile", "Lipid Panel"),
    locationId, token
);
```

3. **Always Check for Empty Results**
```java
List<String> components = PackageComponentResolver.resolvePackageComponents(...);

if (components.isEmpty()) {
    // Handle gracefully - maybe use fallback
    System.out.println("⚠️ Resolution failed, proceeding with package name");
} else {
    // Process components
}
```

4. **Log Resolution Progress**
```java
System.out.println("Resolving packages for location: " + locationId);
for (String packageName : packageNames) {
    List<String> components = PackageComponentResolver.resolvePackageComponents(...);
    System.out.println("  - " + packageName + " → " + components.size() + " components");
}
```

## API Used

All package resolution is built on top of:
- **Endpoint:** `POST /tests/getAllPackages`
- **Authentication:** Bearer token in Authorization header
- **Payload:** `{ limit, page, location, diseases }`
- **Response:** Package objects with `components`/`tests` array

## Related Classes

- `CatalogClient.java` - Low-level API interaction
- `CatalogEndpoints.java` - Endpoint definitions
- `RequestContext.java` - Stores resolved packages and location/token
- `COD_16_VisitStatusAPITest.java` - Primary consumer of package resolver
