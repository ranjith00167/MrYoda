package com.mryoda.diagnostics.api.report;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Extracts the full text of a PDF with PDFBox. Rows are position-sorted so that
 * table cells on the same visual line stay on one text line
 * ("TEST  VALUE  UNIT  RANGE"), which is what {@link PdfResultMatcher} expects.
 */
public class PdfTextExtractor {

    private static final byte[] PDF_MAGIC = "%PDF".getBytes(StandardCharsets.US_ASCII);

    private final int minTextChars;

    public PdfTextExtractor(int minTextChars) {
        this.minTextChars = Math.max(1, minTextChars);
    }

    public PdfExtraction extract(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return PdfExtraction.failed(PdfExtraction.Status.NO_BYTES, "download returned no content");
        }
        if (!containsPdfMagic(pdfBytes)) {
            return PdfExtraction.failed(PdfExtraction.Status.NOT_A_PDF,
                    "content is not a PDF (first bytes: " + printablePrefix(pdfBytes) + ")");
        }
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            int length = text == null ? 0 : text.trim().length();
            if (length < minTextChars) {
                return PdfExtraction.failed(PdfExtraction.Status.EMPTY_TEXT,
                        "PDF has " + document.getNumberOfPages() + " page(s) but only " + length
                                + " text chars (scanned image or not yet rendered?)");
            }
            return PdfExtraction.ok(text, document.getNumberOfPages());
        } catch (InvalidPasswordException e) {
            return PdfExtraction.failed(PdfExtraction.Status.ENCRYPTED, "PDF is password protected");
        } catch (IOException | RuntimeException e) {
            return PdfExtraction.failed(PdfExtraction.Status.MALFORMED,
                    "PDF could not be parsed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /** The spec allows the %PDF marker anywhere in the first 1024 bytes. */
    private static boolean containsPdfMagic(byte[] bytes) {
        int last = Math.min(bytes.length - PDF_MAGIC.length, 1024);
        for (int i = 0; i <= last; i++) {
            boolean match = true;
            for (int j = 0; j < PDF_MAGIC.length; j++) {
                if (bytes[i + j] != PDF_MAGIC[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }
        return false;
    }

    private static String printablePrefix(byte[] bytes) {
        String s = new String(bytes, 0, Math.min(bytes.length, 60), StandardCharsets.UTF_8);
        return s.replaceAll("[^\\x20-\\x7E]", "?");
    }
}
