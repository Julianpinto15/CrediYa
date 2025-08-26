package co.com.pragma.usecase.solicitud.utils;

import co.com.pragma.model.solicitud.exceptions.SolicitudValidationException;

import java.math.BigDecimal;

/**
 * Utility class for common validations
 * Following Clean Architecture principles and Bancolombia scaffold structure
 */
public final class ValidationUtils {

    private ValidationUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Validates that a string value is not null or empty
     * @param value the string to validate
     * @param message error message if validation fails
     * @throws SolicitudValidationException if validation fails
     */
    public static void validateNotNullOrEmpty(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new SolicitudValidationException(message);
        }
    }

    /**
     * Validates that an object is not null
     * @param value the object to validate
     * @param message error message if validation fails
     * @throws SolicitudValidationException if validation fails
     */
    public static void validateNotNull(Object value, String message) {
        if (value == null) {
            throw new SolicitudValidationException(message);
        }
    }

    /**
     * Validates that a BigDecimal amount is not null and greater than the minimum value
     * @param amount the amount to validate
     * @param minValue the minimum allowed value
     * @param message error message if validation fails
     * @throws SolicitudValidationException if validation fails
     */
    public static void validateAmount(BigDecimal amount, BigDecimal minValue, String message) {
        if (amount == null || amount.compareTo(minValue) < 0) {
            throw new SolicitudValidationException(message);
        }
    }

    /**
     * Validates that an integer value is not null and greater than or equal to the minimum value
     * @param value the integer to validate
     * @param minValue the minimum allowed value
     * @param message error message if validation fails
     * @throws SolicitudValidationException if validation fails
     */
    public static void validateInteger(Integer value, Integer minValue, String message) {
        if (value == null || value < minValue) {
            throw new SolicitudValidationException(message);
        }
    }
}
