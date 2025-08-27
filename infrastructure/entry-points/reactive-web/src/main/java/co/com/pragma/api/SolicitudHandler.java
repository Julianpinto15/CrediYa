package co.com.pragma.api;

import co.com.pragma.api.dto.SolicitudRequest;
import co.com.pragma.model.solicitud.Solicitud;
import co.com.pragma.model.solicitud.TipoPrestamo;
import co.com.pragma.model.solicitud.gateways.TipoPrestamoRepository;
import co.com.pragma.usecase.solicitud.SolicitudUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SolicitudHandler {

    private final SolicitudUseCase solicitudUseCase;
    private final TipoPrestamoRepository tipoPrestamoRepository;

    public Mono<ServerResponse> registrarSolicitud(ServerRequest request) {
        return request.bodyToMono(SolicitudRequest.class)
                .flatMap(this::buildSolicitudFromRequest)
                .flatMap(solicitudUseCase::registrarSolicitud)
                .flatMap(solicitud -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(solicitud));
    }

    private Mono<Solicitud> buildSolicitudFromRequest(SolicitudRequest request) {
        return resolveTipoPrestamo(request)
                .map(tipoPrestamo -> Solicitud.builder()
                        .documentoIdentidad(request.getDocumentoIdentidad())
                        .monto(request.getMonto())
                        .plazo(request.getPlazo())
                        .tipoPrestamo(tipoPrestamo)
                        .fechaCreacion(LocalDateTime.now())
                        .build()
                );
    }

    private Mono<TipoPrestamo> resolveTipoPrestamo(SolicitudRequest request) {
        return Mono.justOrEmpty(request.getTipoPrestamo())
                .flatMap(tp -> Mono.justOrEmpty(tp.getNombre())
                        .flatMap(tipoPrestamoRepository::findByNombre))
                .switchIfEmpty(Mono.justOrEmpty(request.getTipoPrestamoId())
                        .flatMap(tipoPrestamoRepository::findById))
                .switchIfEmpty(Mono.justOrEmpty(request.getTipoPrestamoNombre())
                        .flatMap(tipoPrestamoRepository::findByNombre))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Debe especificar el tipo de préstamo")));
    }
}
