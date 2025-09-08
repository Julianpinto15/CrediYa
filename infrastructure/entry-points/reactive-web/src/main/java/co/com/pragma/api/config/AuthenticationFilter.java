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

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private final AuthUseCase authUseCase;

    @Override
    public Mono<ServerResponse> filter(ServerRequest request, HandlerFunction<ServerResponse> next) {
        String path = request.path();
        String method = request.methodName();

        log.debug("🔒 Procesando request protegida: {} {}", method, path);

        return extractToken(request)
                .doOnNext(token -> log.debug("🎫 Token extraído: {}...", token.substring(0, Math.min(token.length(), 20))))
                .flatMap(authUseCase::validateToken)
                .doOnNext(authenticatedUser -> log.debug("👤 Usuario autenticado: {} con rol: {}",
                        authenticatedUser.getCorreoElectronico(), authenticatedUser.getRol()))
                .flatMap(authenticatedUser -> {
            return validateEndpointPermissions(request, authenticatedUser)
                    .flatMap(hasPermission -> {
                        if (Boolean.TRUE.equals(hasPermission)) {
                            log.debug("✅ Acceso permitido para usuario: {}", authenticatedUser.getCorreoElectronico());
                            // 🔧 Usar contextWrite en lugar de attributes
                            return next.handle(request)
                                    .contextWrite(context -> context.put("authenticatedUser", authenticatedUser));
                        } else {
                            log.warn("❌ Acceso denegado para usuario {} en {} {}",
                                    authenticatedUser.getCorreoElectronico(), method, path);
                            return createForbiddenResponse("No tiene permisos para acceder a este recurso");
                        }
                    });
        })
                .onErrorResume(InvalidTokenException.class, e -> {
                    log.warn("🚫 Token inválido en request a {}: {}", path, e.getMessage());
                    return createUnauthorizedResponse("Token inválido o expirado");
                })
                .onErrorResume(UnauthorizedException.class, e -> {
                    log.warn("🚫 Usuario no autorizado para {}: {}", path, e.getMessage());
                    return createForbiddenResponse(e.getMessage());
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("💥 Error inesperado en filtro de autenticación para {}: {}", path, e.getMessage(), e);
                    return createUnauthorizedResponse("Error de autenticación");
                });
    }

    private Mono<String> extractToken(ServerRequest request) {
        return Mono.fromCallable(() -> {
            List<String> authHeaders = request.headers().header(HttpHeaders.AUTHORIZATION);

            if (authHeaders.isEmpty()) {
                log.warn("📝 Request sin header Authorization");
                throw new InvalidTokenException("Token de autorización requerido");
            }

            String authHeader = authHeaders.get(0);
            if (!authHeader.startsWith("Bearer ")) {
                log.warn("📝 Header Authorization con formato incorrecto: {}", authHeader);
                throw new InvalidTokenException("Formato de token inválido. Use: Bearer <token>");
            }

            return authHeader.substring(7);
        });
    }

    private Mono<Boolean> validateEndpointPermissions(ServerRequest request,
                                                      co.com.pragma.model.user.AuthenticatedUser authenticatedUser) {
        String path = request.path();
        String method = request.methodName();
        User.Rol userRole = authenticatedUser.getRol();

        log.debug("🔐 Validando permisos para {} {} con rol {}", method, path, userRole);

        return Mono.fromCallable(() -> {
            // POST /api/v1/usuarios - Solo ADMIN o ASESOR pueden crear usuarios
            if ("/api/v1/usuarios".equals(path) && "POST".equals(method)) {
                boolean hasPermission = userRole == User.Rol.ADMIN || userRole == User.Rol.ASESOR;
                log.debug("👥 Crear usuario: rol {} tiene permiso: {}", userRole, hasPermission);
                return hasPermission;
            }

            // GET /api/v1/usuarios/exists/{documento} - Todos los autenticados pueden verificar
            if (path.startsWith("/api/v1/usuarios/exists/") && "GET".equals(method)) {
                log.debug("🔍 Verificar usuario: permitido para todos los autenticados");
                return true;
            }

            // POST /api/v1/solicitudes - Solo CLIENTE puede crear solicitudes
            if ("/api/v1/solicitudes".equals(path) && "POST".equals(method)) {
                boolean hasPermission = userRole == User.Rol.CLIENTE;
                log.debug("📋 Crear solicitud: rol {} tiene permiso: {}", userRole, hasPermission);
                return hasPermission;
            }

            // GET /api/v1/solicitudes - Solo ASESOR puede listar solicitudes
            if ("/api/v1/solicitudes".equals(path) && "GET".equals(method)) {
                boolean hasPermission = userRole == User.Rol.ASESOR;
                log.debug("📋 Listar solicitudes: rol {} tiene permiso: {}", userRole, hasPermission);
                return hasPermission;
            }

            // Para cualquier otro endpoint, denegar por defecto (más seguro)
            log.warn("⚠️ Endpoint no configurado en filtro de autenticación: {} {}", method, path);
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