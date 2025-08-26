package co.com.pragma.usecase.solicitud;

import co.com.pragma.model.solicitud.Solicitud;
import co.com.pragma.model.solicitud.exceptions.ClientNotFoundException;
import co.com.pragma.model.solicitud.exceptions.InvalidLoanTypeException;
import co.com.pragma.model.solicitud.exceptions.SolicitudValidationException;
import co.com.pragma.model.solicitud.gateways.SolicitudRepository;
import co.com.pragma.model.user.gateways.UserRepository;
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
            s -> validateNotNullOrEmpty(s.getDocumentoIdentidad(), "El documento de identidad es obligatorio"),
            s -> validateMonto(s.getMonto()),
            s -> validatePlazo(s.getPlazo()),
            s -> validateNotNullOrEmpty(s.getTipoPrestamo(), "El tipo de préstamo es obligatorio")
    );

    public Mono<Solicitud> registrarSolicitud(Solicitud solicitud) {
        return validateSolicitud(solicitud)
                .then(verificarClienteExiste(solicitud))
                .flatMap(this::verificarTipoPrestamo)
                .map(s -> s.toBuilder()
                        .estado("Pendiente de revisión")
                        .fechaCreacion(LocalDateTime.now())
                        .build())
                .flatMap(solicitudRepository::save);
    }

    private Mono<Void> validateSolicitud(Solicitud solicitud) {
        return Mono.fromRunnable(() -> validators.forEach(v -> v.validate(solicitud)));
    }

    private static void validateNotNullOrEmpty(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new SolicitudValidationException(message);
        }
    }

    private static void validateMonto(BigDecimal monto) {
        if (monto == null || monto.compareTo(MIN_MONTO) < 0) {
            throw new SolicitudValidationException("El monto debe ser mayor a 0");
        }
    }

    private static void validatePlazo(Integer plazo) {
        if (plazo == null || plazo < MIN_PLAZO) {
            throw new SolicitudValidationException("El plazo debe ser mayor a 0");
        }
    }

    private Mono<Solicitud> verificarClienteExiste(Solicitud solicitud) {
        return userRepository.existsByDocumentoIdentidad(solicitud.getDocumentoIdentidad())
                .switchIfEmpty(Mono.error(new ClientNotFoundException(
                        "Cliente no encontrado con documento: " + solicitud.getDocumentoIdentidad()
                )))
                .thenReturn(solicitud);
    }



    private Mono<Solicitud> verificarTipoPrestamo(Solicitud solicitud) {
        return solicitudRepository.isValidLoanType(solicitud.getTipoPrestamo())
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.error(new InvalidLoanTypeException("Tipo de préstamo inválido: " + solicitud.getTipoPrestamo()));
                    }
                    return Mono.just(solicitud);
                });
    }
}
