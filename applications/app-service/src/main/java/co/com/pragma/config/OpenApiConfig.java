package co.com.pragma.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CrediYa API")
                        .description("""
                            API REST para el sistema de gestión de préstamos CrediYa.
                            
                            Esta API permite:
                            - Registrar nuevos usuarios/solicitantes
                            - Verificar la existencia de usuarios por documento
                            - Registrar solicitudes de préstamo
                            
                            ## Flujo de uso:
                            1. Registrar usuario con datos personales
                            2. Verificar que el usuario existe antes de crear solicitudes
                            3. Crear solicitudes de préstamo asociadas al usuario
                            
                            ## Códigos de respuesta comunes:
                            - **200**: Operación exitosa
                            - **400**: Datos inválidos o error de validación
                            - **404**: Recurso no encontrado
                            - **409**: Conflicto (ej. email duplicado)
                            - **500**: Error interno del servidor
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipo CrediYa")
                                .email("soporte@crediya.com")
                                .url("https://www.crediya.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Servidor de desarrollo"),
                        new Server()
                                .url("https://api.crediya.com")
                                .description("Servidor de producción")));
    }
}
