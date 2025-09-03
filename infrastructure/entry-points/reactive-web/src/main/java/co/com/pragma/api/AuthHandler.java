package co.com.pragma.api;

import co.com.pragma.api.dto.LoginRequest;
import co.com.pragma.model.user.Login;
import co.com.pragma.model.user.exceptions.InvalidCredentialsException;
import co.com.pragma.usecase.user.AuthUseCase;
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
public class AuthHandler {

    private final AuthUseCase authUseCase;

    public Mono<ServerResponse> login(ServerRequest request) {
        log.debug("Recibiendo solicitud de login");

        return request.bodyToMono(LoginRequest.class)
                .doOnNext(loginRequest -> log.debug("Intento de login para: {}", loginRequest.getCorreoElectronico()))
                .map(this::convertToLogin)
                .flatMap(authUseCase::login)
                .flatMap(authResponse -> {
                    log.info("Login exitoso para usuario: {}", authResponse.getUsuario().getCorreoElectronico());
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(authResponse);
                })
                .onErrorResume(InvalidCredentialsException.class, e -> {
                    log.warn("Intento de login fallido: {}", e.getMessage());
                    return ServerResponse.status(401)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(createErrorResponse("Credenciales inválidas"));
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error inesperado en login: {}", e.getMessage(), e);
                    return ServerResponse.status(500)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(createErrorResponse("Error interno del servidor"));
                });
    }

    private Login convertToLogin(LoginRequest loginRequest) {
        return Login.builder()
                .correoElectronico(loginRequest.getCorreoElectronico())
                .password(loginRequest.getPassword())
                .build();
    }

    private ErrorResponse createErrorResponse(String message) {
        return new ErrorResponse(message, System.currentTimeMillis());
    }

    public record ErrorResponse(String message, long timestamp) {}
}