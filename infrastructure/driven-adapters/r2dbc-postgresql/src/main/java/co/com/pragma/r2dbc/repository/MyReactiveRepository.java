package co.com.pragma.r2dbc.repository;

import co.com.pragma.r2dbc.data.UserData;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface MyReactiveRepository extends ReactiveCrudRepository<UserData, String>, ReactiveQueryByExampleExecutor<UserData> {
    Mono<Boolean> existsByCorreoElectronico(String correoElectronico);
    Mono<UserData> findByDocumentoIdentidad(String documentoIdentidad);

    // Nuevos métodos para autenticación
    Mono<UserData> findByCorreoElectronico(String correoElectronico);

    @Query("SELECT * FROM usuarios WHERE correo_electronico = :correo AND password IS NOT NULL")
    Mono<UserData> findByCorreoElectronicoForAuth(String correo);
}
