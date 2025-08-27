package co.com.pragma.api;

import co.com.pragma.model.solicitud.Solicitud;
import co.com.pragma.usecase.solicitud.SolicitudUseCase;
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
public class SolicitudHandler {
    private final SolicitudUseCase solicitudUseCase;

    public Mono<ServerResponse> registrarSolicitud(ServerRequest request) {
        log.debug("Recibiendo solicitud de registro de préstamo");
        return request.bodyToMono(Solicitud.class)
                .flatMap(solicitudUseCase::registrarSolicitud)
                .flatMap(solicitud -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(solicitud))
                .doOnError(e -> log.error("Error en handler de solicitud: {}", e.getMessage(), e));
    }
}