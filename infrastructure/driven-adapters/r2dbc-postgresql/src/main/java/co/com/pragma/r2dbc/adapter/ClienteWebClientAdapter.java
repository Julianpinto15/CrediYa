package co.com.pragma.r2dbc.adapter;

import co.com.pragma.model.solicitud.gateways.ClienteGateway;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
                .bodyToMono(Boolean.class);
    }
}