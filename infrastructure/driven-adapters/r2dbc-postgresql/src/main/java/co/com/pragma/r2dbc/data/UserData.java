package co.com.pragma.r2dbc.data;

import co.com.pragma.model.user.User;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Table("usuarios")
public class UserData {

    @Id
    private Integer id;

    @Column("nombres")
    private String nombres;

    @Column("apellidos")
    private String apellidos;

    @Column("documento_identidad")
    private String documentoIdentidad;

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

    @Column("password")
    private String password;

    @Column("rol")
    private String rol;

    // Constructor por defecto (requerido por Spring Data)
    public UserData() {}

    // Constructor con todos los parámetros
    public UserData(Integer id, String nombres, String apellidos, String documentoIdentidad,
                    LocalDate fechaNacimiento, String direccion, String telefono,
                    String correoElectronico, BigDecimal salarioBase, String password, String rol) {
        this.id = id;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.documentoIdentidad = documentoIdentidad;
        this.fechaNacimiento = fechaNacimiento;
        this.direccion = direccion;
        this.telefono = telefono;
        this.correoElectronico = correoElectronico;
        this.salarioBase = salarioBase;
        this.password = password;
        this.rol = rol;
    }

    // Builder pattern manual
    public static UserDataBuilder builder() {
        return new UserDataBuilder();
    }

    public static class UserDataBuilder {
        private Integer id;
        private String nombres;
        private String apellidos;
        private String documentoIdentidad;
        private LocalDate fechaNacimiento;
        private String direccion;
        private String telefono;
        private String correoElectronico;
        private BigDecimal salarioBase;
        private String password;
        private String rol;

        public UserDataBuilder id(Integer id) {
            this.id = id;
            return this;
        }

        public UserDataBuilder nombres(String nombres) {
            this.nombres = nombres;
            return this;
        }

        public UserDataBuilder apellidos(String apellidos) {
            this.apellidos = apellidos;
            return this;
        }

        public UserDataBuilder documentoIdentidad(String documentoIdentidad) {
            this.documentoIdentidad = documentoIdentidad;
            return this;
        }

        public UserDataBuilder fechaNacimiento(LocalDate fechaNacimiento) {
            this.fechaNacimiento = fechaNacimiento;
            return this;
        }

        public UserDataBuilder direccion(String direccion) {
            this.direccion = direccion;
            return this;
        }

        public UserDataBuilder telefono(String telefono) {
            this.telefono = telefono;
            return this;
        }

        public UserDataBuilder correoElectronico(String correoElectronico) {
            this.correoElectronico = correoElectronico;
            return this;
        }

        public UserDataBuilder salarioBase(BigDecimal salarioBase) {
            this.salarioBase = salarioBase;
            return this;
        }

        public UserDataBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserDataBuilder rol(String rol) {
            this.rol = rol;
            return this;
        }

        public UserData build() {
            return new UserData(id, nombres, apellidos, documentoIdentidad, fechaNacimiento,
                    direccion, telefono, correoElectronico, salarioBase, password, rol);
        }
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getDocumentoIdentidad() {
        return documentoIdentidad;
    }

    public void setDocumentoIdentidad(String documentoIdentidad) {
        this.documentoIdentidad = documentoIdentidad;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico;
    }

    public BigDecimal getSalarioBase() {
        return salarioBase;
    }

    public void setSalarioBase(BigDecimal salarioBase) {
        this.salarioBase = salarioBase;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    // Métodos de conversión
    public User toEntity() {
        return User.builder()
                .id(this.id != null ? this.id.toString() : null)
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
                .id(user.getId() != null ? Integer.valueOf(user.getId()) : null)
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

    @Override
    public String toString() {
        return "UserData{" +
                "id=" + id +
                ", nombres='" + nombres + '\'' +
                ", apellidos='" + apellidos + '\'' +
                ", documentoIdentidad='" + documentoIdentidad + '\'' +
                ", correoElectronico='" + correoElectronico + '\'' +
                ", rol='" + rol + '\'' +
                '}';
    }
}