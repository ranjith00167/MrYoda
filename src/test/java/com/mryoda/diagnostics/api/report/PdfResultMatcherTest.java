package com.mryoda.diagnostics.api.report;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class PdfResultMatcherTest {

    private final PdfResultMatcher matcher = new PdfResultMatcher();

    private static ExpectedTest test(String name) {
        return ExpectedTest.builder(name).build();
    }

    private TestResult single(String pdfText, ExpectedTest expected) {
        return matcher.evaluate(pdfText, Collections.singletonList(expected), t -> null).getResults().get(0);
    }

    @Test(description = "Value, unit read next to the name; reference range is ignored")
    public void readsValueAndUnit() {
        TestResult r = single("CLOTTING TIME 105 Min 5 - 10 Min Capillary", test("CLOTTING TIME"));

        Assert.assertEquals(r.getState(), ResultState.SYNCED, r.toString());
        Assert.assertEquals(r.getValue(), "105");
        Assert.assertEquals(r.getUnit(), "Min");
    }

    @Test(description = "Name present without a value is NOT a synced result")
    public void nameOnlyIsPending() {
        TestResult r = single("Investigation Result Unit\nCLOTTING TIME\nPage 1 of 1", test("CLOTTING TIME"));

        Assert.assertEquals(r.getState(), ResultState.NAME_ONLY, r.toString());
        Assert.assertNull(r.getValue());
        Assert.assertTrue(r.getReason().contains("no result value"), r.getReason());
    }

    @Test(description = "A reference range after the name must never be read as the result")
    public void referenceRangeIsNotAResult() {
        TestResult r = single("CLOTTING TIME 5 - 10 Min", test("CLOTTING TIME"));

        Assert.assertEquals(r.getState(), ResultState.NAME_ONLY, r.toString());
    }

    @Test(description = "Test absent from the PDF")
    public void missingTest() {
        TestResult r = single("HEMOGLOBIN 13.5 g/dL", test("CLOTTING TIME"));

        Assert.assertEquals(r.getState(), ResultState.NOT_FOUND, r.toString());
    }

    @Test(description = "A neighbour's value is never borrowed")
    public void neighbourValueNotBorrowed() {
        String pdf = "HEMOGLOBIN\nWBC COUNT 7.5 cells\n";
        List<ExpectedTest> expected = Arrays.asList(test("HEMOGLOBIN"), test("WBC COUNT"));

        ReportEvaluation ev = matcher.evaluate(pdf, expected, t -> null);

        Assert.assertEquals(ev.getResults().get(0).getState(), ResultState.NAME_ONLY);
        Assert.assertEquals(ev.getResults().get(1).getState(), ResultState.SYNCED);
        Assert.assertEquals(ev.getResults().get(1).getValue(), "7.5");
        Assert.assertEquals(ev.completedCount(), 1);
        Assert.assertEquals(ev.pendingCount(), 1);
    }

    /** Same structure as a real staging report PDF; patient and visit data are fabricated. */
    private static final String REAL_LAYOUT_CBC = String.join("\n",
            "Visit ID : VIS-TEST-1 UHID/MR No : UH.0000000001",
            "Patient Name : Mr.TEST PATIENT Client Code : 0001",
            "DEPARTMENT OF HAEMATOLOGY",
            "Test Name Result Unit Biological Ref. Range Method",
            "*CBC(COMPLETE BLOOD COUNT)",
            "Sample Type : WHOLE BLOOD BOTTLE",
            "HAEMOGLOBIN (HB) 15.0 g/dl 13.0 - 17.0 Cyanide-free SLS",
            "method",
            "RBC COUNT(RED BLOOD CELL COUNT) 5.00 million/cmm 4.50 - 5.50 Impedance",
            "RDW - CV 13 % 11.0-16.0 Automated Calculated",
            "TOTAL LEUCOCYTE COUNT 7,000 cells/ml 4000 - 11000 Flow Cytometry",
            "DLC (by Flow cytometry/Microscopy)",
            "NEUTROPHIL 60 % 40 - 80 Impedance",
            " ",
            "*** End Of Report ***",
            "Verified By : Approved By :",
            "Page 1 of 1");

    private static final String REAL_LAYOUT_CLOTTING = String.join("\n",
            "Visit ID : VIS-TEST-1 UHID/MR No : UH.0000000001",
            "DEPARTMENT OF HAEMATOLOGY",
            "Test Name Result Unit Biological Ref. Range Method",
            "*CLOTTING TIME",
            "Sample Type : CSF",
            "CLOTTING TIME 5 min 3 - 8 Capillary Tube",
            "Method",
            "COMMENTS:",
            "Clotting time is measured to assess blood coagulation status.",
            "*** End Of Report ***");

    @Test(description = "Real layout: '*TEST' header line followed by component result rows is a synced result")
    public void sectionHeaderWithComponentRowsIsResult() {
        TestResult r = single(REAL_LAYOUT_CBC, test("CBC(COMPLETE BLOOD COUNT)"));

        Assert.assertEquals(r.getState(), ResultState.SYNCED, r.toString());
        Assert.assertEquals(r.getValue(), "15.0");
        Assert.assertEquals(r.getUnit(), "g/dl");
        Assert.assertTrue(r.getReason().contains("5 result row(s)"), r.getReason());
    }

    @Test(description = "Real layout: a test whose header repeats on its result row")
    public void headerThenResultRowWithSameName() {
        TestResult r = single(REAL_LAYOUT_CLOTTING, test("CLOTTING TIME"));

        Assert.assertEquals(r.getState(), ResultState.SYNCED, r.toString());
        Assert.assertEquals(r.getValue(), "5");
        Assert.assertEquals(r.getUnit(), "min");
    }

    @Test(description = "Section header with no result rows (comments only) stays pending")
    public void sectionWithoutRowsIsPending() {
        TestResult r = single("*CBC(COMPLETE BLOOD COUNT)\nSample Type : WHOLE BLOOD\nCOMMENTS: awaiting\n"
                + "RBC 5.00 million/cmm 4.50 - 5.50\n*** End Of Report ***", test("CBC(COMPLETE BLOOD COUNT)"));

        Assert.assertEquals(r.getState(), ResultState.NAME_ONLY, r.toString());
    }

    @Test(description = "Next section's rows are never attributed to an empty section")
    public void adjacentSectionRowsNotBorrowed() {
        String pdf = "*TEST ONE\nSample Type : SERUM\n*TEST TWO\nMCV 92 fL 83 - 101 Automated\n*** End Of Report ***";

        ReportEvaluation ev = matcher.evaluate(pdf, Arrays.asList(test("TEST ONE"), test("TEST TWO")), t -> null);

        Assert.assertEquals(ev.getResults().get(0).getState(), ResultState.NAME_ONLY);
        Assert.assertEquals(ev.getResults().get(1).getState(), ResultState.SYNCED);
    }

    @Test(description = "Thousands separators are read as one number; expected value compares numerically")
    public void thousandsSeparator() {
        ExpectedTest tlc = ExpectedTest.builder("TOTAL LEUCOCYTE COUNT").expectedValue("7000").build();
        TestResult r = single("TOTAL LEUCOCYTE COUNT 7,000 cells/ml 4000 - 11000", tlc);

        Assert.assertEquals(r.getState(), ResultState.SYNCED, r.toString());
        Assert.assertEquals(r.getValue(), "7000");
    }

    @Test(description = "Expected value is matched against any row of a section")
    public void sectionExpectedValue() {
        ExpectedTest ok = ExpectedTest.builder("CBC(COMPLETE BLOOD COUNT)").expectedValue("13").build();
        ExpectedTest bad = ExpectedTest.builder("CBC(COMPLETE BLOOD COUNT)").expectedValue("999").build();

        Assert.assertEquals(single(REAL_LAYOUT_CBC, ok).getState(), ResultState.SYNCED);
        Assert.assertEquals(single(REAL_LAYOUT_CBC, bad).getState(), ResultState.MISMATCH);
    }

    @Test(description = "Case, punctuation, spacing and parenthesised alternative names")
    public void toleratesFormattingVariations() {
        ExpectedTest cbc = test("CBC(COMPLETE BLOOD COUNT)");
        TestResult r = single("complete   blood\ncount (cbc)  13.5   g/dL  12-16", cbc);

        Assert.assertEquals(r.getState(), ResultState.SYNCED, r.toString());
        Assert.assertEquals(r.getValue(), "13.5");
    }

    @Test(description = "Value in a wrapped cell on the following line")
    public void wrappedValueOnNextLine() {
        TestResult r = single("CLOTTING TIME\n105 Min 5 - 10 Min", test("CLOTTING TIME"));

        Assert.assertEquals(r.getState(), ResultState.SYNCED, r.toString());
        Assert.assertEquals(r.getValue(), "105");
    }

    @Test(description = "Test code and aliases supplied by the API are used")
    public void matchesByCodeAndAlias() {
        ExpectedTest byCode = ExpectedTest.builder("Prothrombin Time").code("HEMAT019").build();
        ExpectedTest byAlias = ExpectedTest.builder("Glycosylated Haemoglobin").alias("HbA1c").build();

        Assert.assertEquals(single("HEMAT019  12.1 sec", byCode).getValue(), "12.1");
        Assert.assertEquals(single("HbA1c 5.4 %", byAlias).getValue(), "5.4");
        Assert.assertEquals(single("HbA1c 5.4 %", byAlias).getUnit(), "%");
    }

    @Test(description = "Qualitative results are results too")
    public void qualitativeValues() {
        Assert.assertEquals(single("HBsAg Non Reactive", test("HBsAg")).getValue(), "Non Reactive");
        Assert.assertEquals(single("Urine Culture Negative", test("Urine Culture")).getValue(), "Negative");
    }

    @Test(description = "Identifiers and dates next to a name are not values")
    public void identifiersAndDatesAreIgnored() {
        Assert.assertEquals(single("CLOTTING TIME Sample ID 4567", test("CLOTTING TIME")).getState(),
                ResultState.NAME_ONLY);
        Assert.assertEquals(single("CLOTTING TIME 15-10-2026 11:47", test("CLOTTING TIME")).getState(),
                ResultState.NAME_ONLY);
        Assert.assertEquals(single("CLOTTING TIME 9876543210", test("CLOTTING TIME")).getState(),
                ResultState.NAME_ONLY);
    }

    @Test(description = "Expected business value is verified; mismatch is a failure, not a pass")
    public void expectedValueVerification() {
        ExpectedTest ok = ExpectedTest.builder("CLOTTING TIME").expectedValue("105").build();
        ExpectedTest bad = ExpectedTest.builder("CLOTTING TIME").expectedValue("110").build();

        Assert.assertEquals(single("CLOTTING TIME 105 Min", ok).getState(), ResultState.SYNCED);
        TestResult mismatch = single("CLOTTING TIME 105 Min", bad);
        Assert.assertEquals(mismatch.getState(), ResultState.MISMATCH);
        Assert.assertTrue(mismatch.getReason().contains("110"), mismatch.getReason());
    }

    @Test(description = "Value in PDF but API still header-only => PDF_ONLY, not SYNCED")
    public void apiHeaderOnlyIsPdfOnly() {
        Function<ExpectedTest, Boolean> headerOnly = t -> Boolean.FALSE;
        Function<ExpectedTest, Boolean> completed = t -> Boolean.TRUE;
        List<ExpectedTest> one = Collections.singletonList(test("CLOTTING TIME"));

        Assert.assertEquals(matcher.evaluate("CLOTTING TIME 105 Min", one, headerOnly).getResults().get(0).getState(),
                ResultState.PDF_ONLY);
        Assert.assertEquals(matcher.evaluate("CLOTTING TIME 105 Min", one, completed).getResults().get(0).getState(),
                ResultState.SYNCED);
    }

    @Test(description = "Package/panel headers only need to be present by name")
    public void groupHeaderNeedsNoValue() {
        ExpectedTest panel = ExpectedTest.builder("BONE PROFILE -1").valueRequired(false).build();

        Assert.assertEquals(single("BONE PROFILE -1\nCalcium 9.1 mg/dL", panel).getState(), ResultState.GROUP_PRESENT);
        Assert.assertEquals(single("something else", panel).getState(), ResultState.NOT_FOUND);
    }

    @Test(description = "Empty text yields an explicit reason for every test")
    public void emptyTextExplainsWhy() {
        ReportEvaluation ev = matcher.evaluate("", Arrays.asList(test("A TEST"), test("B TEST")), t -> null);

        Assert.assertEquals(ev.completedCount(), 0);
        for (TestResult r : ev.getResults()) {
            Assert.assertEquals(r.getState(), ResultState.NOT_FOUND);
            Assert.assertFalse(r.getReason().isEmpty());
        }
    }

    @Test(description = "End to end: real PDF table through the extractor and matcher")
    public void worksOnGeneratedPdf() {
        PdfExtraction ex = new PdfTextExtractor(20).extract(PdfFixtures.pdf(
                "Investigation|Result|Unit|Reference",
                "CLOTTING TIME|105|Min|5 - 10 Min",
                "HEMOGLOBIN|13.5|g/dL|12 - 16",
                "CALCIUM| |mg/dL|8.5 - 10.5"));
        Assert.assertTrue(ex.isOk(), ex.getDetail());

        ReportEvaluation ev = matcher.evaluate(ex.getText(),
                Arrays.asList(test("CLOTTING TIME"), test("HEMOGLOBIN"), test("CALCIUM")), t -> null);

        Assert.assertEquals(ev.getResults().get(0).getValue(), "105");
        Assert.assertEquals(ev.getResults().get(1).getValue(), "13.5");
        // CALCIUM has no result - its reference range must not be mistaken for one.
        Assert.assertEquals(ev.getResults().get(2).getState(), ResultState.NAME_ONLY);
        Assert.assertEquals(ev.completedCount(), 2);
        Assert.assertEquals(ev.pendingCount(), 1);
    }
}
