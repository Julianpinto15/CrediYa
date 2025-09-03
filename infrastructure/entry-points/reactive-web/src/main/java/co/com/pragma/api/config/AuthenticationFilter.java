package co.com.pragma.api.config;

import co.com.pragma.model.user.User;
import co.com.pragma.model.user.exceptions.InvalidTokenException;
import co.com.pragma.model.user.exceptions.UnauthorizedException;
import co.com.pragma.usecase.user.AuthUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private final AuthUseCase authUseCase;

    // Endpoints que no requieren autenticación
    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
            "/api/v1/login"
    );

    // Configuración de roles por endpoint
    private static final String REGISTER_USER_PATH = "/api/v1/usuarios";
    private static final String CREATE_SOLICITUD_PATH = "/api/v1/solicitud";

    @Override
    public Mono<ServerResponse> filter(ServerRequest request, HandlerFunction<ServerResponse> next) {
        String path = request.path();
        String method = request.methodName();

        log.debug("Procesando request: {} {}", method, path);

        // Permitir acceso a endpoints públicos
        if (PUBLIC_ENDPOINTS.contains(path)) {
            log.debug("Endpoint público, permitiendo acceso: {}", path);
            return next.handle(request);
        }

        // Extraer y validar token
        return extractToken(request)
                .flatMap(authUseCase::validateToken)
                .flatMap(authenticatedUser -> {
                    // Validar permisos específicos por endpoint
                    return validateEndpointPermissions(request, authenticatedUser)
                            .flatMap(hasPermission -> {
                                if (Boolean.TRUE.equals(hasPermission)) {
                                    // Agregar información del usuario autenticado al contexto
                                    ServerRequest enrichedRequest = ServerRequest.from(request)
                                            .attribute("authenticatedUser", authenticatedUser)
                                            .build();
                                    return next.handle(enrichedRequest);
                                } else {
                                    return createForbiddenResponse("No tiene permisos para acceder a este recurso");
                                }
                            });
                })
                .onErrorResume(InvalidTokenException.class, e -> {
                    log.warn("Token inválido en request a {}: {}", path, e.getMessage());
                    return createUnauthorizedResponse("Token inválido o expirado");
                })
                .onErrorResume(UnauthorizedException.class, e -> {
                    log.warn("Usuario no autorizado para {}: {}", path, e.getMessage());
                    return createForbiddenResponse(e.getMessage());
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error inesperado en filtro de autenticación: {}", e.getMessage(), e);
                    return createUnauthorizedResponse("Error de autenticación");
                });
    }

    private Mono<String> extractToken(ServerRequest request) {
        return Mono.fromCallable(() -> {
            List<String> authHeaders = request.headers().header(HttpHeaders.AUTHORIZATION);

            if (authHeaders.isEmpty()) {
                throw new InvalidTokenException("Token de autorización requerido");
            }

            String authHeader = authHeaders.get(0);
            if (!authHeader.startsWith("Bearer ")) {
                throw new InvalidTokenException("Formato de token inválido");
            }

            return authHeader.substring(7);
        });
    }

    private Mono<Boolean> validateEndpointPermissions(ServerRequest request,
                                                      co.com.pragma.model.user.AuthenticatedUser authenticatedUser) {
        String path = request.path();
        String method = request.methodName();
        User.Rol userRole = authenticatedUser.getRol();

        log.debug("Validando permisos para {} {} con rol {}", method, path, userRole);

        return Mono.fromCallable(() -> {
            // Registro de usuarios: solo admin/asesor
            if (REGISTER_USER_PATH.equals(path) && "POST".equals(method)) {
                return userRole == User.Rol.ADMIN || userRole == User.Rol.ASESOR;
            }

            // Crear solicitud de préstamo: solo cliente
            if (CREATE_SOLICITUD_PATH.equals(path) && "POST".equals(method)) {
                return userRole == User.Rol.CLIENTE;
            }

            // Listado de solicitudes: solo asesor
            if (CREATE_SOLICITUD_PATH.equals(path) && "GET".equals(method)) {
                return userRole == User.Rol.ASESOR;
            }

            // Por defecto, denegar acceso a endpoints no configurados
            log.warn("Endpoint no configurado en filtro de autenticación: {} {}", method, path);
            return false;
        });
    }

    private Mono<ServerResponse> createUnauthorizedResponse(String message) {
        return ServerResponse.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ErrorResponse(message, System.currentTimeMillis()));
    }

    private Mono<ServerResponse> createForbiddenResponse(String message) {
        return ServerResponse.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ErrorResponse(message, System.currentTimeMillis()));
    }

    public record ErrorResponse(String message, long timestamp) {}
}