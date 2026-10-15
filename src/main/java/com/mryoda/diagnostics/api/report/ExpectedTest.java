package com.mryoda.diagnostics.api.report;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A test (or package/panel header) that must appear in a lab report.
 * Immutable. Names, codes and aliases are all treated as search terms.
 */
public final class ExpectedTest {

    private final String name;
    private final String code;
    private final String department;
    private final Set<String> aliases;
    private final boolean valueRequired;
    private final String expectedValue;

    private ExpectedTest(Builder b) {
        this.name = b.name;
        this.code = b.code;
        this.department = b.department;
        this.aliases = Collections.unmodifiableSet(new LinkedHashSet<>(b.aliases));
        this.valueRequired = b.valueRequired;
        this.expectedValue = b.expectedValue;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getDepartment() {
        return department;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    /** false for package / panel headers, which only need to be present by name. */
    public boolean isValueRequired() {
        return valueRequired;
    }

    /** Optional business-expected result (numeric or qualitative); null when unknown. */
    public String getExpectedValue() {
        return expectedValue;
    }

    /** Every non-blank term that identifies this test in a PDF: name, code, aliases. */
    public Set<String> searchTerms() {
        Set<String> terms = new LinkedHashSet<>();
        addIfPresent(terms, name);
        addIfPresent(terms, code);
        aliases.forEach(a -> addIfPresent(terms, a));
        return terms;
    }

    private static void addIfPresent(Set<String> target, String value) {
        if (value != null && !value.trim().isEmpty()) {
            target.add(value.trim());
        }
    }

    @Override
    public String toString() {
        return name + (code != null ? " [" + code + "]" : "");
    }

    public static final class Builder {
        private final String name;
        private String code;
        private String department;
        private final Set<String> aliases = new LinkedHashSet<>();
        private boolean valueRequired = true;
        private String expectedValue;

        private Builder(String name) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Expected test name must not be blank");
            }
            this.name = name.trim();
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder department(String department) {
            this.department = department;
            return this;
        }

        public Builder alias(String alias) {
            if (alias != null && !alias.trim().isEmpty()) {
                this.aliases.add(alias.trim());
            }
            return this;
        }

        public Builder valueRequired(boolean valueRequired) {
            this.valueRequired = valueRequired;
            return this;
        }

        public Builder expectedValue(String expectedValue) {
            this.expectedValue = expectedValue;
            return this;
        }

        public ExpectedTest build() {
            return new ExpectedTest(this);
        }
    }
}
