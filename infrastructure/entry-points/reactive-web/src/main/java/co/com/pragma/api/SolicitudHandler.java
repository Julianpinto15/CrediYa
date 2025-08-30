package co.com.pragma.api;

import co.com.pragma.api.dto.ErrorResponse;
import co.com.pragma.api.dto.SolicitudCreateRequest;
import co.com.pragma.api.dto.SolicitudResponse;
import co.com.pragma.api.mapper.SolicitudMapper;
import co.com.pragma.model.solicitud.Solicitud;
import co.com.pragma.model.solicitud.TipoPrestamo;
import co.com.pragma.model.solicitud.gateways.TipoPrestamoRepository;
import co.com.pragma.usecase.solicitud.SolicitudUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;


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
                .doOnSuccess(resp -> log.info("Solicitud registrada exitosamente"))
                .onErrorResume(e -> {
                    log.error("Error registrando solicitud: {}", e.getMessage(), e);
                    return ServerResponse.badRequest()
                            .bodyValue(new ErrorResponse(e.getMessage()));
                });
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
}
