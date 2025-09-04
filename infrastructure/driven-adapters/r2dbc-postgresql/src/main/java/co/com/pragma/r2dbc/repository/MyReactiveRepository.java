package co.com.pragma.r2dbc.repository;

import co.com.pragma.r2dbc.data.UserData;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface MyReactiveRepository extends ReactiveCrudRepository<UserData, Integer>,
        ReactiveQueryByExampleExecutor<UserData> {

    Mono<Boolean> existsByCorreoElectronico(@Param("correoElectronico") String correoElectronico);

    Mono<UserData> findByDocumentoIdentidad(@Param("documentoIdentidad") String documentoIdentidad);

    // Métodos para autenticación
    Mono<UserData> findByCorreoElectronico(@Param("correoElectronico") String correoElectronico);

    @Query("SELECT * FROM usuarios WHERE correo_electronico = :correo AND password IS NOT NULL")
    Mono<UserData> findByCorreoElectronicoForAuth(@Param("correo") String correo);
}