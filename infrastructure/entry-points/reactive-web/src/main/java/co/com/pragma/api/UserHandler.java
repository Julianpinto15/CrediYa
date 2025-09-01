package co.com.pragma.api;

import co.com.pragma.api.dto.UserRequest;
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
        return request.bodyToMono(UserRequest.class)
                .doOnNext(userRequest -> log.debug("Datos recibidos: {}", userRequest))
                .map(this::convertToUser)
                .flatMap(userUseCase::save)
                .flatMap(user -> {
                    log.info("Usuario registrado exitosamente con ID: {}", user.getId());
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(user);
                })
                .onErrorResume(EmailAlreadyExistsException.class, e -> {
                    log.warn("Intento de registro con email duplicado: {}", e.getMessage());
                    return ServerResponse.status(409)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(createErrorResponse("El correo electrónico ya está registrado"));
                })
                .onErrorResume(UserValidationException.class, e -> {
                    log.warn("Error de validación en registro: {}", e.getMessage());
                    return ServerResponse.badRequest()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(createErrorResponse(e.getMessage()));
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error inesperado en registro de usuario: {}", e.getMessage(), e);
                    return ServerResponse.status(500)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(createErrorResponse("Error interno del servidor"));
                });
    }


    public Mono<ServerResponse> existsByDocument(ServerRequest request) {
        String documento = request.pathVariable("documento");
        log.debug("Verificando existencia de usuario con documento: {}", documento);

        return userUseCase.existsByDocumento(documento)
                .flatMap(exists -> {
                    log.debug("Usuario con documento {} existe: {}", documento, exists);
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(new ExistsResponse(exists));
                })
                .onErrorResume(e -> {
                    log.error("Error al verificar existencia de usuario: {}", e.getMessage(), e);
                    return ServerResponse.badRequest()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(createErrorResponse("Error al verificar el documento"));
                });
    }


    private User convertToUser(UserRequest userRequest) {
        return User.builder()
                .nombres(userRequest.getNombres())
                .apellidos(userRequest.getApellidos())
                .documentoIdentidad(userRequest.getDocumentoIdentidad())
                .fechaNacimiento(userRequest.getFechaNacimiento())
                .direccion(userRequest.getDireccion())
                .telefono(userRequest.getTelefono())
                .correoElectronico(userRequest.getCorreoElectronico())
                .salarioBase(userRequest.getSalarioBase())
                .build();
    }

    private ErrorResponse createErrorResponse(String message) {
        return new ErrorResponse(message, System.currentTimeMillis());
    }

    // Clases auxiliares para las respuestas
    public record ErrorResponse(String message, long timestamp) {}
    public record ExistsResponse(boolean exists) {}

}
