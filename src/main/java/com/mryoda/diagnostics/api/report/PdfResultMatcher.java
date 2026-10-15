package com.mryoda.diagnostics.api.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides, for every expected test, whether the PDF carries an actual <em>result</em>
 * (not just the test name), using only evidence in the extracted text.
 *
 * <p>Rules (deliberately conservative - a test is never reported as having a value
 * unless one is read next to its name/code/alias):
 * <ul>
 *   <li>Names are matched on token boundaries, tolerant of case, punctuation, repeated
 *       whitespace and line wraps. "All words appear somewhere" is NOT a match.</li>
 *   <li>The value is looked for on the same line after the name; following lines are only
 *       considered when they <em>start</em> with a value (wrapped table cell), so component
 *       rows of a panel are never attributed to the panel.</li>
 *   <li>Reference ranges ("5 - 10"), dates, identifiers and "No./ID" numbers are skipped.</li>
 *   <li>The search window is cut at the next expected test, so a neighbour's value is never
 *       borrowed.</li>
 * </ul>
 */
public class PdfResultMatcher {

    private static final int MAX_WORDS_BEFORE_VALUE = 8;
    private static final int MAX_CONTINUATION_LINES = 2;
    private static final int MIN_ALNUM_FOR_SUBTERM = 3;

    private static final Pattern NUMERIC = Pattern.compile(
            "^[<>]?=?(?:\\d{1,3}(?:,\\d{3})+(?:\\.\\d+)?|\\d+(?:[.,]\\d+)?)$");
    private static final Pattern THOUSANDS = Pattern.compile("^[<>]?=?\\d{1,3}(?:,\\d{3})+(?:\\.\\d+)?$");
    private static final Pattern RANGE_IN_LINE = Pattern.compile("\\d\\s*-\\s*\\d");
    /** Lines that end a result section: next "*SECTION" header, report footer, comment blocks. */
    private static final Pattern SECTION_END = Pattern.compile(
            "^(?:\\*+|verified by|approved by|page\\s+\\d+|end of report|comments?\\b|note\\b|"
                    + "interpretation\\b|remarks?\\b).*", Pattern.CASE_INSENSITIVE);
    private static final int MAX_SECTION_ROWS = 200;
    private static final Pattern RANGE_INLINE = Pattern.compile("^\\d+(?:[.,]\\d+)?[-–]\\d+(?:[.,]\\d+)?$");
    private static final Pattern DATE_LIKE = Pattern.compile("^\\d{1,4}[/.-]\\d{1,2}[/.-]\\d{1,4}$");
    private static final Pattern TIME_LIKE = Pattern.compile("^\\d{1,2}:\\d{2}(?::\\d{2})?$");
    private static final Pattern UNIT = Pattern.compile("^[A-Za-z%µμ/][A-Za-z0-9%µμ/^.*²³-]{0,14}$");
    private static final Pattern FLAG = Pattern.compile("^[HLNhln*]$|^(?:high|low|normal)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d+$");
    private static final Pattern REFERENCE_LINE = Pattern.compile(
            "^(?:reference|ref\\.?|biological|bio\\.|method|note|comment|interpretation|page|remarks?)\\b.*",
            Pattern.CASE_INSENSITIVE);

    /** Words that make a directly following number an identifier, not a result. */
    private static final Set<String> IDENTIFIER_PREFIXES = new HashSet<>(Arrays.asList(
            "id", "no", "no.", "number", "date", "age", "ref", "visit", "sin", "barcode", "mobile",
            "phone", "page", "of", "sample", "reg", "uhid", "pid", "lab", "bill", "order"));

    private static final Set<String> QUALITATIVE = new HashSet<>(Arrays.asList(
            "positive", "negative", "reactive", "nonreactive", "non-reactive", "detected", "undetected",
            "absent", "present", "nil", "trace", "normal", "abnormal", "seen", "reactive.", "negative."));

    /** One value read from the PDF, with the text line it came from. */
    static final class ExtractedValue {
        final String value;
        final String unit;
        final String line;

        ExtractedValue(String value, String unit, String line) {
            this.value = value;
            this.unit = unit;
            this.line = line;
        }
    }

    /**
     * @param pdfText      full extracted PDF text
     * @param expected     tests to verify (each verified independently)
     * @param apiCompleted per-test API completion flag: TRUE = non-header entry, FALSE = header-only,
     *                     null = test not listed by the API
     */
    public ReportEvaluation evaluate(String pdfText, List<ExpectedTest> expected,
                                     Function<ExpectedTest, Boolean> apiCompleted) {
        return evaluate(t -> pdfText, expected, apiCompleted);
    }

    /**
     * Variant for reports delivered as one PDF per test: {@code textFor} supplies the text that
     * is authoritative for each test (null/empty means no PDF could be read for it).
     */
    public ReportEvaluation evaluate(Function<ExpectedTest, String> textFor, List<ExpectedTest> expected,
                                     Function<ExpectedTest, Boolean> apiCompleted) {
        java.util.Map<String, String> normalized = new java.util.HashMap<>();
        List<TestResult> results = new ArrayList<>();
        for (ExpectedTest test : expected) {
            String text = textFor.apply(test);
            if (text == null || text.trim().isEmpty()) {
                results.add(new TestResult(test, ResultState.NOT_FOUND, null, null,
                        "no readable PDF text available for this test"));
                continue;
            }
            String flat = normalized.computeIfAbsent(text, PdfResultMatcher::normalize);
            Boolean api = apiCompleted == null ? null : apiCompleted.apply(test);
            results.add(evaluateOne(flat, test, expected, api));
        }
        return new ReportEvaluation(results);
    }

    private TestResult evaluateOne(String flat, ExpectedTest test, List<ExpectedTest> all, Boolean apiCompleted) {
        List<Matcher> matchers = termMatchers(test, flat);
        boolean nameSeen = false;
        for (Matcher m : matchers) {
            while (m.find()) {
                nameSeen = true;
                if (!test.isValueRequired()) {
                    return new TestResult(test, ResultState.GROUP_PRESENT, null, null,
                            "group/panel header found in PDF; no value required");
                }
                int cut = nextBoundary(flat, m.end(), test, all);
                String window = flat.substring(m.end(), cut);
                ExtractedValue found = readValue(window);
                if (found != null) {
                    return withValue(test, found, apiCompleted);
                }
                // Lab layout: "*TEST NAME" header line followed by component result rows.
                List<ExtractedValue> rows = readSectionRows(window);
                if (!rows.isEmpty()) {
                    return withSection(test, rows, apiCompleted);
                }
            }
        }
        if (nameSeen) {
            return new TestResult(test, ResultState.NAME_ONLY, null, null,
                    "name/code present in PDF but no result value next to it (header or title only)");
        }
        return new TestResult(test, ResultState.NOT_FOUND, null, null,
                "none of " + test.searchTerms() + " found in PDF text");
    }

    private TestResult withSection(ExpectedTest test, List<ExtractedValue> rows, Boolean apiCompleted) {
        ExtractedValue first = rows.get(0);
        String expectedValue = test.getExpectedValue();
        if (expectedValue != null) {
            boolean anyMatch = false;
            for (ExtractedValue row : rows) {
                anyMatch |= valuesEqual(expectedValue, row.value);
            }
            if (!anyMatch) {
                return new TestResult(test, ResultState.MISMATCH, first.value, first.unit,
                        "none of " + rows.size() + " result row(s) equals expected '" + expectedValue + "'");
            }
        }
        String why = rows.size() + " result row(s) under the section header; first: " + first.line.trim();
        if (Boolean.FALSE.equals(apiCompleted)) {
            return new TestResult(test, ResultState.PDF_ONLY, first.value, first.unit,
                    why + "; API entry still header-only");
        }
        return new TestResult(test, ResultState.SYNCED, first.value, first.unit, why);
    }

    /**
     * Result rows of a section whose header line carries only the test name. A row is a line with a
     * value plus a unit or a reference range; the section ends at the next "*HEADER", the footer or a
     * comment block, so another test's rows are never borrowed.
     */
    private List<ExtractedValue> readSectionRows(String window) {
        List<ExtractedValue> rows = new ArrayList<>();
        String[] lines = window.split("\n", -1);
        if (lines.length < 2 || !lines[0].replaceAll("[\\s:()*-]", "").isEmpty()) {
            return rows; // the name is not alone on its header line
        }
        for (int i = 1; i < lines.length && rows.size() < MAX_SECTION_ROWS; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.toLowerCase(Locale.ROOT).startsWith("sample type")) {
                continue;
            }
            if (SECTION_END.matcher(line).matches()) {
                break;
            }
            ExtractedValue v = scanLine(line, true);
            if (v != null && (v.unit != null || RANGE_IN_LINE.matcher(line).find())) {
                rows.add(v);
            }
        }
        return rows;
    }

    private TestResult withValue(ExpectedTest test, ExtractedValue found, Boolean apiCompleted) {
        String expectedValue = test.getExpectedValue();
        if (expectedValue != null && !valuesEqual(expectedValue, found.value)) {
            return new TestResult(test, ResultState.MISMATCH, found.value, found.unit,
                    "PDF value '" + found.value + "' differs from expected '" + expectedValue + "'");
        }
        if (Boolean.FALSE.equals(apiCompleted)) {
            return new TestResult(test, ResultState.PDF_ONLY, found.value, found.unit,
                    "value in PDF; API entry still header-only (metadata not yet synced)");
        }
        return new TestResult(test, ResultState.SYNCED, found.value, found.unit,
                "value in PDF" + (Boolean.TRUE.equals(apiCompleted) ? " and API entry completed"
                        : " (test not listed by API)"));
    }

    // ---------------------------------------------------------------- matching

    private List<Matcher> termMatchers(ExpectedTest test, String flat) {
        List<Matcher> out = new ArrayList<>();
        for (String term : expandTerms(test.searchTerms())) {
            Pattern p = boundaryPattern(term);
            if (p != null) {
                out.add(p.matcher(flat));
            }
        }
        return out;
    }

    /** "CBC(COMPLETE BLOOD COUNT)" also searches "CBC" and "COMPLETE BLOOD COUNT". */
    static Set<String> expandTerms(Set<String> terms) {
        Set<String> expanded = new LinkedHashSet<>(terms);
        for (String term : terms) {
            int open = term.indexOf('(');
            int close = term.indexOf(')', open + 1);
            if (open >= 0 && close > open) {
                addSubTerm(expanded, term.substring(0, open));
                addSubTerm(expanded, term.substring(open + 1, close));
                addSubTerm(expanded, term.substring(close + 1));
            }
        }
        return expanded;
    }

    private static void addSubTerm(Set<String> target, String candidate) {
        String trimmed = candidate == null ? "" : candidate.trim();
        if (trimmed.replaceAll("[^A-Za-z0-9]", "").length() >= MIN_ALNUM_FOR_SUBTERM) {
            target.add(trimmed);
        }
    }

    static Pattern boundaryPattern(String term) {
        String[] tokens = term.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        List<String> parts = new ArrayList<>();
        for (String t : tokens) {
            if (!t.isEmpty()) {
                parts.add(Pattern.quote(t));
            }
        }
        if (parts.isEmpty()) {
            return null;
        }
        return Pattern.compile("(?<![a-z0-9])" + String.join("[^a-z0-9]+", parts) + "(?![a-z0-9])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /** End of the search window: next other expected test (or a repeat of this one) within reach. */
    private int nextBoundary(String flat, int from, ExpectedTest current, List<ExpectedTest> all) {
        int limit = flat.length();
        for (ExpectedTest other : all) {
            for (String term : expandTerms(other.searchTerms())) {
                Pattern p = boundaryPattern(term);
                if (p == null) {
                    continue;
                }
                Matcher m = p.matcher(flat);
                if (m.find(from) && m.start() > from) {
                    limit = Math.min(limit, m.start());
                }
            }
        }
        return limit;
    }

    // ----------------------------------------------------------------- values

    private ExtractedValue readValue(String window) {
        String[] lines = window.split("\n");
        for (int i = 0; i < lines.length && i <= MAX_CONTINUATION_LINES; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            if (i > 0 && REFERENCE_LINE.matcher(line).matches()) {
                return null;
            }
            ExtractedValue v = scanLine(line, i == 0);
            if (v != null) {
                return v;
            }
            // No value on this line: a wrapped cell may still start the next one.
        }
        return null;
    }

    private ExtractedValue scanLine(String line, boolean sameLineAsName) {
        String[] tokens = line.split("\\s+");
        if (!sameLineAsName && !startsWithValue(line)) {
            return null; // only a wrapped cell (value first) may continue onto the next line
        }
        int limit = Math.min(tokens.length, MAX_WORDS_BEFORE_VALUE + 1);
        for (int i = 0; i < limit; i++) {
            String tok = stripTrailingPunctuation(tokens[i]);
            if (isQualitative(tokens, i)) {
                String text = qualitativeText(tokens, i);
                return new ExtractedValue(text, null, line);
            }
            if (isNumericResult(tokens, i, tok)) {
                return new ExtractedValue(canonicalNumber(tok), unitAfter(tokens, i + 1), line);
            }
        }
        return null;
    }

    private boolean startsWithValue(String line) {
        String[] tokens = line.trim().split("\\s+");
        if (tokens.length == 0 || tokens[0].isEmpty()) {
            return false;
        }
        return isQualitative(tokens, 0) || isNumericResult(tokens, 0, stripTrailingPunctuation(tokens[0]));
    }

    private boolean isNumericResult(String[] tokens, int i, String tok) {
        if (!NUMERIC.matcher(tok).matches()) {
            return false;
        }
        String digits = tok.replaceAll("[^0-9]", "");
        if (digits.length() >= 7) {
            return false; // phone / barcode / visit-like identifier
        }
        if (RANGE_INLINE.matcher(tok).matches() || DATE_LIKE.matcher(tok).matches()
                || TIME_LIKE.matcher(tok).matches()) {
            return false;
        }
        if (i > 0 && IDENTIFIER_PREFIXES.contains(stripTrailingPunctuation(tokens[i - 1]).toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (i + 1 < tokens.length && isRangeSeparator(tokens[i + 1])) {
            return false; // "5 - 10" is a reference range, not a result
        }
        return !(i > 0 && isRangeSeparator(tokens[i - 1]));
    }

    private static boolean isRangeSeparator(String token) {
        return token.equals("-") || token.equals("–") || token.equalsIgnoreCase("to");
    }

    private boolean isQualitative(String[] tokens, int i) {
        String t = stripTrailingPunctuation(tokens[i]).toLowerCase(Locale.ROOT);
        if (QUALITATIVE.contains(t)) {
            return true;
        }
        if ((t.equals("non") || t.equals("not")) && i + 1 < tokens.length) {
            String joined = t + stripTrailingPunctuation(tokens[i + 1]).toLowerCase(Locale.ROOT);
            return joined.equals("nonreactive") || joined.equals("notdetected");
        }
        return false;
    }

    private String qualitativeText(String[] tokens, int i) {
        String t = stripTrailingPunctuation(tokens[i]);
        String lower = t.toLowerCase(Locale.ROOT);
        if ((lower.equals("non") || lower.equals("not")) && i + 1 < tokens.length) {
            return t + " " + stripTrailingPunctuation(tokens[i + 1]);
        }
        return t;
    }

    private String unitAfter(String[] tokens, int next) {
        if (next >= tokens.length) {
            return null;
        }
        String candidate = tokens[next];
        if (NUMERIC.matcher(candidate).matches() || FLAG.matcher(candidate).matches()
                || DIGITS_ONLY.matcher(candidate).matches() || isRangeSeparator(candidate)) {
            return null;
        }
        return UNIT.matcher(candidate).matches() ? candidate : null;
    }

    private static String stripTrailingPunctuation(String token) {
        return token.replaceAll("[,;:]+$", "");
    }

    /** "7,000" -> "7000" (thousands separator); "13,5" -> "13.5" (decimal comma). */
    static String canonicalNumber(String token) {
        return THOUSANDS.matcher(token).matches() ? token.replace(",", "") : token.replace(",", ".");
    }

    // ------------------------------------------------------------- comparison

    static boolean valuesEqual(String expected, String actual) {
        String e = canonicalNumber(expected.trim());
        String a = canonicalNumber(actual.trim());
        try {
            double ed = Double.parseDouble(e.replaceAll("^[<>=]+", ""));
            double ad = Double.parseDouble(a.replaceAll("^[<>=]+", ""));
            return Math.abs(ed - ad) < 0.01;
        } catch (NumberFormatException notNumeric) {
            return e.replaceAll("[\\s-]", "").equalsIgnoreCase(a.replaceAll("[\\s-]", ""));
        }
    }

    // ----------------------------------------------------------- normalization

    /** Unifies dashes/spaces/line endings; case is kept so values are reported exactly as printed. */
    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\r\n", "\n").replace('\r', '\n')
                .replace(' ', ' ').replace(' ', ' ').replace(' ', ' ')
                .replace('–', '-').replace('—', '-').replace('−', '-')
                .replaceAll("[ \\t\\f]+", " ");
    }
}
