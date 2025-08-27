package co.com.pragma.r2dbc.repository;

import co.com.pragma.r2dbc.data.EstadoSolicitudData;

import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface EstadoSolicitudReactiveRepository extends ReactiveCrudRepository<EstadoSolicitudData, UUID>, ReactiveQueryByExampleExecutor<EstadoSolicitudData> {
    Mono<EstadoSolicitudData> findByNombre(String nombre);
}
