package co.com.pragma.r2dbc.adapter;

import co.com.pragma.model.solicitud.ClienteCompleto;
import co.com.pragma.r2dbc.adapter.dto.ExistsResponse;
import co.com.pragma.model.solicitud.gateways.ClienteGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
public class ClienteWebClientAdapter implements ClienteGateway {

    private final WebClient webClient;


    public ClienteWebClientAdapter(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://localhost:8080").build();
    }

    @Override
    public Mono<Boolean> existsByDocumento(String documento) {
        return webClient.get()
                .uri("/api/v1/usuarios/exists/{documento}", documento)
                .retrieve()
                .bodyToMono(ExistsResponse.class) // Usa .class
                .map(ExistsResponse::getExists)
                .defaultIfEmpty(false); // Si no hay respuesta, asume false
    }

    @Override
    public Mono<ClienteCompleto> findByDocumento(String documento) {
        log.debug("Obteniendo datos completos del cliente con documento: {}", documento);
        return webClient.get()
                .uri("/api/v1/usuarios/{documento}", documento)
                .retrieve()
                .bodyToMono(ClienteCompleto.class)
                .doOnSuccess(cliente -> {
                    if (cliente != null) {
                        log.debug("Cliente encontrado: {} {}", cliente.getNombre(), cliente.getApellido());
                    }
                })
                .onErrorResume(WebClientResponseException.NotFound.class, ex -> {
                    log.warn("Cliente con documento {} no encontrado", documento);
                    return Mono.empty(); // No existe -> retornamos vacío
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("Error {} del servicio de usuarios al buscar cliente {}: {}",
                            ex.getStatusCode(), documento, ex.getMessage());
                    return Mono.empty(); // Cualquier otro error HTTP -> fallback vacío
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Error inesperado al obtener cliente {}: {}", documento, ex.getMessage());
                    return Mono.empty(); // Evita romper todo el flujo
                });
    }


}