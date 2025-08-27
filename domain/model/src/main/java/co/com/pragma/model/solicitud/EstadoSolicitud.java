package co.com.pragma.model.solicitud;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoSolicitud {
    private UUID id;
    private String nombre;  // Ej. "Pendiente de revisión", "Aprobada"
}