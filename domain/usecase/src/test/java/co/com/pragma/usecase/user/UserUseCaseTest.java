package co.com.pragma.usecase.user;

import co.com.pragma.model.user.User;
import co.com.pragma.model.user.exceptions.EmailAlreadyExistsException;
import co.com.pragma.model.user.exceptions.UserValidationException;
import co.com.pragma.model.user.gateways.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserUseCase userUseCase;

    private User validUser;

    @BeforeEach
    void setUp() {
        validUser = User.builder()
                .nombres("Juan")
                .apellidos("Perez")
                .documentoIdentidad("123456789")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .direccion("Calle 123")
                .telefono("3101234567")
                .correoElectronico("juan@example.com")
                .salarioBase(BigDecimal.valueOf(1000000))
                .build();
    }

    @Test
    void save_validUser_savesSuccessfully() {
        when(userRepository.existsByCorreo(anyString())).thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(validUser));

        StepVerifier.create(userUseCase.save(validUser))
                .expectNext(validUser)
                .verifyComplete();
    }

    @Test
    void save_nullNombres_throwsValidationException() {
        User invalidUser = validUser.toBuilder().nombres(null).build();

        StepVerifier.create(userUseCase.save(invalidUser))
                .expectErrorMatches(e -> e instanceof UserValidationException &&
                        e.getMessage().equals("El campo nombres es obligatorio"))
                .verify();
    }

    @Test
    void save_emptyNombres_throwsValidationException() {
        User invalidUser = validUser.toBuilder().nombres("").build();

        StepVerifier.create(userUseCase.save(invalidUser))
                .expectErrorMatches(e -> e instanceof UserValidationException &&
                        e.getMessage().equals("El campo nombres es obligatorio"))
                .verify();
    }

    @Test
    void save_nullApellidos_throwsValidationException() {
        User invalidUser = validUser.toBuilder().apellidos(null).build();

        StepVerifier.create(userUseCase.save(invalidUser))
                .expectErrorMatches(e -> e instanceof UserValidationException &&
                        e.getMessage().equals("El campo apellidos es obligatorio"))
                .verify();
    }

    @Test
    void save_nullCorreo_throwsValidationException() {
        User invalidUser = validUser.toBuilder().correoElectronico(null).build();

        StepVerifier.create(userUseCase.save(invalidUser))
                .expectErrorMatches(e -> e instanceof UserValidationException &&
                        e.getMessage().equals("El campo correo_electronico es obligatorio"))
                .verify();
    }

    @Test
    void save_invalidEmail_throwsValidationException() {
        User invalidUser = validUser.toBuilder().correoElectronico("invalid").build();

        StepVerifier.create(userUseCase.save(invalidUser))
                .expectErrorMatches(e -> e instanceof UserValidationException &&
                        e.getMessage().equals("El formato del correo electrónico no es válido"))
                .verify();
    }

    @Test
    void save_nullSalario_throwsValidationException() {
        User invalidUser = validUser.toBuilder().salarioBase(null).build();

        StepVerifier.create(userUseCase.save(invalidUser))
                .expectErrorMatches(e -> e instanceof UserValidationException &&
                        e.getMessage().equals("El campo salario_base es obligatorio"))
                .verify();
    }

    @Test
    void save_negativeSalario_throwsValidationException() {
        User invalidUser = validUser.toBuilder().salarioBase(BigDecimal.valueOf(-100)).build();

        StepVerifier.create(userUseCase.save(invalidUser))
                .expectErrorMatches(e -> e instanceof UserValidationException &&
                        e.getMessage().equals("El salario base debe estar entre 0 y 15,000,000"))
                .verify();
    }

    @Test
    void save_excessiveSalario_throwsValidationException() {
        User invalidUser = validUser.toBuilder().salarioBase(new BigDecimal("16000000")).build();

        StepVerifier.create(userUseCase.save(invalidUser))
                .expectErrorMatches(e -> e instanceof UserValidationException &&
                        e.getMessage().equals("El salario base debe estar entre 0 y 15,000,000"))
                .verify();
    }

    @Test
    void save_duplicateEmail_throwsEmailAlreadyExists() {
        when(userRepository.existsByCorreo(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(userUseCase.save(validUser))  // Usar validUser
                .expectErrorMatches(e -> e instanceof EmailAlreadyExistsException &&
                        e.getMessage().equals("El correo ya existe"))
                .verify();

        verify(userRepository).existsByCorreo(validUser.getCorreoElectronico());
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    void existsByDocumento_userExists_returnsTrue() {
        when(userRepository.existsByDocumentoIdentidad("123456789")).thenReturn(Mono.just(true));

        StepVerifier.create(userUseCase.existsByDocumento("123456789"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void existsByDocumento_userNotExists_returnsFalse() {
        when(userRepository.existsByDocumentoIdentidad("123456789")).thenReturn(Mono.just(false));

        StepVerifier.create(userUseCase.existsByDocumento("123456789"))
                .expectNext(false)
                .verifyComplete();
    }
}