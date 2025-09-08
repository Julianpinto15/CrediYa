package co.com.pragma.api;

import co.com.pragma.api.dto.SolicitudCreateRequest;
import co.com.pragma.api.dto.SolicitudListadoResponse;
import co.com.pragma.api.dto.SolicitudResponse;
import co.com.pragma.api.mapper.SolicitudMapper;
import co.com.pragma.model.solicitud.Solicitud;
import co.com.pragma.model.solicitud.TipoPrestamo;
import co.com.pragma.model.solicitud.gateways.TipoPrestamoRepository;
import co.com.pragma.model.user.AuthenticatedUser;
import co.com.pragma.model.user.User;
import co.com.pragma.usecase.solicitud.SolicitudUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;


@Component
@RequiredArgsConstructor
public class SolicitudHandler {

    private static final Logger log = LoggerFactory.getLogger(SolicitudHandler.class);
    private final SolicitudUseCase solicitudUseCase;
    private final TipoPrestamoRepository tipoPrestamoRepository;

    private final SolicitudMapper solicitudMapper;

    public Mono<ServerResponse> registrarSolicitud(ServerRequest request) {
        return request.bodyToMono(SolicitudCreateRequest.class)
                .doOnNext(dto -> log.debug("📩 Solicitud recibida: {}", dto))
                .flatMap(solicitudMapper::toDomain)  // Ahora retorna Mono<Solicitud>
                .flatMap(solicitudUseCase::registrarSolicitud)
                .map(solicitudMapper::toResponse)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .doOnSuccess(resp -> log.info("Solicitud registrada exitosamente"));
    }


    private Mono<Solicitud> buildSolicitudFromRequest(SolicitudCreateRequest request) {
        return resolveTipoPrestamo(request.getTipoPrestamoNombre())
                .map(tipoPrestamo -> Solicitud.builder()
                        .documentoIdentidad(request.getDocumentoIdentidad())
                        .monto(request.getMonto())
                        .plazo(request.getPlazo())
                        .tipoPrestamo(tipoPrestamo)
                        .build() // sin estado ni fecha
                );
    }


    private Mono<TipoPrestamo> resolveTipoPrestamo(String tipoPrestamoNombre) {
        return Mono.justOrEmpty(tipoPrestamoNombre)
                .flatMap(tipoPrestamoRepository::findByNombre)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Debe especificar un tipo de préstamo válido")));
    }


    private SolicitudResponse mapToResponse(Solicitud solicitud) {
        SolicitudResponse response = new SolicitudResponse();
        response.setId(solicitud.getId());
        response.setDocumentoIdentidad(solicitud.getDocumentoIdentidad());
        response.setMonto(solicitud.getMonto());
        response.setPlazo(solicitud.getPlazo());
        response.setFechaCreacion(solicitud.getFechaCreacion());

        if (solicitud.getTipoPrestamo() != null) {
            SolicitudResponse.TipoPrestamoResponse tp = new SolicitudResponse.TipoPrestamoResponse();
            tp.setId(solicitud.getTipoPrestamo().getId());
            tp.setNombre(solicitud.getTipoPrestamo().getNombre());
            response.setTipoPrestamo(tp);
        }

        if (solicitud.getEstado() != null) {
            SolicitudResponse.EstadoSolicitudResponse est = new SolicitudResponse.EstadoSolicitudResponse();
            est.setId(solicitud.getEstado().getId());
            est.setNombre(solicitud.getEstado().getNombre());
            response.setEstado(est);
        }

        return response;
    }

    public Mono<ServerResponse> listarSolicitudes(ServerRequest request) {
        log.debug("Listando solicitudes para revisión manual");

        return getAuthenticatedUser(request)
                .flatMap(authenticatedUser -> {
                    // Validar que sea ASESOR (ya se valida en el filtro, pero por seguridad)
                    if (authenticatedUser.getRol() != User.Rol.ASESOR) {
                        log.warn("Usuario {} sin permisos de asesor intentó listar solicitudes",
                                authenticatedUser.getCorreoElectronico());
                        return ServerResponse.status(403)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(createErrorResponse("Solo los asesores pueden listar solicitudes"));
                    }

                    // Obtener parámetros de paginación
                    int page = request.queryParam("page")
                            .map(Integer::parseInt)
                            .filter(p -> p >= 0)
                            .orElse(0);

                    int size = request.queryParam("size")
                            .map(Integer::parseInt)
                            .filter(s -> s > 0 && s <= 100) // Limitar tamaño máximo
                            .orElse(10);

                    log.debug("Listando solicitudes - página: {}, tamaño: {}", page, size);

                    // Obtener solicitudes y total en paralelo
                    Mono<java.util.List<SolicitudListadoResponse.SolicitudItem>> solicitudesMono =
                            solicitudUseCase.listarSolicitudesPendientes(page, size)
                                    .map(this::convertToListadoItem)
                                    .collectList();

                    Mono<Long> totalMono = solicitudUseCase.contarSolicitudesPendientes();

                    return Mono.zip(solicitudesMono, totalMono)
                            .map(tuple -> {
                                java.util.List<SolicitudListadoResponse.SolicitudItem> solicitudes = tuple.getT1();
                                Long total = tuple.getT2();

                                // Crear información de paginación
                                int totalPages = (int) Math.ceil((double) total / size);
                                SolicitudListadoResponse.PageInfo pageInfo = new SolicitudListadoResponse.PageInfo(
                                        page, size, total, totalPages,
                                        page == 0,
                                        page >= totalPages - 1
                                );

                                return new SolicitudListadoResponse(solicitudes, pageInfo);
                            })
                            .flatMap(response -> {
                                log.info("Listado de solicitudes completado - {} solicitudes encontradas para asesor: {}",
                                        response.getSolicitudes().size(), authenticatedUser.getCorreoElectronico());

                                return ServerResponse.ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(response);
                            });
                })
                .onErrorResume(NumberFormatException.class, e -> {
                    log.warn("Parámetros de paginación inválidos: {}", e.getMessage());
                    return ServerResponse.badRequest()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(createErrorResponse("Parámetros de paginación inválidos"));
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error inesperado al listar solicitudes: {}", e.getMessage(), e);
                    return ServerResponse.status(500)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(createErrorResponse("Error interno del servidor"));
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

    private SolicitudListadoResponse.SolicitudItem convertToListadoItem(Solicitud solicitud) {
        // TODO: Necesitaremos obtener los datos del cliente (email, nombre, salario)
        // Por ahora creamos con los datos disponibles

        SolicitudListadoResponse.SolicitudItem item = new SolicitudListadoResponse.SolicitudItem();
        item.setId(solicitud.getId());
        item.setMonto(solicitud.getMonto());
        item.setPlazo(solicitud.getPlazo());
        item.setFechaCreacion(solicitud.getFechaCreacion());

        // Datos del tipo de préstamo
        if (solicitud.getTipoPrestamo() != null) {
            item.setTipoPrestamo(solicitud.getTipoPrestamo().getNombre());
            item.setTasaInteres(solicitud.getTipoPrestamo().getTasaInteres());
        }

        // Estado
        if (solicitud.getEstado() != null) {
            item.setEstadoSolicitud(solicitud.getEstado().getNombre());
        }

        // Calcular monto mensual básico (monto / plazo)
        if (solicitud.getMonto() != null && solicitud.getPlazo() != null && solicitud.getPlazo() > 0) {
            BigDecimal montoMensual = solicitud.getMonto().divide(
                    BigDecimal.valueOf(solicitud.getPlazo()),
                    2,
                    RoundingMode.HALF_UP
            );
            item.setMontoMensualSolicitud(montoMensual);
        }

        // TODO: Necesitamos obtener estos datos del cliente mediante ClienteGateway:
        // - item.setEmail(cliente.getCorreoElectronico());
        // - item.setNombre(cliente.getNombres() + " " + cliente.getApellidos());
        // - item.setSalarioBase(cliente.getSalarioBase());

        return item;
    }

    private ErrorResponse createErrorResponse(String message) {
        return new ErrorResponse(message, System.currentTimeMillis());
    }

    public record ErrorResponse(String message, long timestamp) {}

}
