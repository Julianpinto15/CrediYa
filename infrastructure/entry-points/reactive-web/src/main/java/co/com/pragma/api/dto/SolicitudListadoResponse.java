package co.com.pragma.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta paginada de solicitudes para revisión")
public class SolicitudListadoResponse {

    @Schema(description = "Lista de solicitudes")
    private List<SolicitudItem> solicitudes;

    @Schema(description = "Información de paginación")
    private PageInfo paginacion;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SolicitudItem {
        @Schema(description = "ID de la solicitud")
        private UUID id;

        @Schema(description = "Monto solicitado", example = "5000000")
        private BigDecimal monto;

        @Schema(description = "Plazo en meses", example = "12")
        private Integer plazo;

        @Schema(description = "Correo del cliente", example = "cliente@example.com")
        private String email;

        @Schema(description = "Nombre completo del cliente", example = "Juan Pérez")
        private String nombre;

        @Schema(description = "Tipo de préstamo", example = "Hipotecario")
        private String tipoPrestamo;

        @Schema(description = "Tasa de interés", example = "8.5")
        private BigDecimal tasaInteres;

        @Schema(description = "Estado de la solicitud", example = "Pendiente de revisión")
        private String estadoSolicitud;

        @Schema(description = "Salario base del cliente", example = "3500000")
        private BigDecimal salarioBase;

        @Schema(description = "Monto mensual de la solicitud", example = "450000")
        private BigDecimal montoMensualSolicitud;

        @Schema(description = "Fecha de creación")
        private LocalDateTime fechaCreacion;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo {
        @Schema(description = "Número de página actual (empezando en 0)")
        private int paginaActual;

        @Schema(description = "Tamaño de página")
        private int tamano;

        @Schema(description = "Total de elementos")
        private long totalElementos;

        @Schema(description = "Total de páginas")
        private int totalPaginas;

        @Schema(description = "Es la primera página")
        private boolean primera;

        @Schema(description = "Es la última página")
        private boolean ultima;
    }
}
