package co.com.pragma.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Datos requeridos para registrar un nuevo usuario en el sistema")
public class UserRequest {

    @Schema(description = "Nombres completos del usuario",
            example = "Juan Carlos",
            required = true,
            maxLength = 100)
    @NotBlank(message = "El campo nombres es obligatorio")
    private String nombres;

    @Schema(description = "Apellidos completos del usuario",
            example = "Pérez Gómez",
            required = true,
            maxLength = 100)
    @NotBlank(message = "El campo apellidos es obligatorio")
    private String apellidos;

    @Schema(description = "Número de documento de identidad del usuario (cédula)",
            example = "1234567890",
            required = true,
            pattern = "^[0-9]{8,10}$")
    @NotBlank(message = "El documento de identidad es obligatorio")
    @Pattern(regexp = "^[0-9]{8,10}$", message = "El documento debe contener entre 8 y 10 dígitos")
    private String documentoIdentidad;

    @Schema(description = "Fecha de nacimiento del usuario (debe ser mayor de edad)",
            example = "1990-03-21",
            required = true)
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    private LocalDate fechaNacimiento;

    @Schema(description = "Dirección de residencia del usuario",
            example = "Calle 123 #45-67, Bogotá",
            required = true,
            maxLength = 200)
    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    @Schema(description = "Número de teléfono del usuario",
            example = "3101234567",
            required = true,
            pattern = "^[0-9]{10}$")
    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono debe contener exactamente 10 dígitos")
    private String telefono;

    @Schema(description = "Correo electrónico del usuario",
            example = "juan.perez@example.com",
            required = true,
            format = "email")
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    private String correoElectronico;

    @Schema(description = "Salario base mensual del usuario en pesos colombianos",
            example = "3200000",
            required = true,
            minimum = "0")
    @NotNull(message = "El salario base es obligatorio")
    @PositiveOrZero(message = "El salario base debe ser mayor o igual a 0")
    @Max(value = 15000000, message = "El salario base debe ser menor o igual a $15.000.000")
    private BigDecimal salarioBase;
}