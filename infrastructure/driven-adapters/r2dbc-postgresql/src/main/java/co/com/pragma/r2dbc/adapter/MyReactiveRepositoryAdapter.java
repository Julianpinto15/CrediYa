package co.com.pragma.r2dbc.adapter;

import co.com.pragma.model.user.User;
import co.com.pragma.model.user.gateways.UserRepository;
import co.com.pragma.r2dbc.data.UserData;
import co.com.pragma.r2dbc.helper.ReactiveAdapterOperations;
import co.com.pragma.r2dbc.repository.MyReactiveRepository;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
public class MyReactiveRepositoryAdapter extends ReactiveAdapterOperations<User,
        UserData,
        String,
        MyReactiveRepository
        > implements UserRepository {

    public MyReactiveRepositoryAdapter(MyReactiveRepository repository, ObjectMapper mapper) {
        // Como tu User tiene @Builder, puedes usar mapBuilder:
        super(repository, mapper, d -> mapper.mapBuilder(d, User.UserBuilder.class).build());
        // Si no usaras builder sería:
        // super(repository, mapper, d -> mapper.map(d, User.class));
    }

    @Override
    public Mono<Boolean> existsByCorreo(String correo) {
        log.debug("Verificando existencia de usuario por correo: {}", correo);
        return repository.existsByCorreoElectronico(correo)
                .doOnNext(exists -> log.debug("Usuario con correo {} existe: {}", correo, exists))
                .doOnError(error -> log.error("Error verificando existencia por correo {}: {}", correo, error.getMessage()));
    }

    @Override
    @Transactional
    public Mono<User> save(User user) {
        log.debug("Guardando usuario: {}", user.getCorreoElectronico());
        return super.save(user)
                .doOnSuccess(savedUser ->
                        log.info("Usuario guardado exitosamente con ID: {} y correo: {}",
                                savedUser.getId(), savedUser.getCorreoElectronico()))
                .doOnError(error ->
                        log.error("Error guardando usuario con correo {}: {}",
                                user.getCorreoElectronico(), error.getMessage()));
    }

    @Override
    public Mono<Boolean> existsByDocumentoIdentidad(String documentoIdentidad) {
        log.debug("Verificando existencia de usuario por documento: {}", documentoIdentidad);
        return repository.findByDocumentoIdentidad(documentoIdentidad)
                .doOnNext(userData -> log.debug("Usuario encontrado por documento {}: {}",
                        documentoIdentidad, userData.getCorreoElectronico()))
                .hasElement() // ← CLAVE: Esto retorna true si hay elemento, false si está vacío
                .doOnNext(exists -> log.debug("Usuario con documento {} existe: {}", documentoIdentidad, exists))
                .doOnError(error -> log.error("Error verificando existencia por documento {}: {}",
                        documentoIdentidad, error.getMessage()));
    }

}