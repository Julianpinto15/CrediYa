package co.com.pragma.api.dto;

import co.com.pragma.model.solicitud.EstadoSolicitud;
import co.com.pragma.model.solicitud.TipoPrestamo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SolicitudRequest {

    @NotBlank(message = "El documento de identidad es obligatorio")
    private String documentoIdentidad;  // FK implícita a Usuario (via autenticación)

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    @NotNull(message = "El plazo es obligatorio")
    @Positive(message = "El plazo debe ser mayor a 0")
    private Integer plazo;

    @NotNull(message = "El tipo de préstamo es obligatorio")
    private TipoPrestamo tipoPrestamo;  // Referencia a entidad

    @NotNull(message = "El estado es obligatorio")
    private EstadoSolicitud estado;     // Referencia a entidad

    private LocalDateTime fechaCreacion;
}

