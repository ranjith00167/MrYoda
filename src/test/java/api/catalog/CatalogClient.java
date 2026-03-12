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
            
            List<Map<String, Object>> tests = extractEntityList(
                testsResponse,
                "data",
                "data.tests",
                "tests",
                "data.results",
                "results"
            );
            
            if (tests != null) {
                for (Map<String, Object> test : tests) {
                    if (test == null) continue;
                    
                    String testName = (String) test.get("test_name");
                    if (testName == null) testName = (String) test.get("name");
                    if (testName == null) testName = (String) test.get("testName");
                    
                    double price = extractPriceFromEntity(test);

                    // Add test if we found a name
                    if (testName != null) {
                        testPricing.put(testName, price);
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
            
            List<Map<String, Object>> packages = extractEntityList(
                packagesResponse,
                "data",
                "data.packages",
                "packages",
                "data.results",
                "results"
            );
            
            if (packages != null) {
                for (Map<String, Object> pkg : packages) {
                    if (pkg == null) continue;
                    
                    String packageName = (String) pkg.get("name");
                    if (packageName == null) packageName = (String) pkg.get("packageName");
                    if (packageName == null) packageName = (String) pkg.get("package_name");

                    double price = extractPriceFromEntity(pkg);
                    
                    // Add package if we found a name
                    if (packageName != null) {
                        packagePricing.put(packageName, price);
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

    private List<Map<String, Object>> extractEntityList(Response response, String... paths) {
        List<Map<String, Object>> entities = new ArrayList<>();
        if (response == null || paths == null) {
            return entities;
        }

        for (String path : paths) {
            Object raw = response.jsonPath().get(path);
            List<Map<String, Object>> mapped = toMapList(raw);
            if (!mapped.isEmpty()) {
                return mapped;
            }
        }

        return entities;
    }

    private List<Map<String, Object>> toMapList(Object raw) {
        List<Map<String, Object>> mapped = new ArrayList<>();
        if (raw instanceof List) {
            List<?> list = (List<?>) raw;
            for (Object item : list) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> casted = (Map<String, Object>) item;
                    mapped.add(casted);
                }
            }
        } else if (raw instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) raw;
            mapped.add(casted);
        }
        return mapped;
    }

    private Double tryParseDouble(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof Number) {
            return ((Number) rawValue).doubleValue();
        }

        String value = rawValue.toString().trim();
        if (value.isEmpty()) {
            return null;
        }

        value = value.replace(",", "").replaceAll("[^0-9.\\-]", "");
        if (value.isEmpty() || "-".equals(value) || ".".equals(value)) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Double readNumericField(Map<?, ?> source, String key) {
        if (source == null || !source.containsKey(key)) {
            return null;
        }

        Object raw = source.get(key);
        Double direct = tryParseDouble(raw);
        if (direct != null) {
            return direct;
        }

        if (raw instanceof Map) {
            Map<?, ?> nested = (Map<?, ?>) raw;
            String[] nestedKeys = { "price", "amount", "value", "selling_price", "actual_price", "mrp" };
            for (String nestedKey : nestedKeys) {
                Double nestedValue = readNumericField(nested, nestedKey);
                if (nestedValue != null) {
                    return nestedValue;
                }
            }
        }

        if (raw instanceof List) {
            List<?> list = (List<?>) raw;
            for (Object item : list) {
                Double listValue = tryParseDouble(item);
                if (listValue != null) {
                    return listValue;
                }
                if (item instanceof Map) {
                    Map<?, ?> nestedItem = (Map<?, ?>) item;
                    Double nestedListValue = readNumericField(nestedItem, "price");
                    if (nestedListValue == null) {
                        nestedListValue = readNumericField(nestedItem, "amount");
                    }
                    if (nestedListValue != null) {
                        return nestedListValue;
                    }
                }
            }
        }

        return null;
    }

    private double extractPriceFromEntity(Map<String, Object> entity) {
        if (entity == null || entity.isEmpty()) {
            return 0.0;
        }

        String[] directKeys = {
            "price", "selling_price", "sellingPrice", "actual_price", "actualPrice",
            "offer_price", "offerPrice", "final_price", "finalPrice", "discounted_price",
            "amount", "total", "mrp", "package_price", "packagePrice", "test_price", "testPrice"
        };

        for (String key : directKeys) {
            Double value = readNumericField(entity, key);
            if (value != null) {
                return value;
            }
        }

        String[] nestedKeys = {
            "pricing", "prices", "price_info", "priceInfo", "details",
            "member_price", "memberPrice", "non_member_price", "nonMemberPrice"
        };

        for (String nestedKey : nestedKeys) {
            Object nestedObj = entity.get(nestedKey);
            if (nestedObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) nestedObj;
                double nestedPrice = extractPriceFromEntity(nestedMap);
                if (nestedPrice > 0) {
                    return nestedPrice;
                }
            }
        }

        return 0.0;
    }
}
