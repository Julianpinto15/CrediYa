package co.com.pragma.usecase.user;

import co.com.pragma.model.user.User;
import co.com.pragma.model.user.exceptions.EmailAlreadyExistsException;
import co.com.pragma.model.user.gateways.PasswordEncoder;
import co.com.pragma.model.user.gateways.UserRepository;
import co.com.pragma.usecase.user.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class UserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Mono<User> save(User user) {
        return validateUser(user)
                .then(checkEmailExists(user.getCorreoElectronico()))
                .then(encodePassword(user))
                .flatMap(userRepository::save);
    }

    public Mono<Boolean> existsByDocumento(String documento) {
        return userRepository.existsByDocumentoIdentidad(documento);
    }

    private Mono<Void> validateUser(User user) {
        return Mono.fromRunnable(() -> {
            ValidationUtils.validateNotBlank(user.getNombres(), "Los nombres son obligatorios");
            ValidationUtils.validateNotBlank(user.getApellidos(), "Los apellidos son obligatorios");
            ValidationUtils.validateNotBlank(user.getCorreoElectronico(), "El correo electrónico es obligatorio");
            ValidationUtils.validateNotNull(user.getSalarioBase(), "El salario base es obligatorio");
            ValidationUtils.validateNotBlank(user.getPassword(), "La contraseña es obligatoria");
            ValidationUtils.validateNotNull(user.getRol(), "El rol es obligatorio");

            ValidationUtils.validateEmail(user.getCorreoElectronico());
            ValidationUtils.validateSalaryRange(user.getSalarioBase());
            ValidationUtils.validatePasswordLength(user.getPassword());
        });
    }

    private Mono<Void> checkEmailExists(String correoElectronico) {
        return userRepository.existsByCorreo(correoElectronico)
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.error(new EmailAlreadyExistsException("El correo electrónico ya está registrado"));
                    }
                    return Mono.empty();
                });
    }

    private Mono<User> encodePassword(User user) {
        return passwordEncoder.encode(user.getPassword())
                .map(encodedPassword -> user.toBuilder()
                        .password(encodedPassword)
                        .build());
    }
}