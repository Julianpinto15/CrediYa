package co.com.pragma.model.solicitud.exceptions;

public class SolicitudValidationException extends RuntimeException {
    public SolicitudValidationException(String message) {
        super(message);
    }
}