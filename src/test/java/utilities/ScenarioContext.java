package utilities;

import io.cucumber.java.Scenario;
import java.util.*;

public class ScenarioContext {

    public static Scenario scenario;

    // --- General Data ---
    public static Map<String, String> testData = new HashMap<>();
    public static String orderId;
    public static String caseType;
    public static String accessionId;
    public static String labsquireOrderNumber;
    public static List<Map<String, String>> bulkTestData = new ArrayList<>();
    public static int executionCounter = 0;
    public static boolean isBulkMode = false;
    public static String extractedEmrId;
    public static String extractedFirstName;
    public static String extractedSinNo;

    // --- Panels & Tests ---
    private static final Map<String, Set<String>> panelTests = new HashMap<>();
    private static final List<String> selectedPanels = new ArrayList<>();
    private static final List<String> selectedTests = new ArrayList<>();
 // --- Master Test Tube Mapping ---
    private static final Map<String, String> testTubeMap = new HashMap<>();
    
    // --- All Panels reference ---
    public static final Set<String> allPanels = new HashSet<>(Arrays.asList(
        "Basic Metabolic Panel",
        "Comp Metabolic Panel",
        "Hepatic Profile",
        "Hepatitis",
        "Lipid Panel",
        "Renal Panel",
        "Food Allergens-30",
        "Inhalenat Allergens-36",
        "All Tests" // virtual panel
    ));

    // --- Master All Tests list ---
    public static final Set<String> allTests = new HashSet<>(Arrays.asList(
        "Albumin", "Alk Phos", "ALP", "ALT", "Amylase", "ANTI SARS COV 2 S",
        "Anti-HAV", "Anti-HAV IgM", "Anti-Hbc", "Anti-Hbc IgM", "Anti-HBs",
        "Anti-HCV", "Anti-Tg", "Anti-TPO", "APTT", "AST", "Bilirubin, Direct",
        "Bilirubin, Total", "BUN", "BUN/Crea Ratio", "C-Peptide", "C3", "C4",
        "CA 125", "CA 15-3", "CA 19-9", "Calcium", "Calcium,total",
        "CBC w/Automated Diff", "Chloride", "Cholesterol, Total", "CK", "CO2",
        "Cortisol AM", "CORTISOL PM", "Creatinine", "DHEA-S", "Digoxin",
        "EA IgG", "EBNA IgG", "EBV IgM", "eGFR", "ELECTROLYTE PANEL", "ESR",
        "Estradiol", "FECAL OCCULT BLOOD", "Ferritin", "Fibrinogen", "Folate",
        "Free T3", "Free T4", "FSH", "GGT", "GI PROFILE, STOOL, PCR",
        "Globulin", "Glucose", "H pylori Breath Test",
        "H. pylori IgG, Qualitative", "HBsAg", "HCG+B", "HDL", "Hematocrit",
        "Hemoglobin", "HgbA1C", "HIV Screening", "hsCRP", "HSV 1 IgG",
        "HSV 2 IgG", "IgA", "IgG", "IgM", "INSULIN", "Iron Profile", "Lactate",
        "LDH", "LDL", "LH", "Lipase", "Lithium", "Magnesium", "Measles IgG",
        "Mononucleosis, Qualitative", "Mumps IgG",
        "NuSwab Vaginitis Plus (VG+)", "NuSwab VG+,HSV", "Phosphorus",
        "Phosphorus inorganic", "Potassium", "Prealbumin", "proBNP II",
        "Procalcitonin", "Progesterone", "Prolactin", "PSA, Free",
        "PSA, Total", "PT", "PTH", "Reticulocyte Count", "Rheumatoid Factor",
        "Rubella IgG", "Rubella IgM", "SHBG", "Sodium", "Syphilis", "T-Uptake",
        "Testosterone", "Thrombin Time", "TOTAL AND FREE PSA", "Total Protein",
        "Total Protein, Random Urine", "Total T3", "Total T4", "Transferrin",
        "Triglycerides", "TSH", "UIBC", "Uric Acid", "URINE MICRO ALBUMIN",
        "Valproic Acid", "Vancomycin", "Vancomycin, Peak", "Vancomycin, Through",
        "VCA IgG", "Vitamin B12", "Vitamin D", "VZV IgG", "WBC"
    ));

    // --- Special: store All Tests count before selection ---
    private static int allTestsCountBeforeSelection = 0;

    public static void setAllTestsCountBeforeSelection(int count) {
        allTestsCountBeforeSelection = count;
    }

    public static int getAllTestsCountBeforeSelection() {
        return allTestsCountBeforeSelection;
    }

    // --- Panel/Test helpers ---
    public static void addTestToPanel(String panelName, String testName) {
        panelTests.computeIfAbsent(panelName, k -> new HashSet<>()).add(testName);
    }

    public static Set<String> getTestsForPanel(String panelName) {
        if ("All Tests".equalsIgnoreCase(panelName)) {
            return allTests; // special handling
        }
        return panelTests.getOrDefault(panelName, Collections.emptySet());
    }

    public static List<String> getTestsForPanelAsList(String panelName) {
        return new ArrayList<>(getTestsForPanel(panelName));
    }

    public static boolean panelContainsTest(String panelName, String testName) {
        return getTestsForPanel(panelName).contains(testName);
    }

    public static Set<String> getAllPanels() {
        return panelTests.keySet();
    }

    public static void clearPanels() {
        panelTests.clear();
    }

    // --- Selected Panels ---
    public static void setSelectedPanels(List<String> panels) {
        selectedPanels.clear();
        selectedPanels.addAll(panels);
    }

    public static List<String> getSelectedPanels() {
        return new ArrayList<>(selectedPanels);
    }

    // --- Selected Tests ---
    public static void setSelectedTests(List<String> tests) {
        selectedTests.clear();
        selectedTests.addAll(tests);
    }

    public static List<String> getSelectedTests() {
        return new ArrayList<>(selectedTests);
    }

    public static void clearSelectedTests() {
        selectedTests.clear();
    }

    // --- Key-Value storage ---
    public static void set(String key, String value) {
        testData.put(key, value);
    }

    public static String get(String key) {
        return testData.get(key);
    }

    public static void clearTestData() {
        testData.clear();
    }
    // --- Test Tube Validation Helper ---
    public static String getExpectedTestTube(String testName) {
        return testTubeMap.get(testName);
    }

    // --- Default Panels Initialization ---
    static {
        panelTests.put("Basic Metabolic Panel", new HashSet<>(Arrays.asList(
            "Anion Gap", "BUN", "BUN/Crea Ratio", "CO2", "Chloride",
            "Creatinine", "Glucose", "Potassium", "Sodium", "eGFR"
        )));

        panelTests.put("Comp Metabolic Panel", new HashSet<>(Arrays.asList(
            "ALT", "AST", "Albumin", "Alk Phos", "Anion Gap", "BUN",
            "BUN/Crea Ratio", "CO2", "Calcium", "Chloride", "Creatinine",
            "Globulin", "Glucose", "Potassium", "Sodium", "Total Protein",
            "eGFR", "Bilirubin, Total"
        )));

        panelTests.put("Hepatic Profile", new HashSet<>(Arrays.asList(
            "ALT", "AST", "Albumin", "Alk Phos", "Total Protein",
            "Bilirubin, Total", "Bilirubin, Direct"
        )));

        panelTests.put("Hepatitis", new HashSet<>(Arrays.asList(
            "Anti-HAV IgM", "Anti-HCV", "Anti-Hbc IgM", "HBsAg"
        )));

        panelTests.put("Lipid Panel", new HashSet<>(Arrays.asList(
            "HDL", "LDL", "Total Cholesterol", "Triglycerides"
        )));

        panelTests.put("Renal Panel", new HashSet<>(Arrays.asList(
            "DHEA-S", "Estradiol", "Folate", "Free T3", "GGT", "INSULIN",
            "Iron Profile", "LDH", "Magnesium", "Progesterone", "SHBG",
            "Uric Acid", "Vitamin B12", "Vitamin D", "HgbA1C", "CBC w/Automated Diff"
        )));

        panelTests.put("Food Allergens-30", new HashSet<>(Arrays.asList(
            "Banana", "Carrot", "Cheese Cheddar", "Chicken", "Corn(Food)", "Egg White",
            "Fish(Cod)", "Garlic", "Milk", "Onion", "Orange", "Pea", "Peanut",
            "Pork", "Potato", "Rice", "Soybean", "Strawberry", "Wheat(Food)",
            "Yeast Bakers", "Tuna", "Salmon", "Shrimp", "Crab", "Clam",
            "Walnut(Food)", "Hazelnut(Food)", "Cashew Nut", "Almond", "Sesame Seed"
        )));

        panelTests.put("Inhalenat Allergens-36", new HashSet<>(Arrays.asList(
            "Acacia", "Alder Black", "American Sycamore", "Ash", "Aspergillus",
            "Birch White", "Box Elder Maple", "Cat Dander-Epithelium", "Cladosporium",
            "Cocklebur", "Cockroach Mix", "Cottonwood", "Dog Dander", "Elm",
            "English Plantain", "Lambs Quarters", "Mesquite", "Mite Farinae",
            "Mite Pteronyssinus", "Mountain Cedar", "Mugwort", "Mulberry", "Nettle",
            "Olive(Pollen)", "Penicillum", "Pigweed", "Ragweed Mix l", "Rough Marshelder",
            "Russian Thistle", "Sheep Sorrel", "Timothy Grass", "Walnut(Tree)",
            "Alternaria", "Oak", "Bahia Grass", "Bermuda Grass"
        )));

        // virtual panel for All Tests
        panelTests.put("All Tests", allTests);
        
        // Initialize the test tube map with "SST" as the value
        testTubeMap.put("Albumin", "SST");
        testTubeMap.put("ALT", "SST");
        testTubeMap.put("Alk Phos", "SST");
        testTubeMap.put("Amylase", "SST");
        testTubeMap.put("ALP", "SST");
        testTubeMap.put("Anti-Hbc", "SST");
        testTubeMap.put("Anti-HCV", "SST");
        testTubeMap.put("Anti-HAV IgM", "SST");
        testTubeMap.put("Anti-HBs", "SST");
        testTubeMap.put("Anti-HAV", "SST");
        testTubeMap.put("Anti-Hbc IgM", "SST");
        testTubeMap.put("Anti-Tg", "SST");
        testTubeMap.put("Anti-TPO", "SST");
        testTubeMap.put("APTT", "LIGHT BLUE PLASMA");
        testTubeMap.put("Bilirubin, Total", "SST");
        testTubeMap.put("Bilirubin, Direct", "SST");
        testTubeMap.put("AST", "SST");
        testTubeMap.put("BUN", "SST");
        testTubeMap.put("C-Peptide", "SST");
        testTubeMap.put("C3", "RED TOP Serum");
        testTubeMap.put("CA 125", "RED TOP Serum");
        testTubeMap.put("C4", "RED TOP Serum");
        testTubeMap.put("CA 15-3", "RED TOP Serum");
        testTubeMap.put("CBC w/Automated Diff", "LAVENDER TOP");
        testTubeMap.put("Calcium", "SST");
        testTubeMap.put("CA 19-9", "RED TOP Serum");
        testTubeMap.put("Chloride", "SST");
        testTubeMap.put("Cholesterol, Total", "SST");
        testTubeMap.put("CK", "SST");
        testTubeMap.put("CORTISOL PM", "SST");
        testTubeMap.put("Cortisol AM", "SST");
        testTubeMap.put("CO2", "SST");
        testTubeMap.put("Creatinine", "SST");
        testTubeMap.put("DHEA-S", "SST");
        testTubeMap.put("Digoxin", "SST");
        testTubeMap.put("ELECTROLYTE PANEL", "SST");
        testTubeMap.put("FECAL OCCULT BLOOD", "STOOL CARD");
        testTubeMap.put("ESR", "LAVENDER TOP");
        testTubeMap.put("Ferritin", "SST");
        testTubeMap.put("Estradiol", "SST");
        testTubeMap.put("Folate", "SST");
        testTubeMap.put("FSH", "SST");
        testTubeMap.put("H pylori Breath Test", "BREATH BAG");
        testTubeMap.put("HCG+B", "SST");
        testTubeMap.put("Hemoglobin", "LAVENDER TOP");
        testTubeMap.put("HIV Screening", "LAVENDER TOP");
        testTubeMap.put("Glucose", "SST");
        testTubeMap.put("Free T4", "SST");
        testTubeMap.put("HBsAg", "SST");
        testTubeMap.put("HgbA1C", "LAVENDER TOP");
        testTubeMap.put("Hematocrit", "LAVENDER TOP");
        testTubeMap.put("Free T3", "SST");
        testTubeMap.put("GGT", "SST");
        testTubeMap.put("HDL", "SST");
        testTubeMap.put("Hepatitis", "SST");
        testTubeMap.put("hsCRP", "SST");
        testTubeMap.put("INSULIN", "SST");
        testTubeMap.put("LDH", "SST");
        testTubeMap.put("Lipase", "SST");
        testTubeMap.put("HSV 1 IgG", "SST");
        testTubeMap.put("Iron Profile", "SST");
        testTubeMap.put("LDL", "SST");
        testTubeMap.put("Lithium", "SST");
        testTubeMap.put("HSV 2 IgG", "SST");
        testTubeMap.put("Lactate", "SST");
        testTubeMap.put("LH", "SST");
        testTubeMap.put("Magnesium", "SST");
        testTubeMap.put("Prealbumin", "SST");
        testTubeMap.put("Progesterone", "SST");
        testTubeMap.put("PSA, Total", "SST");
        testTubeMap.put("Mononucleosis, Qualitative", "SST");
        testTubeMap.put("Potassium", "SST");
        testTubeMap.put("Procalcitonin", "SST");
        testTubeMap.put("PSA, Free", "SST");
        testTubeMap.put("Phosphorus", "SST");
        testTubeMap.put("proBNP II", "SST");
        testTubeMap.put("Prolactin", "SST");
        testTubeMap.put("PT", "LIGHT BLUE PLASMA");
        testTubeMap.put("Reticulocyte Count", "LAVENDER TOP");
        testTubeMap.put("PTH", "SST");
        testTubeMap.put("Rheumatoid Factor", "SST");
        testTubeMap.put("SHBG", "SST");
        testTubeMap.put("Sodium", "SST");
        testTubeMap.put("Testosterone", "SST");
        testTubeMap.put("Total Protein, Random Urine", "URINE");
        testTubeMap.put("Transferrin", "SST");
        testTubeMap.put("UIBC", "SST");
        testTubeMap.put("Valproic Acid", "SST");
        testTubeMap.put("T-Uptake", "SST");
        testTubeMap.put("Total Protein", "SST");
        testTubeMap.put("Total T4", "SST");
        testTubeMap.put("TSH", "SST");
        testTubeMap.put("URINE MICRO ALBUMIN", "URINE");
        testTubeMap.put("Vancomycin, Through", "SST");
        testTubeMap.put("Syphilis", "SST");
        testTubeMap.put("TOTAL AND FREE PSA", "SST");
        testTubeMap.put("Total T3", "SST");
        testTubeMap.put("Triglycerides", "SST");
        testTubeMap.put("Uric Acid", "SST");
        testTubeMap.put("Vancomycin, Peak", "SST");
        testTubeMap.put("Vancomycin", "SST");
        testTubeMap.put("Vitamin B12", "SST");
        testTubeMap.put("WBC", "LAVENDER TOP");
        testTubeMap.put("Vitamin D", "SST");
    }
}
