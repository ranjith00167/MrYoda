package com.mryoda.diagnostics.api.report;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;

public class PdfTextExtractorTest {

    private final PdfTextExtractor extractor = new PdfTextExtractor(20);

    @Test(description = "Table row stays on one text line so name and value are adjacent")
    public void extractsTableRowsOnOneLine() {
        PdfExtraction ex = extractor.extract(PdfFixtures.pdf(
                "Investigation|Result|Unit|Reference",
                "CLOTTING TIME|105|Min|5 - 10 Min",
                "HEMOGLOBIN|13.5|g/dL|12 - 16"));

        Assert.assertTrue(ex.isOk(), ex.getDetail());
        Assert.assertTrue(ex.getText().replaceAll("[ \\t]+", " ").contains("CLOTTING TIME 105 Min 5 - 10 Min"),
                "row split across lines: " + ex.getText());
        Assert.assertEquals(ex.getPageCount(), 1);
    }

    @Test(description = "null / empty download is reported, not thrown")
    public void reportsMissingBytes() {
        Assert.assertEquals(extractor.extract(null).getStatus(), PdfExtraction.Status.NO_BYTES);
        Assert.assertEquals(extractor.extract(new byte[0]).getStatus(), PdfExtraction.Status.NO_BYTES);
    }

    @Test(description = "An S3 XML/HTML error body is not mistaken for a PDF")
    public void rejectsNonPdfContent() {
        byte[] body = "<?xml version=\"1.0\"?><Error><Code>AccessDenied</Code></Error>"
                .getBytes(StandardCharsets.UTF_8);
        PdfExtraction ex = extractor.extract(body);

        Assert.assertEquals(ex.getStatus(), PdfExtraction.Status.NOT_A_PDF);
        Assert.assertTrue(ex.getDetail().contains("AccessDenied"), ex.getDetail());
        Assert.assertEquals(ex.getText(), "");
    }

    @Test(description = "A file with a PDF header but corrupt body becomes MALFORMED")
    public void rejectsMalformedPdf() {
        byte[] corrupt = "%PDF-1.4\n1 0 obj << /Broken >>\n this is not a real pdf body"
                .getBytes(StandardCharsets.US_ASCII);

        PdfExtraction ex = extractor.extract(corrupt);

        Assert.assertEquals(ex.getStatus(), PdfExtraction.Status.MALFORMED, ex.getDetail());
    }

    @Test(description = "A truncated download never yields usable text")
    public void rejectsTruncatedPdf() {
        byte[] good = PdfFixtures.pdf("CLOTTING TIME|105|Min");
        byte[] truncated = java.util.Arrays.copyOf(good, good.length / 3);

        PdfExtraction ex = extractor.extract(truncated);

        Assert.assertFalse(ex.isOk(), "truncated PDF must not be treated as readable");
        Assert.assertEquals(ex.getText(), "");
        Assert.assertFalse(ex.getDetail().isEmpty());
    }

    @Test(description = "Valid PDF with no text (image-only / blank) is EMPTY_TEXT")
    public void rejectsPdfWithoutText() {
        PdfExtraction ex = extractor.extract(PdfFixtures.blankPdf());

        Assert.assertEquals(ex.getStatus(), PdfExtraction.Status.EMPTY_TEXT, ex.getDetail());
    }

    @Test(description = "Text shorter than the configured minimum is treated as incomplete")
    public void rejectsTextBelowMinimum() {
        PdfTextExtractor strict = new PdfTextExtractor(500);

        Assert.assertEquals(strict.extract(PdfFixtures.pdf("CLOTTING TIME|105|Min")).getStatus(),
                PdfExtraction.Status.EMPTY_TEXT);
    }
}
