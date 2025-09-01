package co.com.pragma.api;

import co.com.pragma.api.dto.UserRequest;
import co.com.pragma.model.user.User;
import co.com.pragma.model.user.exceptions.EmailAlreadyExistsException;
import co.com.pragma.model.user.exceptions.UserValidationException;
import co.com.pragma.usecase.user.UserUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@ExtendWith(MockitoExtension.class)
class UserHandlerTest {

    @Mock
    private UserUseCase userUseCase;

    @InjectMocks
    private UserHandler userHandler;

    private WebTestClient webTestClient;

    private UserRequest validRequest;
    private User validUser;

    @BeforeEach
    void setUp() {
        // Crear RouterFunction para testear
        RouterFunction<ServerResponse> routerFunction = RouterFunctions
                .route(POST("/api/v1/usuarios").and(accept(MediaType.APPLICATION_JSON)),
                        userHandler::registrarUsuario)
                .andRoute(GET("/api/v1/usuarios/exists/{documento}"),
                        userHandler::existsByDocument);

        // Configurar WebTestClient con RouterFunction
        webTestClient = WebTestClient
                .bindToRouterFunction(routerFunction)
                .build();

        validRequest = new UserRequest();
        validRequest.setNombres("Juan");
        validRequest.setApellidos("Perez");
        validRequest.setDocumentoIdentidad("123456789");
        validRequest.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        validRequest.setDireccion("Calle 123");
        validRequest.setTelefono("3101234567");
        validRequest.setCorreoElectronico("juan@example.com");
        validRequest.setSalarioBase(BigDecimal.valueOf(1000000));

        validUser = User.builder()
                .id("1")
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
    void registrarUsuario_success_returnsOk() {
        when(userUseCase.save(any(User.class))).thenReturn(Mono.just(validUser));

        webTestClient.post()
                .uri("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("1")
                .jsonPath("$.nombres").isEqualTo("Juan")
                .jsonPath("$.apellidos").isEqualTo("Perez")
                .jsonPath("$.correoElectronico").isEqualTo("juan@example.com");

    }

    @Test
    void registrarUsuario_invalidEmail_returnsBadRequest() {
        UserRequest invalidRequest = new UserRequest();
        invalidRequest.setNombres("Juan");
        invalidRequest.setApellidos("Perez");
        invalidRequest.setCorreoElectronico("invalid");
        invalidRequest.setSalarioBase(BigDecimal.valueOf(1000000));

        when(userUseCase.save(any(User.class)))
                .thenReturn(Mono.error(new UserValidationException("El formato del correo electrónico no es válido")));

        webTestClient.post()
                .uri("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("El formato del correo electrónico no es válido");
    }

    @Test
    void registrarUsuario_duplicateEmail_returnsConflict() {
        when(userUseCase.save(any(User.class)))
                .thenReturn(Mono.error(new EmailAlreadyExistsException("El correo ya existe")));

        webTestClient.post()
                .uri("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequest)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.message").isEqualTo("El correo electrónico ya está registrado");
    }

    @Test
    void existsByDocument_userExists_returnsTrue() {
        when(userUseCase.existsByDocumento("123456789")).thenReturn(Mono.just(true));

        webTestClient.get()
                .uri("/api/v1/usuarios/exists/123456789")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.exists").isEqualTo(true);
    }

    @Test
    void existsByDocument_userNotExists_returnsFalse() {
        when(userUseCase.existsByDocumento("123456789")).thenReturn(Mono.just(false));

        webTestClient.get()
                .uri("/api/v1/usuarios/exists/123456789")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.exists").isEqualTo(false);
    }
}