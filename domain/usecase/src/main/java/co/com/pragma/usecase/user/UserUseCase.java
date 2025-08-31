package co.com.pragma.usecase.user;

import co.com.pragma.model.user.User;
import co.com.pragma.model.user.exceptions.EmailAlreadyExistsException;
import co.com.pragma.model.user.exceptions.UserValidationException;
import co.com.pragma.model.user.gateways.UserRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class UserUseCase {

    private final UserRepository userRepository;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    private static final BigDecimal MIN_SALARY = BigDecimal.ZERO;
    private static final BigDecimal MAX_SALARY = new BigDecimal("15000000");

    public Mono<User> save(User user) {
        return Mono.defer(() -> {
            try {
                validateUser(user); // si falla, lanzamos excepción y la capturamos
            } catch (UserValidationException e) {
                return Mono.error(e);
            }

            return userRepository.existsByCorreo(user.getCorreoElectronico())
                    .flatMap(exists -> {
                        if (exists) {
                            return Mono.error(new EmailAlreadyExistsException("El correo ya existe"));
                        }
                        return userRepository.save(user);
                    });
        });
    }


    public Mono<Boolean> existsByDocumento(String documento) {
        return userRepository.existsByDocumentoIdentidad(documento);
    }

    private void validateUser(User user) {
        // Validar nombres
        if (user.getNombres() == null || user.getNombres().trim().isEmpty()) {
            throw new UserValidationException("El campo nombres es obligatorio");
        }

        // Validar apellidos
        if (user.getApellidos() == null || user.getApellidos().trim().isEmpty()) {
            throw new UserValidationException("El campo apellidos es obligatorio");
        }

        // Validar correo electrónico
        if (user.getCorreoElectronico() == null || user.getCorreoElectronico().trim().isEmpty()) {
            throw new UserValidationException("El campo correo_electronico es obligatorio");
        }

        if (!EMAIL_PATTERN.matcher(user.getCorreoElectronico()).matches()) {
            throw new UserValidationException("El formato del correo electrónico no es válido");
        }

        // Validar salario
        if (user.getSalarioBase() == null) {
            throw new UserValidationException("El campo salario_base es obligatorio");
        }

        if (user.getSalarioBase().compareTo(MIN_SALARY) < 0 ||
                user.getSalarioBase().compareTo(MAX_SALARY) > 0) {
            throw new UserValidationException("El salario base debe estar entre 0 y 15,000,000");
        }
    }
}