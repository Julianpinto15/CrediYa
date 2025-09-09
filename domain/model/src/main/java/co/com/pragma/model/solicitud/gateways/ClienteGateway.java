package co.com.pragma.model.solicitud.gateways;

import co.com.pragma.model.solicitud.ClienteCompleto;
import reactor.core.publisher.Mono;

public interface ClienteGateway {
    Mono<Boolean> existsByDocumento(String documento);
    // Nuevo método para obtener datos completos del cliente
    Mono<ClienteCompleto> findByDocumento(String documento);

}
