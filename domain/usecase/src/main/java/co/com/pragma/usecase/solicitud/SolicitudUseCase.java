package co.com.pragma.usecase.solicitud;

import co.com.pragma.model.solicitud.Solicitud;
import co.com.pragma.model.solicitud.exceptions.ClientNotFoundException;
import co.com.pragma.model.solicitud.exceptions.InvalidLoanTypeException;
import co.com.pragma.model.solicitud.exceptions.SolicitudValidationException;
import co.com.pragma.model.solicitud.gateways.SolicitudRepository;
import co.com.pragma.model.solicitud.gateways.TipoPrestamoRepository;
import co.com.pragma.model.solicitud.gateways.EstadoSolicitudRepository;
import co.com.pragma.usecase.solicitud.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class SolicitudUseCase {

    private final SolicitudRepository solicitudRepository;
    private final TipoPrestamoRepository tipoPrestamoRepository;
    private final EstadoSolicitudRepository estadoSolicitudRepository;
    private final WebClient webClient;

    private static final BigDecimal MIN_MONTO = BigDecimal.ONE;
    private static final int MIN_PLAZO = 1;

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
        return validateSolicitud(solicitud)
                .then(verificarClienteExiste(solicitud))
                .flatMap(this::verificarTipoPrestamoValido)
                .flatMap(this::asignarEstadoInicial)
                .map(this::buildSolicitudWithDefaults)
                .flatMap(solicitudRepository::save);
    }

    private Mono<Void> validateSolicitud(Solicitud solicitud) {
        try {
            validators.forEach(validator -> validator.validate(solicitud));
            return Mono.empty();
        } catch (SolicitudValidationException e) {
            return Mono.error(e);
        }
    }

    private Solicitud buildSolicitudWithDefaults(Solicitud solicitud) {
        return solicitud.toBuilder()
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    private Mono<Solicitud> verificarClienteExiste(Solicitud solicitud) {
        return webClient.get()
                .uri("/api/v1/usuarios/exists/{documento}", solicitud.getDocumentoIdentidad())
                .retrieve()
                .bodyToMono(Boolean.class)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ClientNotFoundException("Cliente no encontrado con documento: " + solicitud.getDocumentoIdentidad()));
                    }
                    return Mono.just(solicitud);
                });
    }

    private Mono<Solicitud> verificarTipoPrestamoValido(Solicitud solicitud) {
        return tipoPrestamoRepository.findByNombre(solicitud.getTipoPrestamo().getNombre())
                .switchIfEmpty(Mono.error(new InvalidLoanTypeException("Tipo de préstamo inválido: " + solicitud.getTipoPrestamo().getNombre())))
                .thenReturn(solicitud);
    }

    private Mono<Solicitud> asignarEstadoInicial(Solicitud solicitud) {
        return estadoSolicitudRepository.findByNombre("Pendiente de revisión")
                .switchIfEmpty(Mono.error(new SolicitudValidationException("Estado inicial no encontrado")))
                .map(estado -> solicitud.toBuilder().estado(estado).build());
    }
}