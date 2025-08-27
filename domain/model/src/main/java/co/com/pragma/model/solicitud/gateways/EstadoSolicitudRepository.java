package co.com.pragma.model.solicitud.gateways;

import co.com.pragma.model.solicitud.EstadoSolicitud;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface EstadoSolicitudRepository {
    Mono<EstadoSolicitud> findByNombre(String nombre);
    Mono<EstadoSolicitud> findById(UUID id);
}