package utilities;

import java.util.*;

/**
 * Strict Price & Test Validation Manager
 * ---------------------------------------
 * Supports:
 *  ✔ Excel-order guaranteed selection capture
 *  ✔ TestName → Price mapping
 *  ✔ Strict index matching between selection & checkout
 *  ✔ Duplicate-test prevention
 *  ✔ Total comparison helpers
 *  ✔ Checkout-only reset and full reset
 */
public class BasePriceManager {

    // -------------------------------------------------------
    //  Test → Price maps (strict)
    // -------------------------------------------------------
    private static final Map<String, Double> selectionTestDetails = new LinkedHashMap<>();
    private static final Map<String, Double> checkoutTestDetails = new LinkedHashMap<>();

    // ====== MEMBER + TEST TRACKING ======
    private static final Map<String, List<String>> memberTestsMap = new LinkedHashMap<>();
    private static final Map<String, Double> unitPriceMap = new LinkedHashMap<>();

    // -------------------------------------------------------
    //  Simple ordered lists (maintain Excel order)
    // -------------------------------------------------------
    private static final List<Double> selectionPrices = new ArrayList<>();
    private static final List<Double> checkoutPrices = new ArrayList<>();

    // -------------------------------------------------------
    //  Clean price: "₹2,200" → 2200.0
    // -------------------------------------------------------
    public static double cleanAndConvert(String raw) {
        if (raw == null) return 0;
        String clean = raw.replaceAll("[^0-9.]", "");
        return clean.isEmpty() ? 0 : Double.parseDouble(clean);
    }

    public static void addSelectionTestPrice(String testName, double price) {
        if (selectionTestDetails.containsKey(testName)) {
            System.out.println("⚠ Duplicate detected (ignored) → " + testName);
            return;
        }
        selectionTestDetails.put(testName, price);
        selectionPrices.add(price);
    }

    // -------------------------------------------------------
    //  Add checkout test (STRICT: prevents duplicates)
    // -------------------------------------------------------
    public static void addCheckoutTestPrice(String testName, double price) {
        if (checkoutTestDetails.containsKey(testName)) {
            throw new AssertionError(
                    "Duplicate test found in CHECKOUT stage → " + testName +
                    "\nCheckout page has the same test listed twice."
            );
        }
        checkoutTestDetails.put(testName, price);
        checkoutPrices.add(price);
    }

    // -------------------------------------------------------
    //  Getters
    // -------------------------------------------------------
    public static Map<String, Double> getSelectionTestDetails() {
        return selectionTestDetails;
    }

    public static Map<String, Double> getCheckoutTestDetails() {
        return checkoutTestDetails;
    }

    public static List<Double> getSelectionPrices() {
        return selectionPrices;
    }

    public static List<Double> getCheckoutPrices() {
        return checkoutPrices;
    }

    // -------------------------------------------------------
    //  Totals
    // -------------------------------------------------------
    public static double getSelectionTotal() {
        return selectionPrices.stream().mapToDouble(Double::doubleValue).sum();
    }

    public static double getCheckoutTotal() {
        return checkoutPrices.stream().mapToDouble(Double::doubleValue).sum();
    }

    // -------------------------------------------------------
    // PARTIAL RESET
    // -------------------------------------------------------
    public static void resetCheckoutOnly() {
        checkoutTestDetails.clear();
        checkoutPrices.clear();
    }

    // -------------------------------------------------------
    // FULL RESET
    // -------------------------------------------------------
    public static void resetAll() {
        selectionTestDetails.clear();
        checkoutTestDetails.clear();
        selectionPrices.clear();
        checkoutPrices.clear();
    }

    // -------------------------------------------------------
    // Helper #1: Selection count must equal Checkout count
    // -------------------------------------------------------
    public static void assertSelectionCountMatchesCheckout() {
        if (selectionPrices.size() != checkoutPrices.size()) {
            throw new AssertionError(
                    "❌ Test Count Mismatch!" +
                    "\nSelection Count: " + selectionPrices.size() +
                    "\nCheckout Count: " + checkoutPrices.size() +
                    "\nExpected EXACT match (same tests, same order)."
            );
        }
    }

    // -------------------------------------------------------
    // Helper #2: Index-wise price comparison
    // -------------------------------------------------------
    public static void assertPricesMatchExactOrder() {
        for (int i = 0; i < selectionPrices.size(); i++) {
            double s = selectionPrices.get(i);
            double c = checkoutPrices.get(i);
            if (Math.abs(s - c) > 0.1) {
                throw new AssertionError(
                        "❌ PRICE MISMATCH at index " + i +
                        "\nSelection Price: ₹" + s +
                        "\nCheckout Price:  ₹" + c +
                        "\nExpected identical values."
                );
            }
        }
    }

    // -------------------------------------------------------
    // Helper #3: Presence check (Excel → Checkout)
    // -------------------------------------------------------
    public static void assertTestPresent(String testName) {
        if (!checkoutTestDetails.containsKey(testName)) {
            throw new AssertionError("❌ Test Missing in Checkout → " + testName);
        }
    }

    // -------------------------------------------------------
    // Helper #4: TestName → Price match
    // -------------------------------------------------------
    public static void assertTestPriceMatches(String testName) {
        if (!selectionTestDetails.containsKey(testName)) {
            throw new AssertionError("❌ Test not captured during selection → " + testName);
        }
        if (!checkoutTestDetails.containsKey(testName)) {
            throw new AssertionError("❌ Test not found in checkout → " + testName);
        }
        double s = selectionTestDetails.get(testName);
        double c = checkoutTestDetails.get(testName);
        if (Math.abs(s - c) > 0.1) {
            throw new AssertionError(
                    "❌ PRICE MISMATCH for " + testName +
                    "\nSelection: ₹" + s +
                    "\nCheckout : ₹" + c
            );
        }
    }

    // -------------------------------------------------------
    // Debug Helpers
    // -------------------------------------------------------
    public static void printSelectionSummary() {
        System.out.println("\n🟦 Selection Summary:");
        selectionTestDetails.forEach((t, p) ->
                System.out.println("   • " + t + " → ₹" + p));
        System.out.println("   TOTAL: ₹" + getSelectionTotal());
    }

    public static void printCheckoutSummary() {
        System.out.println("\n🟩 Checkout Summary:");
        checkoutTestDetails.forEach((t, p) ->
                System.out.println("   • " + t + " → ₹" + p));
        System.out.println("   TOTAL: ₹" + getCheckoutTotal());
    }

    // -------------------------------------------------------
    // Helper #5: Validate savings = 10% of Amount To Pay
    // -------------------------------------------------------
    public static void assertTenPercentSavings(double amountToPayUI, double displayedSavingsUI) {
        double expectedSavings = amountToPayUI * 0.10;
        if (Math.abs(expectedSavings - displayedSavingsUI) > 1) {
            throw new AssertionError(
                    "❌ Savings mismatch (10% validation failed)!" +
                    "\nAmount To Pay (UI): ₹" + amountToPayUI +
                    "\nExpected Savings (10%): ₹" + expectedSavings +
                    "\nDisplayed Savings: ₹" + displayedSavingsUI
            );
        }
        System.out.println("✔ 10% Savings correctly applied and displayed!");
    }

    // -------------------------------------------------------
    // Member + Test Tracking
    // -------------------------------------------------------
    public static void storeMemberTests(String memberKey, List<String> tests) {
        if (memberKey == null || memberKey.trim().isEmpty()) return;
        if (tests == null) tests = new ArrayList<>();
        memberTestsMap.put(memberKey.trim(), new ArrayList<>(tests));
    }

    public static Set<String> getAllMemberNames() {
        return memberTestsMap.keySet();
    }

    public static List<String> getMemberTests(String memberKey) {
        if (memberKey == null) return Collections.emptyList();
        return memberTestsMap.getOrDefault(memberKey.trim(), Collections.emptyList());
    }

    public static void addUnitPrice(String testName, double price) {
        if (testName == null || testName.trim().isEmpty()) return;
        unitPriceMap.put(testName.trim(), price);
    }

    public static double getUnitPrice(String testName) {
        if (testName == null) return 0.0;
        return unitPriceMap.getOrDefault(testName.trim(), 0.0);
    }

    public static Set<String> getAllTestsUnique() {
        return unitPriceMap.keySet();
    }

    public static int getTotalTestCount(String testName) {
        if (testName == null) return 0;
        String target = testName.trim().toLowerCase();
        int count = 0;
        for (List<String> tests : memberTestsMap.values()) {
            for (String t : tests) {
                if (t != null && t.trim().toLowerCase().equals(target)) {
                    count++;
                }
            }
        }
        return count;
    }

    public static void resetMemberTestData() {
        memberTestsMap.clear();
        unitPriceMap.clear();
    }
}
