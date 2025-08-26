package co.com.pragma.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Max;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UserRequest {
    @NotBlank(message = "El campo nombres es obligatorio")
    private String nombres;

    @NotBlank(message = "El campo apellidos es obligatorio")
    private String apellidos;

    private String documentoIdentidad;
    private LocalDate fechaNacimiento;
    private String direccion;
    private String telefono;

    @NotBlank(message = "El campo correo_electronico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    private String correoElectronico;

    @PositiveOrZero(message = "El salario base debe ser mayor o igual a 0")
    @Max(value = 15000000, message = "El salario base debe ser menor o igual a 15,000,000")
    private BigDecimal salarioBase;
}
