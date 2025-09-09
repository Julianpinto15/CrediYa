package co.com.pragma.usecase.solicitud;

import co.com.pragma.model.solicitud.Solicitud;
import co.com.pragma.model.solicitud.exceptions.ClientNotFoundException;
import co.com.pragma.model.solicitud.exceptions.SolicitudValidationException;
import co.com.pragma.model.solicitud.gateways.ClienteGateway;
import co.com.pragma.model.solicitud.gateways.EstadoSolicitudRepository;
import co.com.pragma.model.solicitud.gateways.SolicitudRepository;
import co.com.pragma.usecase.solicitud.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class SolicitudUseCase {

    private final SolicitudRepository solicitudRepository;
    private final EstadoSolicitudRepository estadoSolicitudRepository;
    private final ClienteGateway clienteGateway;

    private static final BigDecimal MIN_MONTO = BigDecimal.ONE;
    private static final int MIN_PLAZO = 1;
    private static final String ESTADO_INICIAL = "Pendiente de revisión";

    @FunctionalInterface
    interface SolicitudValidator {
        void validate(Solicitud solicitud) throws SolicitudValidationException;
    }

    private final List<SolicitudValidator> validators = List.of(
            s -> ValidationUtils.validateNotNullOrEmpty(s.getDocumentoIdentidad(), "El documento de identidad es obligatorio"),
            s -> ValidationUtils.validateAmount(s.getMonto(), MIN_MONTO, "El monto debe ser mayor a 0"),
            s -> ValidationUtils.validateInteger(s.getPlazo(), MIN_PLAZO, "El plazo debe ser mayor a 0"),
            s -> ValidationUtils.validateNotNull(s.getTipoPrestamo(), "El tipo de préstamo es obligatorio")
    );

    public Mono<Solicitud> registrarSolicitud(Solicitud solicitud) {
        return Mono.fromCallable(() -> {
                    // Ejecutar validaciones síncronas dentro de un callable para manejo reactivo
                    validateSolicitudSync(solicitud);
                    return solicitud;
                })
                .flatMap(this::verificarClienteExiste)
                .flatMap(this::verificarRangosTipoPrestamo)
                .flatMap(this::asignarEstadoInicial)
                .map(this::asignarFechaCreacion)
                .flatMap(solicitudRepository::save);
    }

    private void validateSolicitudSync(Solicitud solicitud) throws SolicitudValidationException {
        validators.forEach(validator -> validator.validate(solicitud));
    }

    private Solicitud asignarFechaCreacion(Solicitud solicitud) {
        return solicitud.toBuilder()
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    private Mono<Solicitud> verificarClienteExiste(Solicitud solicitud) {
        return clienteGateway.existsByDocumento(solicitud.getDocumentoIdentidad())
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ClientNotFoundException("Cliente no encontrado con documento: " + solicitud.getDocumentoIdentidad()));
                    }
                    return Mono.just(solicitud);
                });
    }

    private Mono<Solicitud> verificarRangosTipoPrestamo(Solicitud solicitud) {
        if (solicitud.getMonto().compareTo(solicitud.getTipoPrestamo().getMontoMinimo()) < 0 ||
                solicitud.getMonto().compareTo(solicitud.getTipoPrestamo().getMontoMaximo()) > 0) {
            return Mono.error(new SolicitudValidationException(
                    "El monto debe estar entre " + solicitud.getTipoPrestamo().getMontoMinimo() +
                            " y " + solicitud.getTipoPrestamo().getMontoMaximo() + " para el tipo " + solicitud.getTipoPrestamo().getNombre()
            ));
        }
        return Mono.just(solicitud);
    }

    private Mono<Solicitud> asignarEstadoInicial(Solicitud solicitud) {
        return estadoSolicitudRepository.findByNombre(ESTADO_INICIAL)
                .switchIfEmpty(Mono.error(new SolicitudValidationException("Estado inicial '" + ESTADO_INICIAL + "' no encontrado")))
                .map(estado -> solicitud.toBuilder().estado(estado).build());
    }

    /**
     * Lista las solicitudes que requieren revisión manual por parte de un asesor.
     * Filtra por los estados: "Pendiente de revisión", "Rechazadas"
     */
    public Mono<List<Solicitud>> listarSolicitudesPendientes(int page, int size) {
        List<String> estadosPendientes = List.of(
                "Pendiente de revisión",
                "Rechazada"
        );

        // CORRECCIÓN: Pasar directamente los nombres de los estados, no convertir a UUID
        return solicitudRepository
                .findByEstadosWithPagination(estadosPendientes, page, size)
                .flatMap(this::agregarToListadoItem)
                .collectList()
                .defaultIfEmpty(Collections.emptyList());
    }

    /**
     * Cuenta el total de solicitudes pendientes para paginación
     */
    public Mono<Long> contarSolicitudesPendientes() {
        List<String> estadosPendientes = List.of(
                "Pendiente de revisión",
                "Rechazada"
        );

        // CORRECCIÓN: Pasar directamente los nombres de los estados
        return solicitudRepository.countByEstados(estadosPendientes);
    }

    /**
     * Enriquecer solicitud con datos del cliente
     */
    private Mono<Solicitud> agregarToListadoItem(Solicitud solicitud) {
        return clienteGateway.findByDocumento(solicitud.getDocumentoIdentidad())
                .map(cliente -> solicitud.toBuilder()
                        .emailCliente(cliente.getEmail())
                        .nombreCliente(cliente.getNombreCompleto())
                        .salarioCliente(cliente.getSalario())
                        .build())
                .switchIfEmpty(Mono.just(solicitud)); // Si no encuentra cliente, retorna solicitud original
    }



}