package co.com.pragma.model.solicitud;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Solicitud {

    private UUID id;
    private String emailCliente;
    private String nombreCliente;
    private String documentoIdentidad;
    private BigDecimal monto;
    private Integer plazo;
    private BigDecimal tasaInteres;
    private TipoPrestamo tipoPrestamo;
    private EstadoSolicitud estado;
    private BigDecimal salarioCliente;
    private LocalDateTime fechaCreacion;

}
