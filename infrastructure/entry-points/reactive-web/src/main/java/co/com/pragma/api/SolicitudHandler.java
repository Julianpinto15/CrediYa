package co.com.pragma.api;

import co.com.pragma.model.solicitud.Solicitud;
import co.com.pragma.usecase.solicitud.SolicitudUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class SolicitudHandler {
    private final SolicitudUseCase solicitudUseCase;

    public Mono<ServerResponse> registrarSolicitud(ServerRequest request) {
        return request.bodyToMono(Solicitud.class)
                .flatMap(solicitudUseCase::registrarSolicitud)
                .flatMap(solicitud -> ServerResponse.ok().bodyValue(solicitud))
                .onErrorResume(e -> ServerResponse.badRequest().bodyValue(e.getMessage()));
    }
}
