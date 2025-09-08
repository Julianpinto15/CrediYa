package co.com.pragma.model.solicitud.gateways;

import co.com.pragma.model.solicitud.EstadoSolicitud;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface EstadoSolicitudRepository {
    Mono<EstadoSolicitud> findByNombre(String nombre);
    Mono<EstadoSolicitud> findById(UUID id);
    // Nuevo método para encontrar múltiples estados por nombres
    Flux<EstadoSolicitud> findByNombres(List<String> nombres);
}