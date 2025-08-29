package co.com.pragma.r2dbc.data;

import jakarta.persistence.GeneratedValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("solicitudes")
public class SolicitudData {

    @Id
    private UUID id;

    @Column("documento_identidad")
    private String documentoIdentidad;

    private BigDecimal monto;

    private Integer plazo;

    @Column("tipo_prestamo_id")
    private UUID tipoPrestamoId;

    @Column("estado_solicitud_id")
    private UUID estadoSolicitudId;

    @Column("fecha_creacion")
    private LocalDateTime fechaCreacion;
}
