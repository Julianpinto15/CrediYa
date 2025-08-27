package co.com.pragma.r2dbc.repository;


import co.com.pragma.r2dbc.data.TipoPrestamoData;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;


public interface TipoPrestamoReactiveRepository extends ReactiveCrudRepository<TipoPrestamoData, UUID>, ReactiveQueryByExampleExecutor<TipoPrestamoData> {
    Mono<TipoPrestamoData> findByNombre(String nombre);
}
