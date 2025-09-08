package co.com.pragma.model.solicitud.gateways;

import reactor.core.publisher.Mono;

public interface ClienteGateway {
    Mono<Boolean> existsByDocumento(String documento);

}
