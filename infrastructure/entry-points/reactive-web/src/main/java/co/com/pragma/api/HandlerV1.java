package co.com.pragma.api;

import co.com.pragma.model.user.User;
import co.com.pragma.model.user.exceptions.EmailAlreadyExistsException;
import co.com.pragma.model.user.exceptions.UserValidationException;
import co.com.pragma.usecase.user.UserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class HandlerV1 {

private final UserUseCase userUseCase;

    public Mono<ServerResponse> registrarUsuario(ServerRequest request) {
        return request.bodyToMono(User.class)
                .doOnNext(user -> System.out.println("Usuario recibido: " + user))
                .flatMap(userUseCase::save)
                .doOnNext(user -> System.out.println("Usuario guardado: " + user))
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .onErrorResume(UserValidationException.class, e -> {
                    System.out.println("Error de validación: " + e.getMessage());
                    return ServerResponse.badRequest()
                            .bodyValue(Map.of("error", "Datos inválidos", "message", e.getMessage()));
                })
                .onErrorResume(EmailAlreadyExistsException.class, e -> {
                    System.out.println("Error email duplicado: " + e.getMessage());
                    return ServerResponse.badRequest()
                            .bodyValue(Map.of("error", "Correo duplicado", "message", e.getMessage()));
                })
                .onErrorResume(e -> {
                    System.out.println("Error general: " + e.getMessage());
                    e.printStackTrace();
                    return ServerResponse.status(500)
                            .bodyValue(Map.of("error", "Error interno del servidor"));
                });
    }
}
