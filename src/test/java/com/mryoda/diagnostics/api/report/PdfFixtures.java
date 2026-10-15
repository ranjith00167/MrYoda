package com.mryoda.diagnostics.api.report;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** In-memory PDFs and API payloads for report tests - no network, no real visit. */
final class PdfFixtures {

    private static final float LEFT = 40f;
    private static final float TOP = 780f;
    private static final float LINE_HEIGHT = 16f;
    private static final float COLUMN_WIDTH = 130f;

    private PdfFixtures() {
    }

    /**
     * Builds a one-page PDF. Each argument is a table row; cells are separated by '|' and placed in
     * separate columns, like a lab report table.
     */
    static byte[] pdf(String... rows) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = TOP;
                for (String row : rows) {
                    String[] cells = row.split("\\|", -1);
                    for (int c = 0; c < cells.length; c++) {
                        if (cells[c].trim().isEmpty()) {
                            continue;
                        }
                        content.beginText();
                        content.setFont(PDType1Font.HELVETICA, 10);
                        content.newLineAtOffset(LEFT + c * COLUMN_WIDTH, y);
                        content.showText(cells[c].trim());
                        content.endText();
                    }
                    y -= LINE_HEIGHT;
                }
            }
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A page with no text at all (e.g. scanned image or blank render). */
    static byte[] blankPdf() {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.addPage(new PDPage(PDRectangle.A4));
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** One {@code data.tests} row. */
    static JSONObject entry(String name, String code, String department, String key, boolean header) {
        return new JSONObject()
                .put("testName", name)
                .put("testCode", code)
                .put("department_name", department)
                .put("key", key)
                .put("url", "https://reports.example.test/" + key + "?X-Amz-Signature=SECRETSIG&X-Amz-Date=20260101")
                .put("isHeader", header);
    }

    /** Full 200 body for the report endpoint. */
    static String report(String visit, JSONObject... entries) {
        JSONArray tests = new JSONArray();
        for (JSONObject e : entries) {
            tests.put(e);
        }
        return new JSONObject()
                .put("status", 200).put("success", true).put("msg", "Report fetched successfully")
                .put("data", new JSONObject()
                        .put("userName", "Mr.TEST PATIENT").put("mobile", "9876543210")
                        .put("visitNumber", visit).put("order_date", "2026-10-15 11:47 AM")
                        .put("tests", tests).put("full_download_url", ""))
                .toString();
    }
}
