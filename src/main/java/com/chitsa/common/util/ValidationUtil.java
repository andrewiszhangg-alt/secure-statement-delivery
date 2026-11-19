package com.chitsa.common.util;

import com.chitsa.common.exception.InvalidParameterException;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@UtilityClass
public class ValidationUtil {

    /** Throws {@link InvalidParameterException} when {@code condition} is false. */
    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new InvalidParameterException(message);
        }
    }

    /** Unconditional failure after side effects (do not use {@code require(false, …)}). */
    public static void fail(String message) {
        throw new InvalidParameterException(message);
    }

    /** Returns {@code true} when any argument is {@code null}. */
    public static boolean hasNull(Object... values) {
        return values == null || Arrays.stream(values).anyMatch(Objects::isNull);
    }

    /** Returns {@code true} when the value is {@code null} or blank. */
    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Fails when {@code value} is null or blank. */
    public static void requireNotBlank(String value, String message) {
        require(!isBlank(value), message);
    }

    /** Fails when any argument is null. */
    public static void requireNonNull(Object value, String message) {
        require(!hasNull(value), message);
    }

    /** Parses a UUID, or fails with {@code message} when the text is missing or not a UUID. */
    public static UUID requireUuid(String value, String message) {
        try {
            requireNotBlank(value, message);
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new InvalidParameterException(message);
        }
    }
}
