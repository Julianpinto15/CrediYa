package co.com.pragma.usecase.user.utils;

import co.com.pragma.model.user.exceptions.UserValidationException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private static final BigDecimal MIN_SALARY = BigDecimal.ZERO;
    private static final BigDecimal MAX_SALARY = new BigDecimal("15000000");
    private static final int MIN_PASSWORD_LENGTH = 6;

    public static void validateNotBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new UserValidationException(message);
        }
    }

    public static void validateNotNull(Object value, String message) {
        if (value == null) {
            throw new UserValidationException(message);
        }
    }

    public static void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new UserValidationException("El formato del correo electrónico no es válido");
        }
    }

    public static void validateSalaryRange(BigDecimal salary) {
        if (salary == null) {
            throw new UserValidationException("El salario base es obligatorio");
        }

        if (salary.compareTo(MIN_SALARY) <= 0) {
            throw new UserValidationException("El salario base debe ser mayor a 0");
        }

        if (salary.compareTo(MAX_SALARY) > 0) {
            throw new UserValidationException("El salario base no puede ser mayor a 15,000,000");
        }
    }

    public static void validatePasswordLength(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new UserValidationException("La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }
    }
}
