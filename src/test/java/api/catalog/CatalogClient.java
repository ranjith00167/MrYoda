package api.catalog;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import utility.APIResponseCapture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CatalogClient {

    public Response globalSearch(String token, Map<String, Object> payload) {
        Response response = new RequestBuilder()
                .setEndpoint(CatalogEndpoints.GLOBAL_SEARCH)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(payload)
                .post();
        
        // Capture response for later validation
        APIResponseCapture.captureResponse("GLOBAL_SEARCH", response);
        return response;
    }
    
    public Response getLocations() {
        return new RequestBuilder()
                .setEndpoint(CatalogEndpoints.GET_LOCATION)
                .get();
    }

    public Response getSampleType(String token) {
        return new RequestBuilder()
                .setEndpoint(CatalogEndpoints.GET_SAMPLE_TYPE)
                .addHeader("Authorization", "Bearer " + token)
                .get();
    }
    
    // ══════════════════════════════════════════════════════════════════════════
    // NEW APIs for Package Resolution
    // ══════════════════════════════════════════════════════════════════════════
    
    /**
     * Get all packages for a given location
     * @param token Authentication token
     * @param locationId Location ID
     * @param page Page number for pagination (default: 1)
     * @return Response with packages list
     */
    public Response getAllPackages(String token, String locationId, int page) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("limit", 100);
        payload.put("page", page);
        payload.put("location", locationId);
        payload.put("diseases", new ArrayList<>());
        
        Response response = new RequestBuilder()
                .setEndpoint(CatalogEndpoints.GET_ALL_PACKAGES)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(payload)
                .post();
        
        // Capture response for later validation
        APIResponseCapture.captureResponse("GET_ALL_PACKAGES_page_" + page, response);
        return response;
    }
    
    /**
     * Get all packages with default pagination
     */
    public Response getAllPackages(String token, String locationId) {
        return getAllPackages(token, locationId, 1);
    }
    
    /**
     * Get all tests for a given location (individual tests, not packages)
     * @param token Authentication token
     * @param locationId Location ID
     * @param page Page number for pagination
     * @return Response with tests list
     */
    public Response getAllTests(String token, String locationId, int page) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("limit", 100);
        payload.put("page", page);
        payload.put("location", locationId);
        payload.put("diseases", new ArrayList<>());
        
        Response response = new RequestBuilder()
                .setEndpoint(CatalogEndpoints.GET_ALL_TESTS)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(payload)
                .post();
        
        // Capture response for later validation
        APIResponseCapture.captureResponse("GET_ALL_TESTS_page_" + page, response);
        return response;
    }
    
    /**
     * Get all tests with default pagination
     */
    public Response getAllTests(String token, String locationId) {
        return getAllTests(token, locationId, 1);
    }
    
    /**
     * Search for a specific package by name (iterates through pages)
     * @param token Authentication token
     * @param locationId Location ID  
     * @param packageName Package name to search for
     * @return Package map if found, null otherwise
     */
    public Map<String, Object> searchPackageByName(String token, String locationId, String packageName) {
        int maxPages = 5;
        for (int page = 1; page <= maxPages; page++) {
            Response res = getAllPackages(token, locationId, page);
            if (res.getStatusCode() != 200) break;
            
            List<Map<String, Object>> packages = res.jsonPath().getList("data.packages");
            if (packages == null || packages.isEmpty()) break;
            
            for (Map<String, Object> pkg : packages) {
                String name = (String) pkg.get("name");
                if (name != null && name.equalsIgnoreCase(packageName)) {
                    return pkg;
                }
            }
        }
        return null;
    }
    
    /**
     * Extract component tests from package details
     * @param packageMap Package map from API response
     * @return List of component test names
     */
    public List<String> extractPackageComponents(Map<String, Object> packageMap) {
        List<String> components = new ArrayList<>();
        if (packageMap == null) return components;
        
        // Try different field names that might contain components
        Object componentsObj = packageMap.get("components");
        if (componentsObj == null) componentsObj = packageMap.get("tests");
        if (componentsObj == null) componentsObj = packageMap.get("package_tests");
        if (componentsObj == null) componentsObj = packageMap.get("items");
        
        if (componentsObj instanceof List) {
            List<?> componentsList = (List<?>) componentsObj;
            for (Object item : componentsList) {
                String testName = null;
                if (item instanceof String) {
                    testName = (String) item;
                } else if (item instanceof Map) {
                    Map<?, ?> itemMap = (Map<?, ?>) item;
                    testName = (String) itemMap.get("name");
                    if (testName == null) testName = (String) itemMap.get("test_name");
                    if (testName == null) testName = (String) itemMap.get("testName");
                }
                if (testName != null && !testName.isEmpty()) {
                    components.add(testName);
                }
            }
        }
        return components;
    }
    
    /**
     * Get brand/organization information for filtering packages
     * @param token Authentication token
     * @param locationId Location ID (optional)
     * @return Response with brand information
     */
    public Response getBrand(String token, String locationId) {
        Map<String, Object> payload = new HashMap<>();
        if (locationId != null) {
            payload.put("location", locationId);
        }
        
        return new RequestBuilder()
                .setEndpoint(CatalogEndpoints.GET_BRAND)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(payload)
                .post();
    }
    
    /**
     * Get all brands available in the system
     * @param token Authentication token
     * @return Response with brands list
     */
    public Response getBrand(String token) {
        return getBrand(token, null);
    }
    
    /**
     * Search for brand by name
     * @param token Authentication token
     * @param brandName Brand name to search for
     * @return Brand map if found, null otherwise
     */
    public Map<String, Object> searchBrandByName(String token, String brandName) {
        try {
            Response res = getBrand(token);
            if (res.getStatusCode() != 200) return null;
            
            List<Map<String, Object>> brands = res.jsonPath().getList("data.brands");
            if (brands == null || brands.isEmpty()) {
                brands = res.jsonPath().getList("data");
            }
            
            if (brands != null) {
                for (Map<String, Object> brand : brands) {
                    String name = (String) brand.get("name");
                    if (name != null && name.equalsIgnoreCase(brandName)) {
                        return brand;
                    }
                }
            }
        } catch (Exception e) {
            // Log but don't fail - brand search is optional
        }
        return null;
    }
    
    /**
     * Get brand ID by brand name
     * @param token Authentication token
     * @param brandName Brand name
     * @return Brand ID if found, null otherwise
     */
    public String getBrandId(String token, String brandName) {
        Map<String, Object> brand = searchBrandByName(token, brandName);
        if (brand != null) {
            Object brandId = brand.get("id");
            if (brandId != null) {
                return brandId.toString();
            }
        }
        return null;
    }
    
    // ══════════════════════════════════════════════════════════════════════════
    // GLOBAL_SEARCH & GET_ALL_TESTS with Pricing Information
    // ══════════════════════════════════════════════════════════════════════════
    
    /**
     * Search for tests and packages using GLOBAL_SEARCH endpoint
     * @param token Authentication token
     * @param searchTerm Search keyword (test/package name)
     * @param locationId Location ID to filter results
     * @return Response with search results
     */
    public Response searchTestsAndPackages(String token, String searchTerm, String locationId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("q", searchTerm);
        payload.put("location", locationId);
        
        return new RequestBuilder()
                .setEndpoint(CatalogEndpoints.GLOBAL_SEARCH)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(payload)
                .post();
    }
    
    /**
     * Get all tests with pricing information for a location
     * @param token Authentication token
     * @param locationId Location ID
     * @param page Page number for pagination
     * @return Response with complete test information including pricing
     */
    public Response getAllTestsWithPricing(String token, String locationId, int page) {
        return getAllTests(token, locationId, page);
    }
    
    /**
     * Extract test pricing from GET_ALL_TESTS response
     * @param testsResponse Response from getAllTests API
     * @return Map of TestName → Price
     */
    public Map<String, Double> extractTestPricing(Response testsResponse) {
        Map<String, Double> testPricing = new HashMap<>();
        
        try {
            if (testsResponse == null || testsResponse.getStatusCode() != 200) {
                return testPricing;
            }
            
            List<Map<String, Object>> tests = testsResponse.jsonPath().getList("data");
            
            if (tests != null) {
                for (Map<String, Object> test : tests) {
                    if (test == null) continue;
                    
                    String testName = (String) test.get("test_name");
                    if (testName == null) testName = (String) test.get("name");
                    if (testName == null) testName = (String) test.get("testName");
                    
                    // Add test if we found a name (price is 0 - API doesn't provide it)
                    if (testName != null) {
                        testPricing.put(testName, 0.0);
                    }
                }
            }
        } catch (Exception e) {
            // Graceful fallback
        }
        
        return testPricing;
    }
    
    /**
     * Get pricing for a specific test by name
     * @param token Authentication token
     * @param locationId Location ID
     * @param testName Test name to get price for
     * @param page Page number to search
     * @return Price as Double, or 0 if not found or no price available
     */
    public Double getTestPrice(String token, String locationId, String testName, int page) {
        Response res = getAllTests(token, locationId, page);
        if (res.getStatusCode() != 200) return 0.0;
        
        List<Map<String, Object>> tests = res.jsonPath().getList("data.tests");
        if (tests == null) tests = res.jsonPath().getList("data");
        
        if (tests != null) {
            for (Map<String, Object> test : tests) {
                String name = (String) test.get("name");
                if (name == null) name = (String) test.get("testName");
                if (name == null) name = (String) test.get("test_name");
                
                if (name != null && name.equalsIgnoreCase(testName)) {
                    Object priceObj = test.get("price");
                    if (priceObj == null) priceObj = test.get("selling_price");
                    if (priceObj == null) priceObj = test.get("sellingPrice");
                    if (priceObj == null) priceObj = test.get("cost");
                    
                    if (priceObj != null) {
                        try {
                            return Double.parseDouble(priceObj.toString());
                        } catch (NumberFormatException e) {
                            return 0.0;
                        }
                    }
                    return 0.0;  // Test found but no price available
                }
            }
        }
        return 0.0;
    }
    
    /**
     * Extract package pricing from GET_ALL_PACKAGES response
     * @param packagesResponse Response from getAllPackages API
     * @return Map of PackageName → Price
     */
    public Map<String, Double> extractPackagePricing(Response packagesResponse) {
        Map<String, Double> packagePricing = new HashMap<>();
        
        try {
            if (packagesResponse == null || packagesResponse.getStatusCode() != 200) {
                return packagePricing;
            }
            
            List<Map<String, Object>> packages = packagesResponse.jsonPath().getList("data");
            if (packages == null) {
                packages = packagesResponse.jsonPath().getList("data.packages");
            }
            
            if (packages != null) {
                for (Map<String, Object> pkg : packages) {
                    if (pkg == null) continue;
                    
                    String packageName = (String) pkg.get("name");
                    if (packageName == null) packageName = (String) pkg.get("packageName");
                    
                    // Add package (price is 0 - API doesn't provide it)
                    if (packageName != null) {
                        packagePricing.put(packageName, 0.0);
                    }
                }
            }
        } catch (Exception e) {
            // Graceful fallback
        }
        
        return packagePricing;
    }
    
    /**
     * Search for test/package using GLOBAL_SEARCH and extract details
     * @param token Authentication token
     * @param searchTerm Search keyword
     * @param locationId Location ID
     * @return Map with test/package details if found, empty map otherwise
     */
    public Map<String, Object> findTestOrPackageDetails(String token, String searchTerm, String locationId) {
        try {
            Response res = searchTestsAndPackages(token, searchTerm, locationId);
            if (res.getStatusCode() != 200) return new HashMap<>();
            
            // Try different response structures
            List<Map<String, Object>> results = res.jsonPath().getList("data.results");
            if (results == null) results = res.jsonPath().getList("data");
            if (results == null) results = res.jsonPath().getList("tests");
            
            if (results != null && !results.isEmpty()) {
                for (Map<String, Object> result : results) {
                    String name = (String) result.get("name");
                    if (name != null && name.equalsIgnoreCase(searchTerm)) {
                        return result;
                    }
                }
                // Return first result if exact match not found
                return results.get(0);
            }
        } catch (Exception e) {
            // Graceful fallback
        }
        return new HashMap<>();
    }
}
