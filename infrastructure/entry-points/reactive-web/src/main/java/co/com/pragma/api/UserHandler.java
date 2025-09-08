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

    public Mono<ServerResponse> registrarCliente(ServerRequest request) {
        log.debug("Registro público de cliente");

        return request.bodyToMono(UserRequest.class)
                .map(userRequest -> {
                    // Forzamos rol CLIENTE, no dejamos que el JSON lo ponga
                    return convertToUser(userRequest).toBuilder()
                            .rol(User.Rol.CLIENTE)
                            .build();
                })
                .flatMap(userUseCase::save)
                .flatMap(user -> {
                    User userResponse = user.toBuilder().password(null).build();
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(userResponse);
                })
                .onErrorResume(EmailAlreadyExistsException.class, e ->
                        ServerResponse.status(409)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(createErrorResponse("El correo electrónico ya está registrado"))
                )
                .onErrorResume(UserValidationException.class, e ->
                        ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(createErrorResponse(e.getMessage()))
                )
                .onErrorResume(Exception.class, e ->
                        ServerResponse.status(500)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(createErrorResponse("Error interno del servidor"))
                );
    }


    public Mono<ServerResponse> registrarUsuario(ServerRequest request) {
        log.debug("Recibiendo solicitud de registro de usuario");

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
                            .doOnError(error -> log.error("Error al deserializar JSON: {}", error.getMessage(), error))
                            // 🔧 AGREGAR VALIDACIÓN MANUAL
                            .flatMap(this::validateUserRequest)
                            .map(this::convertToUser)
                            .flatMap(userUseCase::save)
                            .flatMap(user -> {
                                log.info("Usuario registrado exitosamente con ID: {} por {}",
                                        user.getId(), authenticatedUser.getCorreoElectronico());

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

    private Mono<UserRequest> validateUserRequest(UserRequest userRequest) {
        return Mono.fromCallable(() -> {
            log.debug("Validando UserRequest: {}", userRequest);

            // Validaciones básicas
            if (userRequest.getNombres() == null || userRequest.getNombres().trim().isEmpty()) {
                throw new UserValidationException("El campo nombres es obligatorio");
            }
            if (userRequest.getApellidos() == null || userRequest.getApellidos().trim().isEmpty()) {
                throw new UserValidationException("El campo apellidos es obligatorio");
            }
            if (userRequest.getCorreoElectronico() == null || userRequest.getCorreoElectronico().trim().isEmpty()) {
                throw new UserValidationException("El correo electrónico es obligatorio");
            }
            if (userRequest.getPassword() == null || userRequest.getPassword().trim().isEmpty()) {
                throw new UserValidationException("La contraseña es obligatoria");
            }
            if (userRequest.getRol() == null) {
                throw new UserValidationException("El rol es obligatorio");
            }
            if (userRequest.getSalarioBase() == null) {
                throw new UserValidationException("El salario base es obligatorio");
            }

            // Validación de email básica
            if (!userRequest.getCorreoElectronico().contains("@")) {
                throw new UserValidationException("El formato del correo electrónico no es válido");
            }

            log.debug("UserRequest validado correctamente");
            return userRequest;
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
        return Mono.deferContextual(contextView -> {
            if (contextView.hasKey("authenticatedUser")) {
                return Mono.just(contextView.get("authenticatedUser"));
            }
            return Mono.error(new RuntimeException("Usuario no autenticado"));
        });
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