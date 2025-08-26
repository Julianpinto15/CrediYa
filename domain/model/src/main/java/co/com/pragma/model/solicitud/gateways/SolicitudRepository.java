package co.com.pragma.model.solicitud.gateways;

import co.com.pragma.model.solicitud.Solicitud;
import reactor.core.publisher.Mono;

public interface SolicitudRepository {
    Mono<Solicitud> save(Solicitud solicitud);
    Mono<Boolean> existsByDocumentoIdentidad(String documentoIdentidad);
    Mono<Boolean> isValidLoanType(String tipoPrestamo);
}
