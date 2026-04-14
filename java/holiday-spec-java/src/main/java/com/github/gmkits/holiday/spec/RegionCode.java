package com.github.gmkits.holiday.spec;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * An immutable value class that wraps a validated region code string.
 *
 * <p>Valid region codes contain only ASCII letters, digits, and hyphens
 * (e.g. {@code "CN"}, {@code "CN-BJ"}).</p>
 */
public final class RegionCode {

    private static final Pattern VALID_PATTERN = Pattern.compile("^[A-Za-z0-9\\-]+$");

    private final String code;

    /**
     * Creates a new {@code RegionCode} after validating its format.
     *
     * @param code the region code string
     * @throws IllegalArgumentException if {@code code} is null, empty, or
     *                                  contains characters other than letters,
     *                                  digits, and hyphens
     */
    public RegionCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Region code must not be null or empty");
        }
        if (!VALID_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    "Invalid region code: '" + code + "'. Only letters, digits, and hyphens are allowed.");
        }
        this.code = code;
    }

    /**
     * Returns the raw region code string.
     *
     * @return the region code
     */
    public String getCode() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegionCode)) return false;
        RegionCode that = (RegionCode) o;
        return code.equals(that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return code;
    }
}
