package co.com.pragma.api.dto;

import co.com.pragma.model.solicitud.EstadoSolicitud;
import co.com.pragma.model.solicitud.TipoPrestamo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SolicitudRequest {

    @NotBlank(message = "El documento de identidad es obligatorio")
    private String documentoIdentidad;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    @NotNull(message = "El plazo es obligatorio")
    @Positive(message = "El plazo debe ser mayor a 0")
    private Integer plazo;

    // Opción 1: Recibir el ID del tipo de préstamo
    @NotNull(message = "El tipo de préstamo es obligatorio")
    private UUID tipoPrestamoId;

    // Opción 2: Recibir el nombre del tipo de préstamo
    private String tipoPrestamoNombre;

    // Opción 3: Recibir el objeto completo (si viene del frontend)
    private TipoPrestamo tipoPrestamo;

    // Similar para el estado
    @NotNull(message = "El estado es obligatorio")
    private UUID estadoSolicitudId;

    private String estadoNombre;
    private EstadoSolicitud estado;

    private LocalDateTime fechaCreacion;
}