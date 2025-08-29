package co.com.pragma.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SolicitudResponse {

    @Schema(description = "ID de la solicitud", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    private String documentoIdentidad;
    private BigDecimal monto;
    private Integer plazo;

    private TipoPrestamoResponse tipoPrestamo;
    private EstadoSolicitudResponse estado;

    private LocalDateTime fechaCreacion;

    @Data
    public static class TipoPrestamoResponse {
        private UUID id;
        private String nombre;
        private Boolean validacionAutomatica;
    }

    @Data
    public static class EstadoSolicitudResponse {
        private UUID id;
        private String nombre;
        private String descripcion;
    }
}
