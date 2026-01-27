package com.mryoda.diagnostics.api.utils;

import com.mryoda.diagnostics.api.builders.RequestBuilder;
import com.mryoda.diagnostics.api.endpoints.APIEndpoints;
import io.restassured.response.Response;

import java.util.*;

public class GlobalSearchHelper {

    /**
     * Helper method to capitalize first letter of each word
     */
    private static String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        String[] words = str.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
                result.append(" ");
            }
        }
        return result.toString().trim();
    }

    /**
     * Perform global test search using search string and selected location
     */
    public static Response searchTests(String searchString, String locationTitle) {

        RequestContext.setSelectedLocation(locationTitle);
        String locationId = RequestContext.getSelectedLocationId();

        // Get token with fallback strategy
        String token = RequestContext.getMemberToken();
        if (token == null)
            token = RequestContext.getNonMemberToken();
        if (token == null)
            token = RequestContext.getNewUserToken();
        if (token == null)
            token = RequestContext.getToken();

        // Verify token is not null
        if (token == null) {
            throw new RuntimeException("❌ Token is null! Please login first to generate a token.");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("page", 1);
        body.put("limit", 50);
        body.put("search_string", searchString);
        body.put("sort_by", "Type");
        body.put("location", locationId);

        System.out.println("\n🔍 SEARCHING TESTS → '" + searchString + "' @ Location: " + locationTitle);

        return new RequestBuilder()
                .setEndpoint(APIEndpoints.GLOBAL_SEARCH)
                .addHeader("Authorization", "Bearer " + token)
                .setRequestBody(body)
                .expectStatus(200)
                .post();
    }

    /**
     * Extract only the required tests & store them in RequestContext
     * Maps all fields from API response JSON structure
     * Uses tests already found and stored by searchTestsByFullNames
     */
    public static void extractAndStoreTests(Response response, String[] requiredTests) {

        // Use tests already stored from searchTestsByFullNames
        List<Map<String, Object>> allTests = RequestContext.getGlobalTests();

        if (allTests == null || allTests.isEmpty()) {
            System.out.println("⚠️ No tests found in RequestContext.getGlobalTests()");
            allTests = new ArrayList<>();
        }

        System.out.println("\n📦 EXTRACTING & STORING TEST DETAILS");
        System.out.println("   Tests already found: " + allTests.size());

        for (String testName : requiredTests) {

            Map<String, Object> found = allTests.stream()
                    .filter(t -> {
                        Object testNameObj = t.get("test_name");
                        if (testNameObj == null) {
                            return false;
                        }
                        String testNameInList = testNameObj.toString();

                        // Normalize both names for comparison
                        String normalizedList = testNameInList.trim().replaceAll("\\s+", " ");
                        String normalizedSearch = testName.trim().replaceAll("\\s+", " ");

                        // Also compare without dashes (e.g., "Profile -1" <=> "Profile 1")
                        String listNoDash = normalizedList.replaceAll("\\s*-\\s*", " ");
                        String searchNoDash = normalizedSearch.replaceAll("\\s*-\\s*", " ");

                        return normalizedList.equalsIgnoreCase(normalizedSearch) ||
                                listNoDash.equalsIgnoreCase(searchNoDash);
                    })
                    .findFirst()
                    .orElse(null);

            if (found == null) {
                System.out.println("\n❌ Test NOT found: " + testName);
                System.out.println("   This test was not found during search.");
                System.out.println("   Skipping this test...");
                continue; // Skip instead of failing the whole test
            }

            // Get the actual test name from the found test (might be different due to dash
            // normalization)
            String actualTestName = (String) found.get("test_name");
            if (!actualTestName.equalsIgnoreCase(testName)) {
                System.out.println(
                        "\n   ℹ️  Note: Searching for \"" + testName + "\", found as \"" + actualTestName + "\"");
            }

            // Extract ALL fields from the JSON response
            Map<String, Object> storeData = new HashMap<>();

            // Basic fields
            storeData.put("_id", found.get("_id"));
            storeData.put("test_id", found.get("test_id"));
            storeData.put("test_name", found.get("test_name"));
            storeData.put("slug", found.get("slug"));
            storeData.put("Type", found.get("Type"));
            storeData.put("status", found.get("status"));

            // Pricing fields
            storeData.put("price", found.get("price") != null ? found.get("price") : 0);
            storeData.put("original_price", found.get("original_price") != null ? found.get("original_price") : 0);
            storeData.put("b2b_price", found.get("b2b_price"));
            storeData.put("discount_percentage",
                    found.get("discount_percentage") != null ? found.get("discount_percentage") : 0);
            storeData.put("discount_rate", found.get("discount_rate"));
            storeData.put("rewards_percentage", found.get("rewards_percentage"));
            storeData.put("membership_discount",
                    found.get("membership_discount") != null ? found.get("membership_discount") : 0);
            storeData.put("courier_charges", found.get("courier_charges") != null ? found.get("courier_charges") : 0);
            storeData.put("cpt_price", found.get("cpt_price") != null ? found.get("cpt_price") : 0);
            storeData.put("actual_cprt_price",
                    found.get("actual_cprt_price") != null ? found.get("actual_cprt_price") : 0);
            storeData.put("cpt_comment", found.get("cpt_comment"));

            // Details fields
            storeData.put("specimen", found.get("specimen"));
            storeData.put("turn_around_time", found.get("turn_around_time"));
            storeData.put("home_collection", found.get("home_collection"));
            storeData.put("pre_test_information", found.get("pre_test_information"));
            storeData.put("description", found.get("description"));
            storeData.put("comment", found.get("comment"));
            storeData.put("usage", found.get("usage"));
            storeData.put("result_interpretation", found.get("result_interpretation"));

            // Boolean flags
            storeData.put("popular", found.get("popular") != null ? found.get("popular") : false);
            storeData.put("speciality_tests",
                    found.get("speciality_tests") != null ? found.get("speciality_tests") : false);
            storeData.put("frequently_booked",
                    found.get("frequently_booked") != null ? found.get("frequently_booked") : false);

            // Array/List fields
            storeData.put("components", found.get("components"));
            storeData.put("locations", found.get("locations"));
            storeData.put("genders", found.get("genders"));
            storeData.put("business_type", found.get("business_type"));
            storeData.put("stability", found.get("stability"));
            storeData.put("method", found.get("method"));
            storeData.put("organ", found.get("organ"));
            storeData.put("diseases", found.get("diseases"));
            storeData.put("search_keywords", found.get("search_keywords"));
            storeData.put("other_names", found.get("other_names"));
            storeData.put("frequently_asked_questions", found.get("frequently_asked_questions"));
            storeData.put("department", found.get("department"));
            storeData.put("doctor_speciality", found.get("doctor_speciality"));
            storeData.put("doctorsSpeciality", found.get("doctorsSpeciality")); // Note: different from
                                                                                // doctor_speciality

            // Timestamps
            storeData.put("createdAt", found.get("createdAt"));
            storeData.put("updatedAt", found.get("updatedAt"));

            // Other fields
            storeData.put("index", found.get("index"));

            // Store the complete raw object for reference
            storeData.put("raw", found);

            // Store ONLY under the ACTUAL name (not both) to avoid duplicate cart entries
            // Using actual name ensures we don't add the same product_id twice
            RequestContext.storeTest(actualTestName, storeData);

            // Log if search name was different
            if (!actualTestName.equalsIgnoreCase(testName)) {
                System.out.println(
                        "\n   ℹ️  Note: Searching for \"" + testName + "\", found as \"" + actualTestName + "\"");
                System.out.println("   ✅ Stored under actual name only: \"" + actualTestName
                        + "\" (prevents duplicate cart entries)");
            }

            System.out.println("\n🎯 MATCHED & STORED TEST: " + actualTestName);
            System.out.println("   Test ID       : " + storeData.get("test_id"));
            System.out.println("   Product ID    : " + storeData.get("_id"));
            System.out.println("   Price         : ₹" + storeData.get("price"));
            System.out.println("   Original Price: ₹" + storeData.get("original_price"));
            System.out.println("   Type          : " + storeData.get("Type"));
            System.out.println("   Status        : " + storeData.get("status"));

            // Display home collection status properly
            Object homeCollectionValue = storeData.get("home_collection");
            String homeCollectionDisplay = "NOT SET";
            if (homeCollectionValue != null) {
                String homeCollectionStr = homeCollectionValue.toString().trim();
                if ("AVAILABLE".equalsIgnoreCase(homeCollectionStr) ||
                        "true".equalsIgnoreCase(homeCollectionStr) ||
                        "yes".equalsIgnoreCase(homeCollectionStr) ||
                        "1".equals(homeCollectionStr)) {
                    homeCollectionDisplay = "✅ AVAILABLE";
                } else if ("NOT AVAILABLE".equalsIgnoreCase(homeCollectionStr) ||
                        "false".equalsIgnoreCase(homeCollectionStr) ||
                        "no".equalsIgnoreCase(homeCollectionStr) ||
                        "0".equals(homeCollectionStr)) {
                    homeCollectionDisplay = "❌ NOT AVAILABLE";
                } else {
                    homeCollectionDisplay = homeCollectionStr;
                }
            }
            System.out.println("   Home Collection: " + homeCollectionDisplay);
        }

        System.out.println("\n✅ All requested tests extracted and stored successfully!");
    }

    /**
     * Search for multiple tests - searches each test individually with its FULL
     * NAME
     */
    public static Response searchTestsByFullNames(String[] fullTestNames, String locationName) {

        if (fullTestNames == null || fullTestNames.length == 0) {
            throw new RuntimeException("❌ No test names provided to search!");
        }

        System.out.println("\n🔍 SEARCHING FOR " + fullTestNames.length + " TESTS");

        // Set location
        RequestContext.setSelectedLocation(locationName);
        String locationId = RequestContext.getSelectedLocationId();
        System.out.println("📌 LOCATION: " + locationName + " → " + locationId);

        // Get token with fallback strategy
        String token = RequestContext.getMemberToken();
        if (token == null)
            token = RequestContext.getNonMemberToken();
        if (token == null)
            token = RequestContext.getNewUserToken();
        if (token == null)
            token = RequestContext.getToken();
        if (token == null) {
            throw new RuntimeException("❌ Token is null! Please login first.");
        }

        // Combined list to store all found tests
        List<Map<String, Object>> allFoundTests = new ArrayList<>();
        Set<String> addedTestIds = new HashSet<>(); // To avoid duplicates

        // Search for each test individually using FULL TEST NAME with RETRY LOGIC
        for (String testName : fullTestNames) {

            System.out.println("\n🔍 Searching: " + testName);
            boolean found = false;

            // Create search variations to try (for cases like "Bone Profile -1")
            List<String> searchVariations = new ArrayList<>();
            searchVariations.add(testName); // Original name
            searchVariations.add(testName.replace(" -", "-")); // Remove space before dash
            searchVariations.add(testName.replace("- ", "-")); // Remove space after dash
            searchVariations.add(testName.replace(" - ", "-")); // Remove spaces around dash
            searchVariations.add(testName.replaceAll("\\s+", " ")); // Normalize multiple spaces

            // If name contains dash, also try without the part after dash
            if (testName.contains("-")) {
                String[] parts = testName.split("-");
                if (parts.length > 0) {
                    searchVariations.add(parts[0].trim()); // e.g., "Bone Profile"
                }
            }

            // Try each variation until we find a match
            for (int attempt = 0; attempt < searchVariations.size() && !found; attempt++) {
                String searchString = searchVariations.get(attempt);

                System.out.println("\n   🔄 Attempt " + (attempt + 1) + "/" + searchVariations.size());
                System.out.println("   Search String: \"" + searchString + "\"");

                // Call Global Search API
                Response response = new RequestBuilder()
                        .setEndpoint(APIEndpoints.GLOBAL_SEARCH)
                        .addHeader("Authorization", "Bearer " + token)
                        .addBodyParam("page", 1)
                        .addBodyParam("limit", 50)
                        .addBodyParam("search_string", searchString)
                        .addBodyParam("sort_by", "Type")
                        .addBodyParam("location", locationId)
                        .expectStatus(200)
                        .post();

                // Extract tests from this response
                List<Map<String, Object>> testsInResponse = response.jsonPath().getList("data");

                if (testsInResponse != null && !testsInResponse.isEmpty()) {
                    System.out.println("   📊 API returned " + testsInResponse.size() + " results");

                    // 🔍 DEBUG: Print ALL results to see what we're getting
                    System.out.println("\n   🔍 DEBUG - ALL RESULTS for search \"" + searchString + "\":");
                    for (int i = 0; i < Math.min(5, testsInResponse.size()); i++) {
                        Map<String, Object> debugTest = testsInResponse.get(i);
                        System.out.println("      Result " + (i + 1) + ":");
                        System.out.println("         test_name: " + debugTest.get("test_name"));
                        System.out.println("         _id: " + debugTest.get("_id"));
                        System.out.println("         test_id: " + debugTest.get("test_id"));
                        System.out.println("         slug: " + debugTest.get("slug"));
                        System.out.println("         Type: " + debugTest.get("Type"));
                    }
                    System.out.println();

                    // Look for exact match first, then partial match
                    for (Map<String, Object> test : testsInResponse) {
                        String testNameInResponse = (String) test.get("test_name");
                        String testId = (String) test.get("_id");

                        // 🔍 If test_name is null, try alternative fields
                        if (testNameInResponse == null) {
                            System.out.println("   ⚠️  test_name is NULL for ID: " + testId);
                            System.out.println("      Checking alternative fields...");

                            // Try product_name or other name fields
                            testNameInResponse = (String) test.get("product_name");
                            if (testNameInResponse == null) {
                                testNameInResponse = (String) test.get("name");
                            }
                            if (testNameInResponse == null) {
                                testNameInResponse = (String) test.get("title");
                            }

                            // If still null, try to construct from slug
                            if (testNameInResponse == null) {
                                String slug = (String) test.get("slug");
                                if (slug != null) {
                                    // Convert slug to readable name (e.g., "bone-profile-1" -> "Bone Profile 1")
                                    testNameInResponse = slug.replace("-", " ").replace("_", " ");
                                    testNameInResponse = capitalizeWords(testNameInResponse);
                                    System.out.println("      ℹ️  Constructed name from slug: " + testNameInResponse);
                                    // Also store it in the test object for later use
                                    test.put("test_name", testNameInResponse);
                                }
                            }

                            if (testNameInResponse != null) {
                                System.out.println("      ✅ Found alternative name: " + testNameInResponse);
                            }
                        }

                        if (testNameInResponse != null && testId != null) {
                            // Normalize both names for comparison
                            String normalizedResponse = testNameInResponse.trim().replaceAll("\\s+", " ");
                            String normalizedOriginal = testName.trim().replaceAll("\\s+", " ");

                            // Also create versions without dashes for comparison (e.g., "Profile -1" <=>
                            // "Profile 1")
                            String responseNoDash = normalizedResponse.replaceAll("\\s*-\\s*", " ");
                            String originalNoDash = normalizedOriginal.replaceAll("\\s*-\\s*", " ");

                            // Check for exact match (case insensitive, normalized spaces)
                            if (normalizedResponse.equalsIgnoreCase(normalizedOriginal) ||
                                    responseNoDash.equalsIgnoreCase(originalNoDash)) {
                                // Add only if not already added
                                if (!addedTestIds.contains(testId)) {
                                    allFoundTests.add(test);
                                    addedTestIds.add(testId);
                                    System.out.println("   ✅ EXACT MATCH FOUND: " + testNameInResponse);
                                    System.out.println("      Test ID: " + test.get("test_id"));
                                    System.out.println("      Product ID: " + testId);
                                    System.out
                                            .println("      ✅ SUCCESS with search variation: \"" + searchString + "\"");
                                    if (!normalizedResponse.equalsIgnoreCase(normalizedOriginal)) {
                                        System.out.println("      ℹ️  Matched by removing dash: \"" + testName
                                                + "\" → \"" + testNameInResponse + "\"");
                                    }
                                    found = true;
                                    break;
                                }
                            }
                            // Try contains match as fallback (for partial matches)
                            else if (normalizedResponse.toLowerCase().contains(normalizedOriginal.toLowerCase()) ||
                                    normalizedOriginal.toLowerCase().contains(normalizedResponse.toLowerCase())) {
                                if (!addedTestIds.contains(testId)) {
                                    allFoundTests.add(test);
                                    addedTestIds.add(testId);
                                    System.out.println("   ✅ PARTIAL MATCH FOUND: " + testNameInResponse);
                                    System.out.println("      Test ID: " + test.get("test_id"));
                                    System.out.println("      Product ID: " + testId);
                                    System.out.println("      ⚠️  Note: Using partial match for: \"" + testName + "\"");
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (!found && attempt < searchVariations.size() - 1) {
                        System.out.println("   ⚠️  No match with this variation, trying next...");
                        System.out.println("   ℹ️  Available tests in results:");
                        for (int i = 0; i < Math.min(3, testsInResponse.size()); i++) {
                            System.out.println("      - " + testsInResponse.get(i).get("test_name"));
                        }
                    }
                } else {
                    System.out.println("   ❌ No results returned, trying next variation...");
                }
            }

            // Final status for this test
            if (!found) {
                System.out.println("\n   ❌ FAILED: Could not find \"" + testName + "\" after " + searchVariations.size()
                        + " attempts");
                System.out.println("   ℹ️  Tried variations:");
                for (String variation : searchVariations) {
                    System.out.println("      - \"" + variation + "\"");
                }
            }
        }

        System.out.println("\n📊 ========================================");
        System.out.println("   TOTAL TESTS FOUND: " + allFoundTests.size() + " / " + fullTestNames.length);
        System.out.println("   ========================================");

        // Store all found tests in RequestContext
        RequestContext.storeGlobalTests(allFoundTests);

        // Return the last response (required by method signature)
        // The actual data will be used from RequestContext.getGlobalTests()
        String lastTestName = fullTestNames[fullTestNames.length - 1];
        return new RequestBuilder()
                .setEndpoint(APIEndpoints.GLOBAL_SEARCH)
                .addHeader("Authorization", "Bearer " + token)
                .addBodyParam("page", 1)
                .addBodyParam("limit", 50)
                .addBodyParam("search_string", lastTestName)
                .addBodyParam("sort_by", "Type")
                .addBodyParam("location", locationId)
                .expectStatus(200)
                .post();
    }

    // ============================================================
    // HELPER METHODS TO RETRIEVE STORED TEST FIELDS
    // ============================================================

    /**
     * Get test field value with type safety
     */
    public static Object getTestField(String testName, String fieldName) {
        Map<String, Object> test = RequestContext.getTest(testName);
        return test != null ? test.get(fieldName) : null;
    }

    /**
     * Get test ID (e.g., GEN110)
     */
    public static String getTestId(String testName) {
        Object value = getTestField(testName, "test_id");
        return value != null ? value.toString() : null;
    }

    /**
     * Get product ID (MongoDB _id)
     */
    public static String getProductId(String testName) {
        Object value = getTestField(testName, "_id");
        return value != null ? value.toString() : null;
    }

    /**
     * Get test price
     */
    public static double getTestPrice(String testName) {
        Object value = getTestField(testName, "price");
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    /**
     * Get original price
     */
    public static double getOriginalPrice(String testName) {
        Object value = getTestField(testName, "original_price");
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    /**
     * Get home collection availability
     */
    public static String getHomeCollection(String testName) {
        Object value = getTestField(testName, "home_collection");
        return value != null ? value.toString() : null;
    }

    /**
     * Get test status (ACTIVE/INACTIVE)
     */
    public static String getTestStatus(String testName) {
        Object value = getTestField(testName, "status");
        return value != null ? value.toString() : null;
    }

    /**
     * Get test type (diagnostics, etc.)
     */
    public static String getTestType(String testName) {
        Object value = getTestField(testName, "Type");
        return value != null ? value.toString() : null;
    }

    /**
     * Get slug
     */
    public static String getSlug(String testName) {
        Object value = getTestField(testName, "slug");
        return value != null ? value.toString() : null;
    }

    /**
     * Get discount percentage
     */
    public static double getDiscountPercentage(String testName) {
        Object value = getTestField(testName, "discount_percentage");
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    /**
     * Get discount rate
     */
    public static String getDiscountRate(String testName) {
        Object value = getTestField(testName, "discount_rate");
        return value != null ? value.toString() : null;
    }

    /**
     * Get rewards percentage
     */
    public static String getRewardsPercentage(String testName) {
        Object value = getTestField(testName, "rewards_percentage");
        return value != null ? value.toString() : null;
    }

    /**
     * Get membership discount
     */
    public static double getMembershipDiscount(String testName) {
        Object value = getTestField(testName, "membership_discount");
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    /**
     * Get courier charges
     */
    public static double getCourierCharges(String testName) {
        Object value = getTestField(testName, "courier_charges");
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    /**
     * Get B2B price
     */
    public static Double getB2BPrice(String testName) {
        Object value = getTestField(testName, "b2b_price");
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    /**
     * Get CPT price
     */
    public static double getCPTPrice(String testName) {
        Object value = getTestField(testName, "cpt_price");
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    /**
     * Get actual CPRT price
     */
    public static double getActualCPRTPrice(String testName) {
        Object value = getTestField(testName, "actual_cprt_price");
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    /**
     * Get CPT comment
     */
    public static String getCPTComment(String testName) {
        Object value = getTestField(testName, "cpt_comment");
        return value != null ? value.toString() : null;
    }

    /**
     * Get specimen
     */
    public static String getSpecimen(String testName) {
        Object value = getTestField(testName, "specimen");
        return value != null ? value.toString() : null;
    }

    /**
     * Get turn around time
     */
    public static Object getTurnAroundTime(String testName) {
        return getTestField(testName, "turn_around_time");
    }

    /**
     * Get pre-test information
     */
    public static String getPreTestInformation(String testName) {
        Object value = getTestField(testName, "pre_test_information");
        return value != null ? value.toString() : null;
    }

    /**
     * Get description
     */
    public static String getDescription(String testName) {
        Object value = getTestField(testName, "description");
        return value != null ? value.toString() : null;
    }

    /**
     * Get comment
     */
    public static String getComment(String testName) {
        Object value = getTestField(testName, "comment");
        return value != null ? value.toString() : null;
    }

    /**
     * Get usage
     */
    public static String getUsage(String testName) {
        Object value = getTestField(testName, "usage");
        return value != null ? value.toString() : null;
    }

    /**
     * Get result interpretation
     */
    public static String getResultInterpretation(String testName) {
        Object value = getTestField(testName, "result_interpretation");
        return value != null ? value.toString() : null;
    }

    /**
     * Get popular flag
     */
    public static boolean isPopular(String testName) {
        Object value = getTestField(testName, "popular");
        return value instanceof Boolean ? (Boolean) value : false;
    }

    /**
     * Get speciality tests flag
     */
    public static boolean isSpecialityTest(String testName) {
        Object value = getTestField(testName, "speciality_tests");
        return value instanceof Boolean ? (Boolean) value : false;
    }

    /**
     * Get frequently booked flag
     */
    public static boolean isFrequentlyBooked(String testName) {
        Object value = getTestField(testName, "frequently_booked");
        return value instanceof Boolean ? (Boolean) value : false;
    }

    /**
     * Get components list
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getComponents(String testName) {
        Object value = getTestField(testName, "components");
        return value instanceof List ? (List<Object>) value : new ArrayList<>();
    }

    /**
     * Get locations list
     */
    @SuppressWarnings("unchecked")
    public static List<String> getLocations(String testName) {
        Object value = getTestField(testName, "locations");
        return value instanceof List ? (List<String>) value : new ArrayList<>();
    }

    /**
     * Get genders list
     */
    @SuppressWarnings("unchecked")
    public static List<String> getGenders(String testName) {
        Object value = getTestField(testName, "genders");
        return value instanceof List ? (List<String>) value : new ArrayList<>();
    }

    /**
     * Get business type list
     */
    @SuppressWarnings("unchecked")
    public static List<String> getBusinessType(String testName) {
        Object value = getTestField(testName, "business_type");
        return value instanceof List ? (List<String>) value : new ArrayList<>();
    }

    /**
     * Get stability list
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getStability(String testName) {
        Object value = getTestField(testName, "stability");
        return value instanceof List ? (List<Object>) value : new ArrayList<>();
    }

    /**
     * Get method list
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getMethod(String testName) {
        Object value = getTestField(testName, "method");
        return value instanceof List ? (List<Object>) value : new ArrayList<>();
    }

    /**
     * Get organ list
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getOrgan(String testName) {
        Object value = getTestField(testName, "organ");
        return value instanceof List ? (List<Object>) value : new ArrayList<>();
    }

    /**
     * Get diseases list
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getDiseases(String testName) {
        Object value = getTestField(testName, "diseases");
        return value instanceof List ? (List<Object>) value : new ArrayList<>();
    }

    /**
     * Get search keywords list
     */
    @SuppressWarnings("unchecked")
    public static List<String> getSearchKeywords(String testName) {
        Object value = getTestField(testName, "search_keywords");
        return value instanceof List ? (List<String>) value : new ArrayList<>();
    }

    /**
     * Get other names list
     */
    @SuppressWarnings("unchecked")
    public static List<String> getOtherNames(String testName) {
        Object value = getTestField(testName, "other_names");
        return value instanceof List ? (List<String>) value : new ArrayList<>();
    }

    /**
     * Get frequently asked questions list
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getFrequentlyAskedQuestions(String testName) {
        Object value = getTestField(testName, "frequently_asked_questions");
        return value instanceof List ? (List<Object>) value : new ArrayList<>();
    }

    /**
     * Get department list
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getDepartment(String testName) {
        Object value = getTestField(testName, "department");
        return value instanceof List ? (List<Map<String, Object>>) value : new ArrayList<>();
    }

    /**
     * Get doctor speciality list
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getDoctorSpeciality(String testName) {
        Object value = getTestField(testName, "doctor_speciality");
        return value instanceof List ? (List<Object>) value : new ArrayList<>();
    }

    /**
     * Get doctorsSpeciality list (note: different from doctor_speciality)
     */
    @SuppressWarnings("unchecked")
    public static List<Object> getDoctorsSpeciality(String testName) {
        Object value = getTestField(testName, "doctorsSpeciality");
        return value instanceof List ? (List<Object>) value : new ArrayList<>();
    }

    /**
     * Get createdAt timestamp
     */
    public static String getCreatedAt(String testName) {
        Object value = getTestField(testName, "createdAt");
        return value != null ? value.toString() : null;
    }

    /**
     * Get updatedAt timestamp
     */
    public static String getUpdatedAt(String testName) {
        Object value = getTestField(testName, "updatedAt");
        return value != null ? value.toString() : null;
    }

    /**
     * Get index
     */
    public static Integer getIndex(String testName) {
        Object value = getTestField(testName, "index");
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    /**
     * Get raw test object (complete JSON data)
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getRawTestData(String testName) {
        Object value = getTestField(testName, "raw");
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    /**
     * Print all details of a stored test
     */
    public static void printTestDetails(String testName) {
        Map<String, Object> test = RequestContext.getTest(testName);

        if (test == null) {
            System.out.println("❌ Test not found: " + testName);
            return;
        }

        System.out.println("\n========================================================");
        System.out.println("   TEST DETAILS: " + testName);
        System.out.println("========================================================");
        System.out.println("🆔 Product ID        : " + test.get("_id"));
        System.out.println("🔢 Test ID           : " + test.get("test_id"));
        System.out.println("🧪 Test Name         : " + test.get("test_name"));
        System.out.println("🔗 Slug              : " + test.get("slug"));
        System.out.println("💰 Price             : ₹" + test.get("price"));
        System.out.println("💵 Original Price    : ₹" + test.get("original_price"));
        System.out.println(
                "💳 B2B Price         : " + (test.get("b2b_price") != null ? "₹" + test.get("b2b_price") : "N/A"));
        System.out.println("🏷️  Discount %        : " + test.get("discount_percentage") + "%");
        System.out.println("💸 Discount Rate     : ₹" + test.get("discount_rate"));
        System.out.println("🎁 Rewards %         : " + test.get("rewards_percentage") + "%");
        System.out.println("👥 Membership Disc.  : " + test.get("membership_discount") + "%");
        System.out.println("📦 Courier Charges   : ₹" + test.get("courier_charges"));
        System.out.println("🏠 Home Collection   : " + test.get("home_collection"));
        System.out.println("🧬 Specimen          : " + test.get("specimen"));
        System.out.println("⏰ Turn Around Time  : " + test.get("turn_around_time"));
        System.out.println("📍 Status            : " + test.get("status"));
        System.out.println("🏷️  Type              : " + test.get("Type"));
        System.out.println("⭐ Popular           : " + test.get("popular"));
        System.out.println("🔬 Speciality Tests  : " + test.get("speciality_tests"));
        System.out.println("📊 Frequently Booked : " + test.get("frequently_booked"));
        System.out.println("👫 Genders           : " + test.get("genders"));
        System.out.println("🏢 Business Type     : " + test.get("business_type"));
        System.out.println("📍 Locations Count   : "
                + (test.get("locations") instanceof List ? ((List<?>) test.get("locations")).size() : 0));
        System.out.println("🧩 Components Count  : "
                + (test.get("components") instanceof List ? ((List<?>) test.get("components")).size() : 0));
        System.out.println("🏥 Department        : " + test.get("department"));
        System.out.println("👨‍⚕️ Doctor Speciality : " + test.get("doctor_speciality"));
        System.out.println("👩‍⚕️ Doctors Speciality: " + test.get("doctorsSpeciality"));
        System.out.println("🔬 Method            : " + test.get("method"));
        System.out.println("🫀 Organ             : " + test.get("organ"));
        System.out.println("🦠 Diseases          : " + test.get("diseases"));
        System.out.println("🔍 Search Keywords   : " + test.get("search_keywords"));
        System.out.println("📝 Other Names       : " + test.get("other_names"));
        System.out.println("❓ FAQs Count        : " + (test.get("frequently_asked_questions") instanceof List
                ? ((List<?>) test.get("frequently_asked_questions")).size()
                : 0));
        System.out.println("📝 Description       : "
                + (test.get("description") != null && !test.get("description").toString().isEmpty()
                        ? test.get("description")
                        : "N/A"));
        System.out.println("ℹ️  Pre-Test Info     : "
                + (test.get("pre_test_information") != null && !test.get("pre_test_information").toString().isEmpty()
                        ? test.get("pre_test_information")
                        : "N/A"));
        System.out.println("💬 Comment           : "
                + (test.get("comment") != null && !test.get("comment").toString().isEmpty() ? test.get("comment")
                        : "N/A"));
        System.out.println("📋 Usage             : "
                + (test.get("usage") != null && !test.get("usage").toString().isEmpty() ? test.get("usage") : "N/A"));
        System.out.println("📊 Result Interpret. : "
                + (test.get("result_interpretation") != null && !test.get("result_interpretation").toString().isEmpty()
                        ? test.get("result_interpretation")
                        : "N/A"));
        System.out.println("💳 CPT Comment       : "
                + (test.get("cpt_comment") != null && !test.get("cpt_comment").toString().isEmpty()
                        ? test.get("cpt_comment")
                        : "N/A"));
        System.out.println("💰 CPT Price         : ₹" + test.get("cpt_price"));
        System.out.println("💵 Actual CPRT Price : ₹" + test.get("actual_cprt_price"));
        System.out.println("🔢 Index             : " + test.get("index"));
        System.out.println("📅 Created At        : " + test.get("createdAt"));
        System.out.println("🔄 Updated At        : " + test.get("updatedAt"));
        System.out.println("════════════════════════════════════════════════════════\n");
    }
}
