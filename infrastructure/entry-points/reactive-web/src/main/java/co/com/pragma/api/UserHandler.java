package co.com.pragma.api;

import co.com.pragma.api.dto.UserRequest;
import co.com.pragma.model.user.AuthenticatedUser;
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

        // Validar que el usuario autenticado tenga permisos (ADMIN/ASESOR)
        return getAuthenticatedUser(request)
                .flatMap(authenticatedUser -> {
                    if (!hasAdminOrAsesorRole(authenticatedUser)) {
                        log.warn("Usuario {} sin permisos intentó registrar usuario",
                                authenticatedUser.getCorreoElectronico());
                        return ServerResponse.status(403)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(createErrorResponse("No tiene permisos para registrar usuarios"));
                    }

                    return request.bodyToMono(UserRequest.class)
                            .doOnNext(userRequest -> log.debug("Datos recibidos: {}", userRequest))
                            .map(this::convertToUser)
                            .flatMap(userUseCase::save)
                            .flatMap(user -> {
                                log.info("Usuario registrado exitosamente con ID: {} por {}",
                                        user.getId(), authenticatedUser.getCorreoElectronico());

                                // No retornar la contraseña en la respuesta
                                User userResponse = user.toBuilder().password(null).build();

                                return ServerResponse.ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(userResponse);
                            });
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

    private Mono<AuthenticatedUser> getAuthenticatedUser(ServerRequest request) {
        return Mono.justOrEmpty(request.attribute("authenticatedUser"))
                .cast(AuthenticatedUser.class)
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no autenticado")));
    }

    private boolean hasAdminOrAsesorRole(AuthenticatedUser user) {
        return user.getRol() == User.Rol.ADMIN || user.getRol() == User.Rol.ASESOR;
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
                .password(userRequest.getPassword())
                .rol(userRequest.getRol())
                .build();
    }

    private ErrorResponse createErrorResponse(String message) {
        return new ErrorResponse(message, System.currentTimeMillis());
    }

    // Clases auxiliares para las respuestas
    public record ErrorResponse(String message, long timestamp) {}
    public record ExistsResponse(boolean exists) {}
}