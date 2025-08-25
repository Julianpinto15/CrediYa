package co.com.pragma.model.user.exceptions;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("El correo " + email + " ya está registrado");
    }
}