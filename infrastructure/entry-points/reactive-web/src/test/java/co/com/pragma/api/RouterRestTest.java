package co.com.pragma.api;

import co.com.pragma.api.config.GlobalErrorHandler;
import co.com.pragma.api.dto.SolicitudCreateRequest;
import co.com.pragma.api.dto.SolicitudResponse;
import co.com.pragma.api.mapper.SolicitudMapper;
import co.com.pragma.model.solicitud.EstadoSolicitud;
import co.com.pragma.model.solicitud.Solicitud;
import co.com.pragma.model.solicitud.TipoPrestamo;
import co.com.pragma.model.solicitud.exceptions.ClientNotFoundException;
import co.com.pragma.model.solicitud.exceptions.InvalidLoanTypeException;
import co.com.pragma.model.solicitud.exceptions.SolicitudValidationException;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

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
        GlobalErrorHandler errorHandler = new GlobalErrorHandler();

        webTestClient = WebTestClient
                .bindToRouterFunction(
                        routerRest.userRoutes(userHandler)
                                .and(routerRest.solicitudRoutes(solicitudHandler))
                )
                .webFilter((exchange, chain) -> {
                    return chain.filter(exchange)
                            .onErrorResume(throwable -> errorHandler.handle(exchange, throwable));
                })
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

    @Test
    void testRegistrarSolicitud_successful() {
        UUID solicitudId = UUID.randomUUID();
        UUID tipoPrestamoId = UUID.randomUUID();
        UUID estadoId = UUID.randomUUID();
        LocalDateTime fechaCreacion = LocalDateTime.now();

        TipoPrestamo tipoPrestamo = TipoPrestamo.builder()
                .id(tipoPrestamoId)
                .nombre("PERSONAL")
                .montoMinimo(BigDecimal.valueOf(100000))
                .montoMaximo(BigDecimal.valueOf(1000000))
                .build();

        EstadoSolicitud estado = EstadoSolicitud.builder()
                .id(estadoId)
                .nombre("Pendiente de revisión")
                .descripcion("Solicitud en revisión")
                .build();

        Solicitud mockSolicitud = Solicitud.builder()
                .id(solicitudId)
                .documentoIdentidad("123456789")
                .monto(BigDecimal.valueOf(500000))
                .plazo(12)
                .tipoPrestamo(tipoPrestamo)
                .estado(estado)
                .fechaCreacion(fechaCreacion)
                .build();

        SolicitudResponse response = new SolicitudResponse();
        response.setId(solicitudId);
        response.setDocumentoIdentidad("123456789");
        response.setMonto(BigDecimal.valueOf(500000));
        response.setPlazo(12);
        response.setTipoPrestamo(new SolicitudResponse.TipoPrestamoResponse());
        response.getTipoPrestamo().setId(tipoPrestamoId);
        response.getTipoPrestamo().setNombre("PERSONAL");
        response.setEstado(new SolicitudResponse.EstadoSolicitudResponse());
        response.getEstado().setId(estadoId);
        response.getEstado().setNombre("Pendiente de revisión");
        response.getEstado().setDescripcion("Solicitud en revisión");
        response.setFechaCreacion(fechaCreacion);

        // Setup mocks
        when(solicitudMapper.toDomain(any(SolicitudCreateRequest.class)))
                .thenReturn(Mono.just(mockSolicitud));
        when(solicitudUseCase.registrarSolicitud(any(Solicitud.class)))
                .thenReturn(Mono.just(mockSolicitud));
        when(solicitudMapper.toResponse(any(Solicitud.class)))
                .thenReturn(response);

        String solicitudRequestBody = """
                {
                    "documentoIdentidad": "123456789",
                    "monto": 500000,
                    "plazo": 12,
                    "tipoPrestamoNombre": "PERSONAL"
                }
                """;

        webTestClient.post()
                .uri("/api/v1/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(solicitudRequestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(solicitudId.toString())
                .jsonPath("$.documentoIdentidad").isEqualTo("123456789")
                .jsonPath("$.monto").isEqualTo(500000)
                .jsonPath("$.plazo").isEqualTo(12)
                .jsonPath("$.tipoPrestamo.nombre").isEqualTo("PERSONAL")
                .jsonPath("$.estado.nombre").isEqualTo("Pendiente de revisión");
    }

    @Test
    void testRegistrarSolicitud_invalidInput() {
        // Mock para manejar entrada inválida
        when(solicitudMapper.toDomain(any(SolicitudCreateRequest.class)))
                .thenReturn(Mono.error(new SolicitudValidationException("Datos de solicitud inválidos")));

        String solicitudRequestBody = """
                {
                    "documentoIdentidad": "",
                    "monto": -500000,
                    "plazo": -12,
                    "tipoPrestamoNombre": ""
                }
                """;

        webTestClient.post()
                .uri("/api/v1/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(solicitudRequestBody)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isNotEmpty();
    }

    @Test
    void testRegistrarSolicitud_clientNotFound() {
        TipoPrestamo tipoPrestamo = TipoPrestamo.builder()
                .id(UUID.randomUUID())
                .nombre("PERSONAL")
                .montoMinimo(BigDecimal.valueOf(100000))
                .montoMaximo(BigDecimal.valueOf(1000000))
                .build();

        Solicitud mockSolicitud = Solicitud.builder()
                .documentoIdentidad("999999999")
                .monto(BigDecimal.valueOf(500000))
                .plazo(12)
                .tipoPrestamo(tipoPrestamo)
                .build();

        when(solicitudMapper.toDomain(any(SolicitudCreateRequest.class)))
                .thenReturn(Mono.just(mockSolicitud));
        when(solicitudUseCase.registrarSolicitud(any(Solicitud.class)))
                .thenReturn(Mono.error(new ClientNotFoundException("Cliente no encontrado con documento: 999999999")));

        String solicitudRequestBody = """
                {
                    "documentoIdentidad": "999999999",
                    "monto": 500000,
                    "plazo": 12,
                    "tipoPrestamoNombre": "PERSONAL"
                }
                """;

        webTestClient.post()
                .uri("/api/v1/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(solicitudRequestBody)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Cliente no encontrado con documento: 999999999");
    }

    @Test
    void testRegistrarSolicitud_invalidLoanType() {
        when(solicitudMapper.toDomain(any(SolicitudCreateRequest.class)))
                .thenReturn(Mono.error(new InvalidLoanTypeException("Tipo de préstamo inválido: INVALID")));

        String solicitudRequestBody = """
                {
                    "documentoIdentidad": "123456789",
                    "monto": 500000,
                    "plazo": 12,
                    "tipoPrestamoNombre": "INVALID"
                }
                """;

        webTestClient.post()
                .uri("/api/v1/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(solicitudRequestBody)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Tipo de préstamo inválido: INVALID");
    }

    @Test
    void testRegistrarSolicitud_invalidAmountRange() {
        TipoPrestamo tipoPrestamo = TipoPrestamo.builder()
                .id(UUID.randomUUID())
                .nombre("PERSONAL")
                .montoMinimo(BigDecimal.valueOf(100000))
                .montoMaximo(BigDecimal.valueOf(1000000))
                .build();

        Solicitud mockSolicitud = Solicitud.builder()
                .documentoIdentidad("123456789")
                .monto(BigDecimal.valueOf(50000))
                .plazo(12)
                .tipoPrestamo(tipoPrestamo)
                .build();

        when(solicitudMapper.toDomain(any(SolicitudCreateRequest.class)))
                .thenReturn(Mono.just(mockSolicitud));
        when(solicitudUseCase.registrarSolicitud(any(Solicitud.class)))
                .thenReturn(Mono.error(new SolicitudValidationException(
                        "El monto debe estar entre 100000 y 1000000 para el tipo PERSONAL"
                )));

        String solicitudRequestBody = """
                {
                    "documentoIdentidad": "123456789",
                    "monto": 50000,
                    "plazo": 12,
                    "tipoPrestamoNombre": "PERSONAL"
                }
                """;

        webTestClient.post()
                .uri("/api/v1/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(solicitudRequestBody)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("El monto debe estar entre 100000 y 1000000 para el tipo PERSONAL");
    }
}