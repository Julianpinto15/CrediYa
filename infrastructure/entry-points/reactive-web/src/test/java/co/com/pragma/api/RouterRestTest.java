package co.com.pragma.api;

import co.com.pragma.api.mapper.SolicitudMapper;
import co.com.pragma.model.solicitud.gateways.TipoPrestamoRepository;
import co.com.pragma.model.user.User;
import co.com.pragma.usecase.user.UserUseCase;
import co.com.pragma.usecase.solicitud.SolicitudUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouterRestTest {

    @Mock
    private UserUseCase userUseCase;

    @Mock
    private SolicitudUseCase solicitudUseCase;


    @Mock
    private TipoPrestamoRepository tipoPrestamoRepository;

    @Mock
    private SolicitudMapper solicitudMapper;


    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        UserHandler userHandler = new UserHandler(userUseCase);
        SolicitudHandler solicitudHandler = new SolicitudHandler(
                solicitudUseCase,
                tipoPrestamoRepository,
                solicitudMapper
        );
        RouterRest routerRest = new RouterRest();

        webTestClient = WebTestClient
                .bindToRouterFunction(
                        routerRest.userRoutes(userHandler)
                                .and(routerRest.solicitudRoutes(solicitudHandler))
                )
                .build();
    }

    @Test
    void testRegistrarUsuario_routeOk() {
        User mockUser = User.builder()
                .id("1")
                .nombres("Juan")
                .apellidos("Pérez")
                .documentoIdentidad("123456789")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .direccion("Calle 123")
                .telefono("3101234567")
                .correoElectronico("juan@example.com")
                .salarioBase(BigDecimal.valueOf(1000000))
                .build();

        when(userUseCase.save(any(User.class))).thenReturn(Mono.just(mockUser));

        String userRequestBody = """
                {
                    "nombres": "Juan",
                    "apellidos": "Pérez",
                    "documentoIdentidad": "123456789",
                    "fechaNacimiento": "1990-01-01",
                    "direccion": "Calle 123",
                    "telefono": "3101234567",
                    "correoElectronico": "juan@example.com",
                    "salarioBase": 1000000
                }
                """;

        webTestClient.post()
                .uri("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userRequestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("1")
                .jsonPath("$.nombres").isEqualTo("Juan")
                .jsonPath("$.apellidos").isEqualTo("Pérez")
                .jsonPath("$.documentoIdentidad").isEqualTo("123456789");
    }

    @Test
    void testExistsByDocument_routeOk() {
        when(userUseCase.existsByDocumento(anyString())).thenReturn(Mono.just(true));

        webTestClient.get()
                .uri("/api/v1/usuarios/exists/123456789")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.exists").isEqualTo(true);
    }

    @Test
    void testExistsByDocument_notFound() {
        when(userUseCase.existsByDocumento(anyString())).thenReturn(Mono.just(false));

        webTestClient.get()
                .uri("/api/v1/usuarios/exists/999999999")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.exists").isEqualTo(false);
    }

    /*@Test
    void testRegistrarSolicitud_routeExists() {
        // Configuramos el mock para que no devuelva null
        when(solicitudUseCase.registrarSolicitud(any())).thenReturn(Mono.empty());

        String solicitudRequestBody = """
                {
                    "clienteId": "1",
                    "monto": 500000,
                    "tipoPrestamoId": "1"
                }
                """;

        // Verificamos que la ruta existe - puede fallar por validaciones pero no por routing
        webTestClient.post()
                .uri("/api/v1/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(solicitudRequestBody)
                .exchange()
                .expectStatus().is4xxClientError(); // Puede ser 400 por validaciones de negocio
    }

    @Test
    void testRegistrarSolicitud_successfulRoute() {
        // Mock de una solicitud exitosa
        co.com.pragma.model.solicitud.Solicitud mockSolicitud =
                co.com.pragma.model.solicitud.Solicitud.builder()
                        .id("solicitud-123")
                        .clienteId("1")
                        .monto(new java.math.BigDecimal("500000"))
                        .tipoPrestamoId("1")
                        .estadoId("1")
                        .fechaSolicitud(java.time.LocalDateTime.now())
                        .build();

        when(solicitudUseCase.registrarSolicitud(any())).thenReturn(Mono.just(mockSolicitud));

        String solicitudRequestBody = """
                {
                    "clienteId": "1",
                    "monto": 500000,
                    "tipoPrestamoId": "1"
                }
                """;

        webTestClient.post()
                .uri("/api/v1/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(solicitudRequestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("solicitud-123")
                .jsonPath("$.clienteId").isEqualTo("1")
                .jsonPath("$.monto").isEqualTo(500000);
    }*/
}