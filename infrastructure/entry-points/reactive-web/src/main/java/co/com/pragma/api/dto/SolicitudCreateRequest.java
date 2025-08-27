package co.com.pragma.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SolicitudCreateRequest {

    @Schema(description = "Documento de identidad del cliente", example = "987654321")
    @NotBlank(message = "El documento de identidad es obligatorio")
    private String documentoIdentidad;

    @Schema(description = "Monto solicitado en pesos colombianos", example = "15000000")
    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    @Schema(description = "Plazo del préstamo en meses", example = "24")
    @NotNull(message = "El plazo es obligatorio")
    @Positive(message = "El plazo debe ser mayor a 0")
    private Integer plazo;

    @Schema(description = "Tipo de préstamo solicitado", example = "PERSONAL")
    @NotBlank(message = "El tipo de préstamo es obligatorio")
    private String tipoPrestamoNombre;
}