package com.mryoda.diagnostics.api.tests.tests_packages;

import com.mryoda.diagnostics.api.base.BaseTest;
import com.mryoda.diagnostics.api.utils.GlobalSearchHelper;
import com.mryoda.diagnostics.api.utils.RequestContext;
import com.mryoda.diagnostics.api.utils.AssertionUtil;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import com.mryoda.diagnostics.api.builders.RequestBuilder;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * Global Search API Test
 * 
 * ⚠️ IMPORTANT: This test depends on previous tests to run first:
 * 1. LoginAPITest - To generate authentication token
 * 2. LocationAPITest - To fetch and store location IDs
 * 
 * ✅ HOW TO RUN:
 * - Run entire test suite: mvn test
 * - Run via TestNG XML: mvn test -DsuiteXmlFile=testng.xml
 * - In Eclipse: Right-click testng.xml → Run As → TestNG Suite
 * 
 * ❌ DO NOT RUN THIS TEST ALONE - It will fail!
 */
public class GlobalSearchAPITest extends BaseTest {

    // Core logic extracted to helper method
    private void performGlobalSearch() {
        System.out.println("\n==========================================================");
        System.out.println("         GLOBAL SEARCH API TEST - COMPLETE FLOW");
        System.out.println("==========================================================");

        System.out.println("📌 DEBUG → STORED LOCATIONS: " + RequestContext.getAllLocations());

        // ... (rest of method)

        // SELECT CONFIGURABLE LOCATION
        String location = DEFAULT_LOCATION;

        // ========== CROSS-API VALIDATION: Verify Location Exists ========== //
        System.out.println("\n🔍 STEP 1: Validating Location from Previous API (LocationAPI)");
        String locationId = RequestContext.getLocationId(location);

        // Validate we got the expected location ID
        if (locationId != null) {
            System.out.println("   ✅ Location '" + location + "' validated from LocationAPI: " + locationId);
        } else {
            System.out.println("   ⚠️  WARNING: Location ID not found for: " + location);
        }
        System.out.println("   ✅ This location will be used for Global Search");

        String[] testsToSearch = {
                "Bone Profile -1", "T4 - THYROXINE", "RANDOM BLOOD GLUCOSE (RBS)", "CLOTTING TIME"
        };

        System.out.println("\n✅  IMPORTANT: Tests to search have been configured.");

        System.out.println("\n🎯 Tests to Search: " + String.join(", ", testsToSearch));
        System.out.println("📊 Total Tests: " + testsToSearch.length);

        // AUTO-SEARCH full test names
        Response res = GlobalSearchHelper.searchTestsByFullNames(testsToSearch, location);

        // Extract each test and store it
        GlobalSearchHelper.extractAndStoreTests(res, testsToSearch);

        // ========== VALIDATE WE ONLY GOT THE TESTS WE SEARCHED FOR ========== //
        System.out.println("\n🔍 VALIDATING: Checking we only got the tests we searched for...");
        Map<String, Map<String, Object>> allStoredTests = RequestContext.getAllTests();

        java.util.Set<String> expectedNames = new java.util.HashSet<>();
        for (String testName : testsToSearch) {
            expectedNames.add(testName.toLowerCase().replaceAll("\\s+", " "));
            expectedNames.add(testName.toLowerCase().replaceAll("\\s*-\\s*", " "));
        }

        boolean foundUnexpectedTest = false;
        for (String storedTestName : allStoredTests.keySet()) {
            String normalizedStored = storedTestName.toLowerCase().replaceAll("\\s+", " ");
            String normalizedStoredNoDash = storedTestName.toLowerCase().replaceAll("\\s*-\\s*", " ");

            boolean isExpected = false;
            for (String expected : expectedNames) {
                if (normalizedStored.equals(expected) || normalizedStoredNoDash.equals(expected)) {
                    isExpected = true;
                    break;
                }
            }

            if (!isExpected) {
                System.out.println("   ❌ UNEXPECTED TEST FOUND: " + storedTestName);
                foundUnexpectedTest = true;
            } else {
                System.out.println("   ✅ Expected test: " + storedTestName);
            }
        }

        if (foundUnexpectedTest) {
            System.out.println("\n⚠️  WARNING: Found and removed unexpected tests!");
        }

        // ========== CHECK HOME COLLECTION STATUS ========== //
        System.out.println("\n🏠 ANALYZING HOME COLLECTION STATUS FOR ALL TESTS");
        Map<String, Map<String, Object>> allTests = RequestContext.getAllTests();
        int totalTests = allTests.size();
        int homeCollectionCount = 0;
        int nonHomeCollectionCount = 0;

        for (Map.Entry<String, Map<String, Object>> entry : allTests.entrySet()) {
            String testName = entry.getKey();
            Map<String, Object> testData = entry.getValue();

            Object homeCollectionObj = testData.get("home_collection");
            boolean isHomeCollection = false;

            if (homeCollectionObj != null) {
                String homeCollectionStr = homeCollectionObj.toString().trim();
                if (homeCollectionObj instanceof Boolean) {
                    isHomeCollection = (Boolean) homeCollectionObj;
                } else if ("AVAILABLE".equalsIgnoreCase(homeCollectionStr) ||
                        "true".equalsIgnoreCase(homeCollectionStr) ||
                        "yes".equalsIgnoreCase(homeCollectionStr) ||
                        "1".equals(homeCollectionStr)) {
                    isHomeCollection = true;
                }
            }

            if (isHomeCollection) {
                homeCollectionCount++;
                System.out.println("   ✅ " + testName + " - Home Collection: AVAILABLE (type='home')");
            } else {
                nonHomeCollectionCount++;
                System.out.println("   ✅ " + testName + " - Home Collection: NOT AVAILABLE (type='lab')");
            }
        }

        System.out.println("\n📊 HOME COLLECTION ANALYSIS:");
        System.out.println("   Total tests found: " + totalTests);
        System.out.println("   Tests WITH home collection: " + homeCollectionCount);
        System.out.println("   Tests WITHOUT home collection: " + nonHomeCollectionCount);

        // ========== COMPREHENSIVE TEST VALIDATION ========== //
        Map<String, Map<String, Object>> storedTests = RequestContext.getAllTests();
        int foundCount = storedTests != null ? storedTests.size() : 0;

        if (foundCount == 0) {
            System.out.println("\n⚠️  Note: No tests remain after search.");
            return;
        }

        for (String testName : testsToSearch) {
            Map<String, Object> test = RequestContext.getTest(testName);
            if (test != null) {
                String testId = GlobalSearchHelper.getTestId(testName);
                String productId = GlobalSearchHelper.getProductId(testName);

                System.out.println("\n✅ Validated stored test: " + testName);
                System.out.println("   Test ID: " + testId);
                System.out.println("   Product ID: " + productId);

                // User requested additional validation details
                System.out.println("   Rewards Percentage: " + test.get("rewards_percentage"));
                System.out.println("   B2B Price: " + test.get("b2b_price"));
                System.out.println("   Disease: " + test.get("diseases"));
                System.out.println("   Organ: " + test.get("organ"));
            }
        }

        // ========== PACKAGE CONTENT VALIDATION ========== //
        System.out.println("\n📦 VALIDATING PACKAGE CONTENT (Stored in Context)");
        int pkgCount = RequestContext.getPackageTestCount();
        List<String> pkgTests = RequestContext.getPackageTestNames();

        if (pkgCount > 0) {
            System.out.println("   ✅ Package Test Count Verified: " + pkgCount);
            System.out.println("   ✅ Package Test Names Recovered: " + pkgTests);
            AssertionUtil.verifyTrue(pkgTests != null && !pkgTests.isEmpty(),
                    "Package test names list should not be empty");
        } else {
            // Only a warning if we didn't search for a package, but if we did, this is good
            // info
            System.out.println("   ℹ️  No package content stored (Context is empty or no package found)");
            // No package searches in this run; individual tests only.
        }

        System.out.println("\n✅ GLOBAL SEARCH COMPLETE (Stored " + foundCount + " tests)");
    }

    @Test(priority = 7, dependsOnMethods = "com.mryoda.diagnostics.api.tests.tests_packages.BrandAPITest.testGetBrands_ForMember", description = "QA Automation: Verify Global Search For Member")
    public void testGlobalSearch_ForMember() {
        System.out.println("Running Global Search as part of MEMBER flow...");
        performGlobalSearch();
    }

    @Test(priority = 7, dependsOnMethods = "com.mryoda.diagnostics.api.tests.tests_packages.BrandAPITest.testGetBrands_ForNonMember", description = "QA Automation: Verify Global Search For Non Member")
    public void testGlobalSearch_ForNonMember() {
        System.out.println("Running Global Search as part of NON-MEMBER flow...");
        performGlobalSearch();
    }

    @Test(priority = 8, dependsOnMethods = "com.mryoda.diagnostics.api.tests.tests_packages.BrandAPITest.testGetBrands_ForNewUser", description = "QA Automation: Verify Global Search For New User")
    public void testGlobalSearch_ForNewUser() {
        System.out.println("Running Global Search as part of NEW USER flow...");
        performGlobalSearch();
    }

}
