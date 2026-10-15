package com.mryoda.diagnostics.api.report;

/** Outcome of turning PDF bytes into text. Never throws; failure is a status. */
public final class PdfExtraction {

    public enum Status { OK, NO_BYTES, NOT_A_PDF, ENCRYPTED, MALFORMED, EMPTY_TEXT }

    private final Status status;
    private final String text;
    private final String detail;
    private final int pageCount;

    private PdfExtraction(Status status, String text, String detail, int pageCount) {
        this.status = status;
        this.text = text;
        this.detail = detail;
        this.pageCount = pageCount;
    }

    public static PdfExtraction ok(String text, int pageCount) {
        return new PdfExtraction(Status.OK, text,
                "extracted " + text.length() + " chars, " + pageCount + " page(s)", pageCount);
    }

    public static PdfExtraction failed(Status status, String detail) {
        return new PdfExtraction(status, "", detail, 0);
    }

    public boolean isOk() {
        return status == Status.OK;
    }

    public Status getStatus() {
        return status;
    }

    /** Extracted text; empty string (never null) unless {@link #isOk()}. */
    public String getText() {
        return text;
    }

    public String getDetail() {
        return detail;
    }

    public int getPageCount() {
        return pageCount;
    }
}
