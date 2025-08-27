package co.com.pragma.api.config;

import co.com.pragma.model.solicitud.exceptions.ClientNotFoundException;
import co.com.pragma.model.solicitud.exceptions.InvalidLoanTypeException;
import co.com.pragma.model.solicitud.exceptions.SolicitudValidationException;
import co.com.pragma.model.user.exceptions.EmailAlreadyExistsException;
import co.com.pragma.model.user.exceptions.UserValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Order(-2)  // Alta prioridad
public class GlobalErrorHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = determineStatus(ex);
        Map<String, String> errorBody = Map.of("error", ex.getMessage() != null ? ex.getMessage() : "Error inesperado");

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return exchange.getResponse().writeWith(Mono.fromCallable(() ->
                exchange.getResponse().bufferFactory().wrap(objectMapper.writeValueAsBytes(errorBody))
        ));
    }

    private HttpStatus determineStatus(Throwable ex) {
        if (ex instanceof EmailAlreadyExistsException) return HttpStatus.CONFLICT;
        if (ex instanceof UserValidationException || ex instanceof SolicitudValidationException || ex instanceof InvalidLoanTypeException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof ClientNotFoundException) return HttpStatus.NOT_FOUND;
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}