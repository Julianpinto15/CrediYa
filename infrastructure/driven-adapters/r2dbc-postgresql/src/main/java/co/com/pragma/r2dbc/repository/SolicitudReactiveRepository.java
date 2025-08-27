package co.com.pragma.r2dbc.repository;

import co.com.pragma.r2dbc.data.SolicitudData;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SolicitudReactiveRepository extends ReactiveCrudRepository<SolicitudData, UUID>, ReactiveQueryByExampleExecutor<SolicitudData> {
    Mono<Boolean> existsByDocumentoIdentidad(String documentoIdentidad);
}

