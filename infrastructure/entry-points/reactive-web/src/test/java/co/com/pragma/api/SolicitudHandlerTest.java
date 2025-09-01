package co.com.pragma.api;

import co.com.pragma.api.config.GlobalErrorHandler;
import co.com.pragma.api.dto.SolicitudCreateRequest;
import co.com.pragma.api.dto.SolicitudResponse;
import co.com.pragma.api.mapper.SolicitudMapper;
import co.com.pragma.model.solicitud.Solicitud;
import co.com.pragma.model.solicitud.TipoPrestamo;
import co.com.pragma.model.solicitud.exceptions.ClientNotFoundException;
import co.com.pragma.model.solicitud.exceptions.InvalidLoanTypeException;
import co.com.pragma.model.solicitud.exceptions.SolicitudValidationException;
import co.com.pragma.model.solicitud.gateways.TipoPrestamoRepository;
import co.com.pragma.usecase.solicitud.SolicitudUseCase;
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
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@ExtendWith(MockitoExtension.class)
class SolicitudHandlerTest {

    @Mock
    private SolicitudUseCase solicitudUseCase;

    @Mock
    private SolicitudMapper solicitudMapper;

    @Mock
    private TipoPrestamoRepository tipoPrestamoRepository;

    @InjectMocks
    private SolicitudHandler solicitudHandler;

    private WebTestClient webTestClient;

    private SolicitudCreateRequest validRequest;
    private Solicitud validSolicitud;
    private SolicitudResponse validResponse;
    private TipoPrestamo tipoPrestamo;

    @BeforeEach
    void setUp() {
        // Crear una instancia del GlobalErrorHandler
        GlobalErrorHandler errorHandler = new GlobalErrorHandler();

        // Configurar RouterFunction para testear
        RouterFunction<ServerResponse> routerFunction = RouterFunctions
                .route(POST("/api/v1/solicitudes").and(accept(MediaType.APPLICATION_JSON)),
                        solicitudHandler::registrarSolicitud);

        // Configurar WebTestClient con el RouterFunction y el ErrorHandler
        webTestClient = WebTestClient
                .bindToRouterFunction(routerFunction)
                .webFilter((exchange, chain) -> {
                    // Simular el manejo de excepciones global
                    return chain.filter(exchange).onErrorResume(Throwable.class, ex ->
                            Mono.defer(() -> errorHandler.handle(exchange, ex))
                    );
                })
                .build();

        // Configurar solicitud de prueba
        validRequest = new SolicitudCreateRequest();
        validRequest.setDocumentoIdentidad("123456789");
        validRequest.setMonto(BigDecimal.valueOf(15000000));
        validRequest.setPlazo(24);
        validRequest.setTipoPrestamoNombre("PERSONAL");

        // Configurar tipo de préstamo con UUID
        UUID tipoPrestamoId = UUID.randomUUID();
        tipoPrestamo = TipoPrestamo.builder()
                .id(tipoPrestamoId)
                .nombre("PERSONAL")
                .montoMinimo(BigDecimal.valueOf(1000000))
                .montoMaximo(BigDecimal.valueOf(50000000))
                .build();

        // Configurar solicitud del dominio con UUID
        UUID solicitudId = UUID.randomUUID();
        validSolicitud = Solicitud.builder()
                .id(solicitudId)
                .documentoIdentidad("123456789")
                .monto(BigDecimal.valueOf(15000000))
                .plazo(24)
                .tipoPrestamo(tipoPrestamo)
                .fechaCreacion(LocalDateTime.now())
                .build();

        // Configurar respuesta esperada con UUID
        validResponse = new SolicitudResponse();
        validResponse.setId(solicitudId);
        validResponse.setDocumentoIdentidad("123456789");
        validResponse.setMonto(BigDecimal.valueOf(15000000));
        validResponse.setPlazo(24);
        validResponse.setFechaCreacion(validSolicitud.getFechaCreacion());
        SolicitudResponse.TipoPrestamoResponse tipoPrestamoResponse = new SolicitudResponse.TipoPrestamoResponse();
        tipoPrestamoResponse.setId(tipoPrestamoId);
        tipoPrestamoResponse.setNombre("PERSONAL");
        validResponse.setTipoPrestamo(tipoPrestamoResponse);
    }

    @Test
    void registrarSolicitud_success_returnsOk() {
        when(solicitudMapper.toDomain(any(SolicitudCreateRequest.class))).thenReturn(Mono.just(validSolicitud));
        when(solicitudUseCase.registrarSolicitud(any(Solicitud.class))).thenReturn(Mono.just(validSolicitud));
        when(solicitudMapper.toResponse(any(Solicitud.class))).thenReturn(validResponse);

        webTestClient.post()
                .uri("/api/v1/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(validSolicitud.getId().toString())
                .jsonPath("$.documentoIdentidad").isEqualTo("123456789")
                .jsonPath("$.monto").isEqualTo(15000000)
                .jsonPath("$.plazo").isEqualTo(24)
                .jsonPath("$.tipoPrestamo.nombre").isEqualTo("PERSONAL");
    }

    @Test
    void registrarSolicitud_invalidLoanType_returnsBadRequest() {
        SolicitudCreateRequest invalidRequest = new SolicitudCreateRequest();
        invalidRequest.setDocumentoIdentidad("123456789");
        invalidRequest.setMonto(BigDecimal.valueOf(15000000));
        invalidRequest.setPlazo(24);
        invalidRequest.setTipoPrestamoNombre("INVALIDO");

        when(solicitudMapper.toDomain(any(SolicitudCreateRequest.class)))
                .thenReturn(Mono.error(new InvalidLoanTypeException("Tipo de préstamo inválido: INVALIDO")));

        webTestClient.post()
                .uri("/api/v1/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Tipo de préstamo inválido: INVALIDO");
    }

    @Test
    void registrarSolicitud_missingFields_returnsBadRequest() {
        SolicitudCreateRequest invalidRequest = new SolicitudCreateRequest();
        invalidRequest.setDocumentoIdentidad("123456789");

        when(solicitudMapper.toDomain(any(SolicitudCreateRequest.class)))
                .thenReturn(Mono.error(new SolicitudValidationException("El monto es obligatorio")));

        webTestClient.post()
                .uri("/api/v1/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("El monto es obligatorio");
    }

    @Test
    void registrarSolicitud_clientNotFound_returnsNotFound() {
        when(solicitudMapper.toDomain(any(SolicitudCreateRequest.class))).thenReturn(Mono.just(validSolicitud));
        when(solicitudUseCase.registrarSolicitud(any(Solicitud.class)))
                .thenReturn(Mono.error(new ClientNotFoundException("Cliente no encontrado con documento: 123456789")));

        webTestClient.post()
                .uri("/api/v1/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequest)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Cliente no encontrado con documento: 123456789");
    }

    @Test
    void registrarSolicitud_invalidAmountRange_returnsBadRequest() {
        SolicitudCreateRequest invalidRequest = new SolicitudCreateRequest();
        invalidRequest.setDocumentoIdentidad("123456789");
        invalidRequest.setMonto(BigDecimal.valueOf(500));
        invalidRequest.setPlazo(24);
        invalidRequest.setTipoPrestamoNombre("PERSONAL");

        Solicitud invalidSolicitud = Solicitud.builder()
                .id(UUID.randomUUID())
                .documentoIdentidad("123456789")
                .monto(BigDecimal.valueOf(500))
                .plazo(24)
                .tipoPrestamo(tipoPrestamo)
                .fechaCreacion(LocalDateTime.now())
                .build();

        when(solicitudMapper.toDomain(any(SolicitudCreateRequest.class))).thenReturn(Mono.just(invalidSolicitud));
        when(solicitudUseCase.registrarSolicitud(any(Solicitud.class)))
                .thenReturn(Mono.error(new SolicitudValidationException(
                        "El monto debe estar entre 1000000 y 50000000 para el tipo PERSONAL")));

        webTestClient.post()
                .uri("/api/v1/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("El monto debe estar entre 1000000 y 50000000 para el tipo PERSONAL");
    }
}