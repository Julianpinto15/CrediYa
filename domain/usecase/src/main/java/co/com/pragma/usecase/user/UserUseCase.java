package co.com.pragma.usecase.user;

import co.com.pragma.model.user.User;
import co.com.pragma.model.user.exceptions.EmailAlreadyExistsException;
import co.com.pragma.model.user.exceptions.UserValidationException;
import co.com.pragma.model.user.gateways.UserRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class UserUseCase {
    private final UserRepository userRepository;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    private static final BigDecimal MIN_SALARY = BigDecimal.ZERO;
    private static final BigDecimal MAX_SALARY = new BigDecimal("15000000");

    @FunctionalInterface
    interface UserValidator {
        void validate(User user) throws UserValidationException;
    }

    private final List<UserValidator> validators = List.of(
            user -> validateNotNullOrEmpty(user.getNombres(), "El campo nombres es obligatorio"),
            user -> validateNotNullOrEmpty(user.getApellidos(), "El campo apellidos es obligatorio"),
            user -> validateNotNullOrEmpty(user.getCorreoElectronico(), "El campo correo_electronico es obligatorio"),
            user -> validateSalarioBaseNotNull(user.getSalarioBase()),
            user -> validateEmailFormat(user.getCorreoElectronico()),
            user -> validateSalaryRange(user.getSalarioBase())
    );

    public Mono<User> save(User user) {
        return validateUser(user)
                .then(userRepository.existsByCorreo(user.getCorreoElectronico()))
                .flatMap(existe -> {
                    if (existe) {
                        return Mono.error(new EmailAlreadyExistsException(user.getCorreoElectronico()));
                    }
                    return userRepository.save(user);
                });
    }

    private Mono<Void> validateUser(User user) {
        return Mono.fromRunnable(() -> validators.forEach(validator -> validator.validate(user)));
    }

    private void validateNotNullOrEmpty(String value, String errorMessage) {
        if (value == null || value.trim().isEmpty()) {
            throw new UserValidationException(errorMessage);
        }
    }

    private void validateSalarioBaseNotNull(BigDecimal salarioBase) {
        if (salarioBase == null) {
            throw new UserValidationException("El campo salario_base es obligatorio");
        }
    }

    private void validateEmailFormat(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new UserValidationException("El formato del correo electrónico no es válido");
        }
    }

    private void validateSalaryRange(BigDecimal salary) {
        if (salary.compareTo(MIN_SALARY) < 0 || salary.compareTo(MAX_SALARY) > 0) {
            throw new UserValidationException("El salario base debe estar entre 0 y 15,000,000");
        }
    }
}