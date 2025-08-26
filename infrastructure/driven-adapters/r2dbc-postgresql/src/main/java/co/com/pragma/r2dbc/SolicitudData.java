package co.com.pragma.r2dbc;

import co.com.pragma.model.solicitud.TipoPrestamo;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Table("solicitudes")
public class SolicitudData {
    @Id
    private UUID id;
    @Column("documento_identidad")
    private String documentoIdentidad;
    private BigDecimal monto;
    private Integer plazo;
    @Column("tipo_prestamo")
    private TipoPrestamo tipoPrestamo;
    private String estado;
    @Column("fecha_creacion")
    private LocalDateTime fechaCreacion;
}
