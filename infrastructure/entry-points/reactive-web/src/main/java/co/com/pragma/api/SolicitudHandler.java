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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


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
        log.debug("=== INICIANDO LISTADO DE SOLICITUDES ===");

        return getAuthenticatedUser(request)
                .doOnNext(user -> log.debug("Usuario autenticado: {}, Rol: {}", user.getCorreoElectronico(), user.getRol()))
                .flatMap(authenticatedUser -> {
                    if (authenticatedUser.getRol() != User.Rol.ASESOR) {
                        log.warn("Usuario {} sin permisos de asesor intentó listar solicitudes", authenticatedUser.getCorreoElectronico());
                        return ServerResponse.status(403)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(createErrorResponse("Solo los asesores pueden listar solicitudes"));
                    }

                    int page = request.queryParam("page")
                            .map(Integer::parseInt)
                            .filter(p -> p >= 0)
                            .orElse(0);

                    int size = request.queryParam("size")
                            .map(Integer::parseInt)
                            .filter(s -> s > 0 && s <= 100)
                            .orElse(10);

                    log.debug("=== PARÁMETROS DE PAGINACIÓN ===");
                    log.debug("Página solicitada: {}", page);
                    log.debug("Tamaño solicitado: {}", size);

                    Mono<List<SolicitudListadoResponse.SolicitudItem>> solicitudesMono = solicitudUseCase.listarSolicitudesPendientes(page, size)
                            .map(solicitudes -> {
                                log.debug("=== MAPEANDO SOLICITUDES ===");
                                List<SolicitudListadoResponse.SolicitudItem> items = solicitudes.stream()
                                        .map(solicitud -> {
                                            log.debug("Mapeando solicitud ID: {}", solicitud.getId());
                                            SolicitudListadoResponse.SolicitudItem item = SolicitudMapper.toSolicitudItem(solicitud);
                                            log.debug("Item mapeado - Email: {}, Nombre: {}", item.getEmail(), item.getNombre());
                                            return item;
                                        })
                                        .collect(Collectors.toList());
                                log.debug("Total items mapeados: {}", items.size());
                                return items;
                            })
                            .defaultIfEmpty(Collections.emptyList()) // Maneja el caso de lista vacía
                            .doOnError(error -> log.error("Error al obtener/mapear solicitudes: {}", error.getMessage(), error));

                    Mono<Long> totalMono = solicitudUseCase.contarSolicitudesPendientes()
                            .defaultIfEmpty(0L) // Maneja el caso de conteo 0
                            .doOnNext(total -> log.debug("Total de solicitudes pendientes: {}", total))
                            .doOnError(error -> log.error("Error al contar solicitudes: {}", error.getMessage(), error));

                    return Mono.zip(solicitudesMono, totalMono)
                            .flatMap(tuple -> {
                                List<SolicitudListadoResponse.SolicitudItem> solicitudes = tuple.getT1();
                                Long total = tuple.getT2();

                                int totalPages = (int) Math.ceil((double) total / size);
                                SolicitudListadoResponse.PageInfo pageInfo = new SolicitudListadoResponse.PageInfo(
                                        page, size, total, totalPages,
                                        page == 0,
                                        total == 0 || page >= totalPages - 1
                                );

                                log.debug("=== INFORMACIÓN DE PAGINACIÓN ===");
                                log.debug("Página actual: {}", page);
                                log.debug("Tamaño de página: {}", size);
                                log.debug("Total elementos: {}", total);
                                log.debug("Total páginas: {}", totalPages);
                                log.debug("Es primera página: {}", pageInfo.isPrimera());
                                log.debug("Es última página: {}", pageInfo.isUltima());

                                SolicitudListadoResponse response = new SolicitudListadoResponse(solicitudes, pageInfo);
                                log.debug("=== RESPUESTA FINAL CREADA ===");
                                log.debug("Elementos en respuesta: {}", response.getSolicitudes().size());

                                log.info("=== LISTADO COMPLETADO ===");
                                log.info("Solicitudes en respuesta final: {} para asesor: {}",
                                        response.getSolicitudes().size(), authenticatedUser.getCorreoElectronico());
                                log.debug("Respuesta completa: {}", response);

                                return ServerResponse.ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(response);
                            })
                            .onErrorResume(NumberFormatException.class, e -> {
                                log.warn("Parámetros de paginación inválidos: {}", e.getMessage());
                                return ServerResponse.badRequest()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(createErrorResponse("Parámetros de paginación inválidos"));
                            })
                            .onErrorResume(Exception.class, e -> {
                                log.error("=== ERROR INESPERADO ===");
                                log.error("Error al listar solicitudes: {}", e.getMessage(), e);
                                return ServerResponse.status(500)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(createErrorResponse("Error interno del servidor"));
                            });
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
