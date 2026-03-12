package com.mryoda.diagnostics.api.utils;

import api.catalog.CatalogClient;
import io.restassured.response.Response;
import com.mryoda.diagnostics.api.utils.LoggerUtil;

import java.util.*;

/**
 * Utility to resolve package names to their individual component tests.
 * Used to validate that all package components are included in visit validation.
 */
public class PackageComponentResolver {
    
    private static final CatalogClient catalogClient = new CatalogClient();
    
    // Cache to avoid repeated API calls for same package
    private static final Map<String, List<String>> componentCache = new HashMap<>();
    
    /**
     * Resolve a package name to its component test names
     * @param packageName Package name (e.g., "Anemia Panel")
     * @param locationId Location ID for the package
     * @param token Authentication token
     * @return List of component test names, empty list if package not found
     */
    public static List<String> resolvePackageComponents(String packageName, String locationId, String token) {
        if (packageName == null || packageName.isEmpty() || locationId == null || token == null) {
            LoggerUtil.warn("PackageComponentResolver: Invalid parameters - packageName=" + packageName + 
                       ", locationId=" + locationId + ", token=" + (token != null ? "***" : "null"));
            return new ArrayList<>();
        }
        
        String cacheKey = packageName.toLowerCase() + "_" + locationId;
        
        // Check cache first
        if (componentCache.containsKey(cacheKey)) {
            LoggerUtil.debug("PackageComponentResolver: Using cached components for: " + packageName);
            return componentCache.get(cacheKey);
        }
        
        try {
            LoggerUtil.info("PackageComponentResolver: Resolving package: " + packageName + 
                       " for locationId: " + locationId);
            
            // Search for package by name
            Map<String, Object> packageMap = catalogClient.searchPackageByName(token, locationId, packageName);
            
            if (packageMap == null) {
                LoggerUtil.warn("PackageComponentResolver: Package not found in API: " + packageName);
                return new ArrayList<>();
            }
            
            // Extract components from package
            List<String> components = catalogClient.extractPackageComponents(packageMap);
            LoggerUtil.info("PackageComponentResolver: Found " + components.size() + 
                       " components for package '" + packageName + "': " + components);
            
            // Cache the result
            componentCache.put(cacheKey, components);
            
            return components;
            
        } catch (Exception e) {
            LoggerUtil.error("PackageComponentResolver: Error resolving package: " + packageName, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Resolve multiple package names to their component tests
     * @param packageNames Collection of package names
     * @param locationId Location ID
     * @param token Authentication token
     * @return Map of packageName -> List of component tests
     */
    public static Map<String, List<String>> resolveMultiplePackages(Collection<String> packageNames, 
                                                                     String locationId, String token) {
        Map<String, List<String>> results = new HashMap<>();
        for (String packageName : packageNames) {
            results.put(packageName, resolvePackageComponents(packageName, locationId, token));
        }
        return results;
    }
    
    /**
     * Check if a package name is actually a package (vs individual test name)
     * @param testName Test or package name
     * @param locationId Location ID
     * @param token Authentication token
     * @return true if it's a package, false otherwise
     */
    public static boolean isPackage(String testName, String locationId, String token) {
        try {
            Map<String, Object> packageMap = catalogClient.searchPackageByName(token, locationId, testName);
            return packageMap != null;
        } catch (Exception e) {
            LoggerUtil.error("PackageComponentResolver: isPackage check failed for: " + testName + ". Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the price of a package if available
     * @param packageName Package name
     * @param locationId Location ID
     * @param token Authentication token
     * @return Package price or 0.0 if not found
     */
    public static double getPackagePrice(String packageName, String locationId, String token) {
        try {
            Map<String, Object> packageMap = catalogClient.searchPackageByName(token, locationId, packageName);
            if (packageMap != null) {
                Object price = packageMap.get("price");
                if (price instanceof Number) {
                    return ((Number) price).doubleValue();
                } else if (price instanceof String) {
                    return Double.parseDouble((String) price);
                }
            }
        } catch (Exception e) {
            LoggerUtil.error("PackageComponentResolver: Error getting package price: " + packageName + ". Error: " + e.getMessage());
        }
        return 0.0;
    }
    
    /**
     * Merge individual test names with package components
     * @param testNames Collection of test names and/or package names
     * @param locationId Location ID
     * @param token Authentication token
     * @return Flattened set of all individual test names (packages expanded to components)
     */
    public static Set<String> flattenTestNames(Collection<String> testNames, String locationId, String token) {
        Set<String> flattened = new HashSet<>();
        
        for (String name : testNames) {
            List<String> components = resolvePackageComponents(name, locationId, token);
            if (!components.isEmpty()) {
                // It's a package - add all components
                flattened.addAll(components);
                LoggerUtil.debug("PackageComponentResolver: Expanded package '" + name + 
                           "' to components: " + components);
            } else {
                // It's likely an individual test - add as-is
                flattened.add(name);
            }
        }
        
        return flattened;
    }
    
    /**
     * Clear the component cache (useful for testing or after catalog updates)
     */
    public static void clearCache() {
        componentCache.clear();
        LoggerUtil.info("PackageComponentResolver: Cache cleared");
    }
    
    /**
     * Get brand information for package filtering
     * @param token Authentication token
     * @param brandName Brand name to search for
     * @return Brand map if found, null otherwise
     */
    public static Map<String, Object> getBrandInfo(String token, String brandName) {
        if (brandName == null || token == null) {
            LoggerUtil.warn("PackageComponentResolver: Invalid parameters for getBrandInfo");
            return null;
        }
        
        try {
            LoggerUtil.debug("PackageComponentResolver: Fetching brand info for: " + brandName);
            Map<String, Object> brand = catalogClient.searchBrandByName(token, brandName);
            if (brand != null) {
                LoggerUtil.info("PackageComponentResolver: Found brand: " + brandName);
                return brand;
            } else {
                LoggerUtil.warn("PackageComponentResolver: Brand not found: " + brandName);
            }
        } catch (Exception e) {
            LoggerUtil.error("PackageComponentResolver: Error fetching brand: " + brandName, e);
        }
        return null;
    }
    
    /**
     * Get brand ID for package filtering
     * @param token Authentication token
     * @param brandName Brand name
     * @return Brand ID if found, null otherwise
     */
    public static String getBrandId(String token, String brandName) {
        try {
            String brandId = catalogClient.getBrandId(token, brandName);
            if (brandId != null) {
                LoggerUtil.debug("PackageComponentResolver: Brand '" + brandName + "' has ID: " + brandId);
            }
            return brandId;
        } catch (Exception e) {
            LoggerUtil.error("PackageComponentResolver: Error getting brand ID for: " + brandName, e);
        }
        return null;
    }
    
    /**
     * Get all test pricing information for a location
     * @param locationId Location ID
     * @param token Authentication token
     * @return Map of TestName → Price
     */
    public static Map<String, Double> getTestPricing(String locationId, String token) {
        Map<String, Double> testPricing = new HashMap<>();
        try {
            int maxPages = 5;
            for (int page = 1; page <= maxPages; page++) {
                Response res = new api.catalog.CatalogClient().getAllTestsWithPricing(token, locationId, page);
                if (res.getStatusCode() != 200) break;
                
                Map<String, Double> pagePricing = new api.catalog.CatalogClient().extractTestPricing(res);
                testPricing.putAll(pagePricing);
                
                // Check if there are more pages (safe numeric extraction)
                Object totalObj = res.jsonPath().get("total");
                if (!(totalObj instanceof Number)) {
                    totalObj = res.jsonPath().get("data.total");
                }
                int total = (totalObj instanceof Number) ? ((Number) totalObj).intValue() : 0;
                if (total > 0 && testPricing.size() >= total) break;
            }
            LoggerUtil.info("PackageComponentResolver: Retrieved pricing for " + testPricing.size() + " tests");
        } catch (Exception e) {
            LoggerUtil.error("PackageComponentResolver: Error fetching test pricing from GET_ALL_TESTS", e);
        }
        return testPricing;
    }
    
    /**
     * Get all package pricing information for a location
     * @param locationId Location ID
     * @param token Authentication token
     * @return Map of PackageName → Price
     */
    public static Map<String, Double> getPackagePricing(String locationId, String token) {
        Map<String, Double> packagePricing = new HashMap<>();
        try {
            int maxPages = 5;
            for (int page = 1; page <= maxPages; page++) {
                Response res = new api.catalog.CatalogClient().getAllPackages(token, locationId, page);
                if (res.getStatusCode() != 200) break;
                
                Map<String, Double> pagePricing = new api.catalog.CatalogClient().extractPackagePricing(res);
                packagePricing.putAll(pagePricing);
                
                // Check if there are more pages (safe numeric extraction)
                Object totalObj = res.jsonPath().get("total");
                if (!(totalObj instanceof Number)) {
                    totalObj = res.jsonPath().get("data.total");
                }
                int total = (totalObj instanceof Number) ? ((Number) totalObj).intValue() : 0;
                if (total > 0 && packagePricing.size() >= total) break;
            }
            LoggerUtil.info("PackageComponentResolver: Retrieved pricing for " + packagePricing.size() + " packages");
        } catch (Exception e) {
            LoggerUtil.error("PackageComponentResolver: Error fetching package pricing from GET_ALL_PACKAGES", e);
        }
        return packagePricing;
    }
    
    /**
     * Search for test/package using GLOBAL_SEARCH endpoint
     * @param searchTerm Search keyword
     * @param locationId Location ID
     * @param token Authentication token
     * @return Map with details if found, empty map otherwise
     */
    public static Map<String, Object> searchGlobal(String searchTerm, String locationId, String token) {
        try {
            Map<String, Object> result = new api.catalog.CatalogClient()
                .findTestOrPackageDetails(token, searchTerm, locationId);
            if (result != null && !result.isEmpty()) {
                LoggerUtil.info("PackageComponentResolver: GLOBAL_SEARCH found '" + searchTerm + "'");
                return result;
            }
        } catch (Exception e) {
            LoggerUtil.error("PackageComponentResolver: Error in GLOBAL_SEARCH for: " + searchTerm, e);
        }
        return new HashMap<>();
    }
}
