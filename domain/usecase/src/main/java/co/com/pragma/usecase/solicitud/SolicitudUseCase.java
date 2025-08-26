package co.com.pragma.usecase.solicitud;

import co.com.pragma.model.solicitud.Solicitud;
import co.com.pragma.model.solicitud.TipoPrestamo;
import co.com.pragma.model.solicitud.exceptions.ClientNotFoundException;
import co.com.pragma.model.solicitud.exceptions.InvalidLoanTypeException;
import co.com.pragma.model.solicitud.exceptions.SolicitudValidationException;
import co.com.pragma.model.solicitud.gateways.SolicitudRepository;
import co.com.pragma.model.user.gateways.UserRepository;
import co.com.pragma.usecase.solicitud.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class SolicitudUseCase {

    private final SolicitudRepository solicitudRepository;
    private final UserRepository userRepository;

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
                .flatMap(this::verificarTipoPrestamo)
                .map(this::buildSolicitudWithDefaults)
                .flatMap(solicitudRepository::save);
    }

    private Mono<Void> validateSolicitud(Solicitud solicitud) {
        return Mono.fromRunnable(() ->
                validators.forEach(validator -> validator.validate(solicitud))
        );
    }

    private Solicitud buildSolicitudWithDefaults(Solicitud solicitud) {
        return solicitud.toBuilder()
                .estado("Pendiente de revisión")
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    private Mono<Solicitud> verificarClienteExiste(Solicitud solicitud) {
        return userRepository.existsByDocumentoIdentidad(solicitud.getDocumentoIdentidad())
                .switchIfEmpty(Mono.error(new ClientNotFoundException(
                        "Cliente no encontrado con documento: " + solicitud.getDocumentoIdentidad()
                )))
                .thenReturn(solicitud);
    }

    private Mono<Solicitud> verificarTipoPrestamo(Solicitud solicitud) {
        try {
            TipoPrestamo.valueOf(solicitud.getTipoPrestamo().name());
            return Mono.just(solicitud);
        } catch (IllegalArgumentException e) {
            return Mono.error(new InvalidLoanTypeException(
                    "Tipo de préstamo inválido: " + solicitud.getTipoPrestamo()));
        }
    }
}