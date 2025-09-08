package co.com.pragma.api;

import co.com.pragma.api.config.AuthenticationFilter;
import co.com.pragma.api.dto.SolicitudResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import co.com.pragma.api.dto.UserRequest;
import co.com.pragma.api.dto.SolicitudCreateRequest;
import co.com.pragma.model.user.User;

@Configuration
@RequiredArgsConstructor
@Tag(name = "CrediYa API", description = "Endpoints para gestión de usuarios y solicitudes de préstamo")
public class RouterRest {

    private final AuthenticationFilter authenticationFilter;
    private final UserHandler userHandler;
    private final SolicitudHandler solicitudHandler;
    private final AuthHandler authHandler;

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/v1/login",
                    method = RequestMethod.POST,
                    operation = @Operation(
                            operationId = "login",
                            summary = "Autenticación de usuario",
                            description = "Permite a un usuario autenticarse con correo y contraseña. Devuelve un JWT si es exitoso.",
                            tags = {"Auth"},
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Login exitoso"),
                                    @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/clientes/register",
                    method = RequestMethod.POST,
                    operation = @Operation(
                            operationId = "registrarCliente",
                            summary = "Registro público de clientes",
                            description = "Permite a un cliente crear su cuenta en el sistema. Siempre tendrá rol CLIENTE.",
                            tags = {"Clientes"},
                            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                    required = true,
                                    description = "Datos básicos del cliente a registrar",
                                    content = @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = UserRequest.class)
                                    )
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Cliente registrado exitosamente",
                                            content = @Content(
                                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                    schema = @Schema(implementation = User.class)
                                            )
                                    ),
                                    @ApiResponse(responseCode = "400", description = "Datos inválidos"),
                                    @ApiResponse(responseCode = "409", description = "Correo ya registrado")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> publicRoutes() {
        return RouterFunctions
                .route()
                // 🟢 RUTAS PÚBLICAS - SIN AUTENTICACIÓN
                .POST("/api/v1/login", authHandler::login)
                .POST("/api/v1/clientes/register", userHandler::registrarCliente)
                .build();
    }

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/v1/usuarios",
                    method = RequestMethod.POST,
                    operation = @Operation(
                            operationId = "registrarUsuario",
                            summary = "Registrar un nuevo usuario",
                            description = "Permite registrar un usuario con sus datos personales. Requiere rol ADMIN o ASESOR.",
                            tags = {"Usuarios"},
                            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                    required = true,
                                    description = "Datos del usuario a registrar",
                                    content = @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = UserRequest.class)
                                    )
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Usuario registrado exitosamente",
                                            content = @Content(
                                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                    schema = @Schema(implementation = User.class)
                                            )
                                    ),
                                    @ApiResponse(responseCode = "400", description = "Datos inválidos"),
                                    @ApiResponse(responseCode = "401", description = "Token requerido"),
                                    @ApiResponse(responseCode = "403", description = "Sin permisos - Solo ADMIN/ASESOR"),
                                    @ApiResponse(responseCode = "409", description = "Correo ya registrado")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/usuarios/exists/{documento}",
                    method = RequestMethod.GET,
                    operation = @Operation(
                            operationId = "verificarExistenciaUsuario",
                            summary = "Verificar si existe un usuario por documento",
                            description = "Verifica si ya existe un usuario registrado con el número de documento proporcionado",
                            tags = {"Usuarios"},
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
                                    @ApiResponse(responseCode = "401", description = "Token requerido"),
                                    @ApiResponse(responseCode = "400", description = "Parámetro inválido")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/solicitudes",
                    method = RequestMethod.POST,
                    operation = @Operation(
                            operationId = "registrarSolicitud",
                            summary = "Registrar una nueva solicitud de préstamo",
                            description = "Permite a un cliente registrado enviar su solicitud de préstamo. Requiere rol CLIENTE.",
                            tags = {"Solicitudes"},
                            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                    required = true,
                                    description = "Datos de la solicitud de préstamo",
                                    content = @Content(
                                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = SolicitudCreateRequest.class)
                                    )
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Solicitud registrada exitosamente",
                                            content = @Content(
                                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                    schema = @Schema(implementation = SolicitudResponse.class)
                                            )
                                    ),
                                    @ApiResponse(responseCode = "400", description = "Datos inválidos"),
                                    @ApiResponse(responseCode = "401", description = "Token requerido"),
                                    @ApiResponse(responseCode = "403", description = "Sin permisos - Solo CLIENTE"),
                                    @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/solicitudes",
                    method = RequestMethod.GET,
                    operation = @Operation(
                            operationId = "listarSolicitudes",
                            summary = "Listar solicitudes de préstamo",
                            description = "Permite a un asesor ver todas las solicitudes. Requiere rol ASESOR.",
                            tags = {"Solicitudes"},
                            responses = {
                                    @ApiResponse(responseCode = "200", description = "Lista de solicitudes"),
                                    @ApiResponse(responseCode = "401", description = "Token requerido"),
                                    @ApiResponse(responseCode = "403", description = "Sin permisos - Solo ASESOR")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> protectedRoutes() {
        return RouterFunctions
                .route()
                // 🔒 RUTAS PROTEGIDAS - REQUIEREN AUTENTICACIÓN
                .path("/api/v1/usuarios", builder -> builder
                        .POST("", userHandler::registrarUsuario)           // Solo ADMIN/ASESOR
                        .GET("/exists/{documento}", userHandler::existsByDocument) // Cualquier autenticado
                )
                /*.path("/api/v1/solicitudes", builder -> builder
                        .POST("", solicitudHandler::registrarSolicitud)   // Solo CLIENTE
                        .GET("", solicitudHandler::listarSolicitudes)     // Solo ASESOR (agregar este método si no existe)
                )*/
                // ⚡ APLICAR FILTRO DE AUTENTICACIÓN A TODAS LAS RUTAS PROTEGIDAS
                .filter(authenticationFilter)
                .build();
    }

    // 🔗 Bean principal que combina rutas públicas y protegidas
    @Bean(name = "mainRouter")
    public RouterFunction<ServerResponse> mainRouter() {
        return publicRoutes().and(protectedRoutes());
    }
}