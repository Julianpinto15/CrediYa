package co.com.pragma.model.solicitud.gateways;

import co.com.pragma.model.solicitud.TipoPrestamo;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TipoPrestamoRepository {
    Mono<TipoPrestamo> findByNombre(String nombre);
    Mono<TipoPrestamo> findById(UUID id);
}
