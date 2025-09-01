package co.com.pragma.usecase.solicitud;

import co.com.pragma.model.solicitud.EstadoSolicitud;
import co.com.pragma.model.solicitud.Solicitud;
import co.com.pragma.model.solicitud.TipoPrestamo;
import co.com.pragma.model.solicitud.exceptions.ClientNotFoundException;
import co.com.pragma.model.solicitud.exceptions.SolicitudValidationException;
import co.com.pragma.model.solicitud.gateways.ClienteGateway;
import co.com.pragma.model.solicitud.gateways.EstadoSolicitudRepository;
import co.com.pragma.model.solicitud.gateways.SolicitudRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudUseCaseTest {

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private EstadoSolicitudRepository estadoSolicitudRepository;

    @Mock
    private ClienteGateway clienteGateway;

    private SolicitudUseCase solicitudUseCase;

    private TipoPrestamo tipoPrestamoDefault;
    private EstadoSolicitud estadoPendiente;
    private Solicitud solicitudBase;

    @BeforeEach
    void setUp() {
        solicitudUseCase = new SolicitudUseCase(
                solicitudRepository,
                estadoSolicitudRepository,
                clienteGateway
        );

        // Configurar TipoPrestamo por defecto
        tipoPrestamoDefault = TipoPrestamo.builder()
                .id(UUID.randomUUID())
                .nombre("Préstamo Personal")
                .montoMinimo(BigDecimal.valueOf(1000))
                .montoMaximo(BigDecimal.valueOf(50000))
                .build();

        // Configurar EstadoSolicitud por defecto
        estadoPendiente = EstadoSolicitud.builder()
                .id(UUID.randomUUID())
                .nombre("Pendiente de revisión")
                .build();

        // Configurar Solicitud base para tests
        solicitudBase = Solicitud.builder()
                .documentoIdentidad("12345678")
                .monto(BigDecimal.valueOf(5000))
                .plazo(12)
                .tipoPrestamo(tipoPrestamoDefault)
                .build();
    }

    @Test
    void registrarSolicitud_success() {
        // Arrange
        Solicitud solicitudEsperada = solicitudBase.toBuilder()
                .id(UUID.randomUUID())
                .estado(estadoPendiente)
                .fechaCreacion(LocalDateTime.now())
                .build();

        when(clienteGateway.existsByDocumento(anyString())).thenReturn(Mono.just(true));
        when(estadoSolicitudRepository.findByNombre("Pendiente de revisión")).thenReturn(Mono.just(estadoPendiente));
        when(solicitudRepository.save(any(Solicitud.class))).thenReturn(Mono.just(solicitudEsperada));

        // Act & Assert
        StepVerifier.create(solicitudUseCase.registrarSolicitud(solicitudBase))
                .expectNextMatches(solicitud ->
                        solicitud.getDocumentoIdentidad().equals("12345678") &&
                                solicitud.getMonto().equals(BigDecimal.valueOf(5000)) &&
                                solicitud.getPlazo().equals(12) &&
                                solicitud.getEstado().getNombre().equals("Pendiente de revisión") &&
                                solicitud.getFechaCreacion() != null
                )
                .verifyComplete();
    }

    @Test
    void registrarSolicitud_invalidMontoBelowMinimum() {
        // Arrange
        Solicitud solicitudInvalida = solicitudBase.toBuilder()
                .monto(BigDecimal.valueOf(500)) // Menor al mínimo
                .build();

        when(clienteGateway.existsByDocumento(anyString())).thenReturn(Mono.just(true));

        // Act & Assert
        StepVerifier.create(solicitudUseCase.registrarSolicitud(solicitudInvalida))
                .expectErrorMatches(throwable ->
                        throwable instanceof SolicitudValidationException &&
                                throwable.getMessage().contains("debe estar entre")
                )
                .verify();
    }

    @Test
    void registrarSolicitud_invalidMontoAboveMaximum() {
        // Arrange
        Solicitud solicitudInvalida = solicitudBase.toBuilder()
                .monto(BigDecimal.valueOf(100000)) // Mayor al máximo
                .build();

        when(clienteGateway.existsByDocumento(anyString())).thenReturn(Mono.just(true));

        // Act & Assert
        StepVerifier.create(solicitudUseCase.registrarSolicitud(solicitudInvalida))
                .expectErrorMatches(throwable ->
                        throwable instanceof SolicitudValidationException &&
                                throwable.getMessage().contains("debe estar entre")
                )
                .verify();
    }

    @Test
    void registrarSolicitud_documentoIdentidadEmpty() {
        // Arrange
        Solicitud solicitudInvalida = solicitudBase.toBuilder()
                .documentoIdentidad("") // Vacío
                .build();

        // Act & Assert
        StepVerifier.create(
                        Mono.fromCallable(() -> solicitudUseCase.registrarSolicitud(solicitudInvalida))
                                .flatMap(mono -> mono)
                )
                .expectErrorMatches(throwable ->
                        throwable instanceof SolicitudValidationException &&
                                throwable.getMessage().contains("documento de identidad es obligatorio")
                )
                .verify();
    }

    @Test
    void registrarSolicitud_montoZero() {
        // Arrange
        Solicitud solicitudInvalida = solicitudBase.toBuilder()
                .monto(BigDecimal.ZERO)
                .build();

        // Act & Assert
        StepVerifier.create(
                        Mono.fromCallable(() -> solicitudUseCase.registrarSolicitud(solicitudInvalida))
                                .flatMap(mono -> mono)
                )
                .expectErrorMatches(throwable ->
                        throwable instanceof SolicitudValidationException &&
                                throwable.getMessage().contains("monto debe ser mayor a 0")
                )
                .verify();
    }

    @Test
    void registrarSolicitud_plazoZero() {
        // Arrange
        Solicitud solicitudInvalida = solicitudBase.toBuilder()
                .plazo(0)
                .build();

        // Act & Assert
        StepVerifier.create(
                        Mono.fromCallable(() -> solicitudUseCase.registrarSolicitud(solicitudInvalida))
                                .flatMap(mono -> mono)
                )
                .expectErrorMatches(throwable ->
                        throwable instanceof SolicitudValidationException &&
                                throwable.getMessage().contains("plazo debe ser mayor a 0")
                )
                .verify();
    }

    @Test
    void registrarSolicitud_tipoPrestamoNull() {
        // Arrange
        Solicitud solicitudInvalida = solicitudBase.toBuilder()
                .tipoPrestamo(null)
                .build();

        // Act & Assert
        StepVerifier.create(
                        Mono.fromCallable(() -> solicitudUseCase.registrarSolicitud(solicitudInvalida))
                                .flatMap(mono -> mono)
                )
                .expectErrorMatches(throwable ->
                        throwable instanceof SolicitudValidationException &&
                                throwable.getMessage().contains("tipo de préstamo es obligatorio")
                )
                .verify();
    }

    @Test
    void registrarSolicitud_clienteNoEncontrado() {
        // Arrange
        when(clienteGateway.existsByDocumento(anyString())).thenReturn(Mono.just(false));

        // Act & Assert
        StepVerifier.create(solicitudUseCase.registrarSolicitud(solicitudBase))
                .expectErrorMatches(throwable ->
                        throwable instanceof ClientNotFoundException &&
                                throwable.getMessage().contains("Cliente no encontrado")
                )
                .verify();
    }

    @Test
    void registrarSolicitud_estadoInicialNoEncontrado() {
        // Arrange
        when(clienteGateway.existsByDocumento(anyString())).thenReturn(Mono.just(true));
        when(estadoSolicitudRepository.findByNombre("Pendiente de revisión")).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(solicitudUseCase.registrarSolicitud(solicitudBase))
                .expectErrorMatches(throwable ->
                        throwable instanceof SolicitudValidationException &&
                                throwable.getMessage().contains("Estado inicial") &&
                                throwable.getMessage().contains("no encontrado")
                )
                .verify();
    }
}