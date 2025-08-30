package co.com.pragma.api;

import co.com.pragma.api.dto.SolicitudResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import co.com.pragma.model.solicitud.Solicitud;

@Configuration
@Tag(name = "CrediYa API", description = "Endpoints para gestión de usuarios y solicitudes de préstamo")
public class RouterRest {

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/v1/usuarios",
                    method = RequestMethod.POST,
                    operation = @Operation(
                            operationId = "registrarUsuario",
                            summary = "Registrar un nuevo usuario",
                            description = "Permite registrar un solicitante con sus datos personales",
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
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Datos inválidos - Error de validación en los campos enviados"
                                    ),
                                    @ApiResponse(
                                            responseCode = "409",
                                            description = "Correo ya registrado - El email ya existe en el sistema"
                                    )
                            }
                    )
            ),
            @RouterOperation(
                    path = "/api/v1/usuarios/exists/{cedula}",
                    method = RequestMethod.GET,
                    operation = @Operation(
                            operationId = "verificarExistenciaUsuario",
                            summary = "Verificar si existe un usuario por cédula",
                            description = "Verifica si ya existe un usuario registrado con el número de documento proporcionado",
                            tags = {"Usuarios"},
                            responses = {
                                    @ApiResponse(
                                            responseCode = "200",
                                            description = "Consulta exitosa",
                                            content = @Content(
                                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                                    schema = @Schema(implementation = Boolean.class)
                                            )
                                    ),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Parámetro inválido"
                                    )
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> userRoutes(UserHandler handler) {
        return RouterFunctions
                .route()
                .path("/api/v1/usuarios", builder -> builder
                        .POST("", handler::registrarUsuario)
                        .GET("/exists/{documento}", handler::existsByDocument)
                )
                .build();
    }

    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/v1/solicitudes",
                    method = RequestMethod.POST,
                    operation = @Operation(
                            operationId = "registrarSolicitud",
                            summary = "Registrar una nueva solicitud de préstamo",
                            description = "Permite a un cliente registrado enviar su solicitud de préstamo",
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
                                    @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> solicitudRoutes(SolicitudHandler handler) {
        return RouterFunctions
                .route()
                .path("/api/v1/solicitudes", builder -> builder
                        .POST("", handler::registrarSolicitud)
                )
                .build();
    }
}