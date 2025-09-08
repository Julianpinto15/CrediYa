package co.com.pragma.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class SecurityHeadersConfig implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        log.debug("Adding security headers to: {}", path);

        // Continuar con la cadena PRIMERO, luego agregar headers
        return chain.filter(exchange)
                .doOnSuccess(unused -> {
                    HttpHeaders headers = exchange.getResponse().getHeaders();
                    headers.set("Content-Security-Policy", "default-src 'self'; frame-ancestors 'self'; form-action 'self'");
                    headers.set("Strict-Transport-Security", "max-age=31536000;");
                    headers.set("X-Content-Type-Options", "nosniff");
                    headers.set("Server", "");
                    headers.set("Cache-Control", "no-store");
                    headers.set("Pragma", "no-cache");
                    headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
                    log.debug("Security headers added successfully to: {}", path);
                });
    }
}