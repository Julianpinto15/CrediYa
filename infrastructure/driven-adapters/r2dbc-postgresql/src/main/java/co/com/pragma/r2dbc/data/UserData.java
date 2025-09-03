package co.com.pragma.r2dbc.data;

import co.com.pragma.model.user.User;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder(toBuilder = true)
@Table("usuarios")
public class UserData {
    @Id
    private String id;

    @Column("nombres")
    private String nombres;

    @Column("apellidos")
    private String apellidos;

    @Column("documento_identidad")
    private  String documentoIdentidad;

    @Column("fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column("direccion")
    private String direccion;

    @Column("telefono")
    private String telefono;

    @Column("correo_electronico")
    private String correoElectronico;

    @Column("salario_base")
    private BigDecimal salarioBase;

    // Nuevos campos para autenticación
    @Column("password")
    private String password;

    @Column("rol")
    private String rol;

    public User toEntity() {
        return User.builder()
                .id(this.id)
                .nombres(this.nombres)
                .apellidos(this.apellidos)
                .documentoIdentidad(this.documentoIdentidad)
                .fechaNacimiento(this.fechaNacimiento)
                .direccion(this.direccion)
                .telefono(this.telefono)
                .correoElectronico(this.correoElectronico)
                .salarioBase(this.salarioBase)
                .password(this.password)
                .rol(this.rol != null ? User.Rol.valueOf(this.rol) : null)
                .build();
    }

    public static UserData fromEntity(User user) {
        return UserData.builder()
                .id(user.getId())
                .nombres(user.getNombres())
                .apellidos(user.getApellidos())
                .documentoIdentidad(user.getDocumentoIdentidad())
                .fechaNacimiento(user.getFechaNacimiento())
                .direccion(user.getDireccion())
                .telefono(user.getTelefono())
                .correoElectronico(user.getCorreoElectronico())
                .salarioBase(user.getSalarioBase())
                .password(user.getPassword())
                .rol(user.getRol() != null ? user.getRol().name() : null)
                .build();
    }

}