package co.com.pragma.model.solicitud.exceptions;

public class InvalidLoanTypeException extends RuntimeException {
    public InvalidLoanTypeException(String message) {
        super(message);
    }
}