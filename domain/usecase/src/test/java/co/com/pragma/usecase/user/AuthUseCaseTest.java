package co.com.pragma.usecase.user;


import co.com.pragma.model.user.AuthenticatedUser;
import co.com.pragma.model.user.Login;
import co.com.pragma.model.user.User;
import co.com.pragma.model.user.exceptions.InvalidCredentialsException;
import co.com.pragma.model.user.gateways.JwtTokenGateway;
import co.com.pragma.model.user.gateways.PasswordEncoder;
import co.com.pragma.model.user.gateways.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenGateway jwtTokenGateway;

    private AuthUseCase authUseCase;

    private User testUser;
    private Login testLogin;

    @BeforeEach
    void setUp() {
        authUseCase = new AuthUseCase(userRepository, passwordEncoder, jwtTokenGateway);

        testUser = User.builder()
                .id("1")
                .nombres("Juan")
                .apellidos("Pérez")
                .documentoIdentidad("12345678")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .direccion("Calle 123")
                .telefono("555-1234")
                .correoElectronico("juan.perez@test.com")
                .salarioBase(new BigDecimal("3000000"))
                .password("$2a$10$encodedPassword")
                .rol(User.Rol.CLIENTE)
                .build();

        testLogin = Login.builder()
                .correoElectronico("juan.perez@test.com")
                .password("plainPassword")
                .build();
    }

    @Test
    void shouldLoginSuccessfully() {
        // Arrange
        when(userRepository.findByCorreoElectronico("juan.perez@test.com"))
                .thenReturn(Mono.just(testUser));
        when(passwordEncoder.matches("plainPassword", "$2a$10$encodedPassword"))
                .thenReturn(Mono.just(true));
        when(jwtTokenGateway.generateToken(any(AuthenticatedUser.class)))
                .thenReturn("jwt-token");
        when(jwtTokenGateway.generateRefreshToken(any(AuthenticatedUser.class)))
                .thenReturn("refresh-token");

        // Act & Assert
        StepVerifier.create(authUseCase.login(testLogin))
                .expectNextMatches(authResponse -> {
                    return "jwt-token".equals(authResponse.getToken()) &&
                            "refresh-token".equals(authResponse.getRefreshToken()) &&
                            testUser.equals(authResponse.getUsuario()) &&
                            authResponse.getExpiresIn() == 3600;
                })
                .verifyComplete();
    }

    @Test
    void shouldFailLoginWithInvalidEmail() {
        // Arrange
        when(userRepository.findByCorreoElectronico("invalid@test.com"))
                .thenReturn(Mono.empty());

        Login invalidLogin = Login.builder()
                .correoElectronico("invalid@test.com")
                .password("plainPassword")
                .build();

        // Act & Assert
        StepVerifier.create(authUseCase.login(invalidLogin))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    void shouldFailLoginWithInvalidPassword() {
        // Arrange
        when(userRepository.findByCorreoElectronico("juan.perez@test.com"))
                .thenReturn(Mono.just(testUser));
        when(passwordEncoder.matches("wrongPassword", "$2a$10$encodedPassword"))
                .thenReturn(Mono.just(false));

        Login invalidLogin = Login.builder()
                .correoElectronico("juan.perez@test.com")
                .password("wrongPassword")
                .build();

        // Act & Assert
        StepVerifier.create(authUseCase.login(invalidLogin))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    void shouldValidateTokenSuccessfully() {
        // Arrange
        String token = "valid-jwt-token";
        AuthenticatedUser expectedUser = AuthenticatedUser.builder()
                .userId("1")
                .correoElectronico("juan.perez@test.com")
                .rol(User.Rol.CLIENTE)
                .token(token)
                .build();

        when(jwtTokenGateway.validateToken(token))
                .thenReturn(Mono.just(expectedUser));

        // Act & Assert
        StepVerifier.create(authUseCase.validateToken(token))
                .expectNext(expectedUser)
                .verifyComplete();
    }

    @Test
    void shouldFailTokenValidation() {
        // Arrange
        String invalidToken = "invalid-token";
        when(jwtTokenGateway.validateToken(invalidToken))
                .thenReturn(Mono.error(new RuntimeException("Invalid token")));

        // Act & Assert
        StepVerifier.create(authUseCase.validateToken(invalidToken))
                .expectError(RuntimeException.class)
                .verify();
    }
}