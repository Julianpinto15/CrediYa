package co.com.pragma.r2dbc.data;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Table("tipos_prestamo")
public class TipoPrestamoData {
    @Id
    private UUID id;
    private String nombre;
}