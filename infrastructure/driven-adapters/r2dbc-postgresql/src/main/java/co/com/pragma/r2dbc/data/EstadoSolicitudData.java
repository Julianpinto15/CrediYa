package co.com.pragma.r2dbc.data;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Table("estados_solicitud")
public class EstadoSolicitudData {
    @Id
    private UUID id;
    private String nombre;
    private String descripcion;
}