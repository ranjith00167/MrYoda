package com.mryoda.diagnostics.api.report;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

public class ReportSnapshotTest {

    private static final String VISIT = "VIS-TEST-1";

    @Test(description = "Parses visit, entries, header flags and key epochs from the API contract")
    public void parsesContract() {
        ReportSnapshot s = ReportSnapshot.parse(PdfFixtures.report(VISIT,
                PdfFixtures.entry("CLOTTING TIME", "HEMAT019", "HAEMATOLOGY", "PDF/V/V_HEMAT019_1792045330_Header.pdf", true),
                PdfFixtures.entry("HEMOGLOBIN", "HEMAT001", "HAEMATOLOGY", "PDF/V/V_HEMAT001_1792045999_Result.pdf", false)));

        Assert.assertTrue(s.isSuccess());
        Assert.assertEquals(s.getVisitNumber(), VISIT);
        Assert.assertEquals(s.getEntries().size(), 2);
        Assert.assertEquals(s.nonHeaderCount(), 1);
        Assert.assertEquals(s.version(), 1792045999L);
    }

    @Test(description = "Completion is per test: TRUE non-header, FALSE header-only, null unlisted")
    public void completionPerTest() {
        ReportSnapshot s = ReportSnapshot.parse(PdfFixtures.report(VISIT,
                PdfFixtures.entry("CLOTTING TIME", "HEMAT019", "HAEMATOLOGY", "k1", true),
                PdfFixtures.entry("HEMOGLOBIN", "HEMAT001", "HAEMATOLOGY", "k2", false)));

        Assert.assertEquals(s.completionFor(ExpectedTest.builder("clotting  time").build()), Boolean.FALSE);
        Assert.assertEquals(s.completionFor(ExpectedTest.builder("anything").code("hemat001").build()), Boolean.TRUE);
        Assert.assertNull(s.completionFor(ExpectedTest.builder("UNLISTED").build()));
    }

    @Test(description = "A missing isHeader flag is never treated as a completed result")
    public void missingHeaderFlagMeansNotCompleted() {
        JSONObject e = PdfFixtures.entry("CLOTTING TIME", "HEMAT019", "HAEMATOLOGY", "k1", false);
        e.remove("isHeader");

        ReportSnapshot s = ReportSnapshot.parse(PdfFixtures.report(VISIT, e));

        Assert.assertEquals(s.nonHeaderCount(), 0);
    }

    @Test(description = "Expected tests are derived from the response; packages become name-only groups")
    public void derivesExpectedTests() {
        ReportSnapshot s = ReportSnapshot.parse(PdfFixtures.report(VISIT,
                PdfFixtures.entry("BONE PROFILE -1", "BIO034", "LAB PACKAGE", "k1", true),
                PdfFixtures.entry("BONE PROFILE -1", "BIO254", "LAB PACKAGE", "k2", true),
                PdfFixtures.entry("CALCIUM", "BIO001", "BIOCHEMISTRY", "k3", false)));

        List<ExpectedTest> tests = s.toExpectedTests();

        Assert.assertEquals(tests.size(), 2, "duplicate names collapse");
        Assert.assertFalse(tests.get(0).isValueRequired());
        Assert.assertTrue(tests.get(1).isValueRequired());
    }

    @Test(description = "Error bodies and garbage never throw")
    public void tolerantParsing() {
        Assert.assertFalse(ReportSnapshot.parse(null).isSuccess());
        Assert.assertFalse(ReportSnapshot.parse("<html>502 Bad Gateway</html>").isSuccess());

        ReportSnapshot notFound = ReportSnapshot.parse(
                "{\"status\":404,\"success\":false,\"msg\":\"No report found for the given visit number\"}");
        Assert.assertFalse(notFound.isSuccess());
        Assert.assertTrue(notFound.getEntries().isEmpty());
        Assert.assertTrue(notFound.getMessage().contains("No report found"));
    }
}
