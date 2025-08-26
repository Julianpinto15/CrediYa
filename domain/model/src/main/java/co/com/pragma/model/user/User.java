package co.com.pragma.model.user;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class User {
    private String id;
    private  String nombres;
    private  String apellidos;
    private  String documentoIdentidad;
    private  LocalDate fechaNacimiento;
    private  String direccion;
    private  String telefono;
    private  String correoElectronico;
    private BigDecimal salarioBase;
}
