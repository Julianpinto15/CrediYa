package co.com.pragma.r2dbc;

import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface MyReactiveRepository extends ReactiveCrudRepository<UserData, String>, ReactiveQueryByExampleExecutor<UserData> {
    Mono<Boolean> existsByCorreoElectronico(String correoElectronico);
    Mono<UserData> findByDocumentoIdentidad(String documentoIdentidad);
}
