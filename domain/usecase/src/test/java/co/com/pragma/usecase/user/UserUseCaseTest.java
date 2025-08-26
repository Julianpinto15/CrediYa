package co.com.pragma.usecase.user;

import co.com.pragma.model.user.User;
import co.com.pragma.model.user.exceptions.EmailAlreadyExistsException;
import co.com.pragma.model.user.exceptions.UserValidationException;
import co.com.pragma.model.user.gateways.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class UserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    private UserUseCase userUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userUseCase = new UserUseCase(userRepository);
    }

    @Test
    void save_validUser_savesSuccessfully() {
        User user = User.builder()
                .nombres("Juan")
                .apellidos("Perez")
                .correoElectronico("juan@example.com")
                .salarioBase(BigDecimal.valueOf(1000000))
                .build();

        when(userRepository.existsByCorreo(anyString())).thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));

        StepVerifier.create(userUseCase.save(user))
                .expectNext(user)
                .verifyComplete();
    }

    @Test
    void save_invalidEmail_throwsValidationException() {
        User user = User.builder()
                .nombres("Juan")
                .apellidos("Perez")
                .correoElectronico("invalid")
                .salarioBase(BigDecimal.valueOf(1000000))
                .build();

        StepVerifier.create(userUseCase.save(user))
                .expectError(UserValidationException.class)
                .verify();
    }

    @Test
    void save_duplicateEmail_throwsEmailAlreadyExists() {
        User user = User.builder()
                .nombres("Juan")
                .apellidos("Perez")
                .correoElectronico("juan@example.com")
                .salarioBase(BigDecimal.valueOf(1000000))
                .build(); // 👈 Aquí cierras el builder y obtienes un User

        when(userRepository.existsByCorreo(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(userUseCase.save(user))
                .expectError(EmailAlreadyExistsException.class)
                .verify();
    }

}