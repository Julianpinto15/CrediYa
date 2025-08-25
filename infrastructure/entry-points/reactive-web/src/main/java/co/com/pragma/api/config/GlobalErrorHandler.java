package co.com.pragma.api.config;

import co.com.pragma.model.user.exceptions.EmailAlreadyExistsException;
import co.com.pragma.model.user.exceptions.UserValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public Mono<Void> handleEmailAlreadyExists(EmailAlreadyExistsException ex, ServerWebExchange exchange) {
        return buildErrorResponse(exchange, HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UserValidationException.class)
    public Mono<Void> handleValidation(UserValidationException ex, ServerWebExchange exchange) {
        return buildErrorResponse(exchange, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Mono<Void> handleGeneric(Exception ex, ServerWebExchange exchange) {
        return buildErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado");
    }

    private Mono<Void> buildErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> errorBody = new HashMap<>();
        errorBody.put("error", message);

        byte[] bytes = errorBody.toString().getBytes();
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(bytes)));
    }
}
