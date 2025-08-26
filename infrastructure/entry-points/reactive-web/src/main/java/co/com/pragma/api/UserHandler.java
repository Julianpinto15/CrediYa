package co.com.pragma.api;

import co.com.pragma.model.user.User;
import co.com.pragma.model.user.exceptions.EmailAlreadyExistsException;
import co.com.pragma.model.user.exceptions.UserValidationException;
import co.com.pragma.usecase.user.UserUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserHandler {

private final UserUseCase userUseCase;

    public Mono<ServerResponse> registrarUsuario(ServerRequest request) {
        log.debug("Recibiendo solicitud de registro de usuario");
        return request.bodyToMono(User.class)
                .flatMap(userUseCase::save) // save devuelve Mono<User>
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .doOnError(e -> log.error("Error en handler: {}", e.getMessage(), e));
    }


}
