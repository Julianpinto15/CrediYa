package co.com.pragma.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;


@Configuration
public class RouterRest {
    @Bean
    public RouterFunction<ServerResponse> routerFunction(UserHandler handlerV1, SolicitudHandler handlerV2) {
        return RouterFunctions
            .route()
                .path("/api/v1/usuarios", builder -> builder
                        .POST("", handlerV1::registrarUsuario)
                )
            .path("/api/v1/solicitudes", builder -> builder.POST("", handlerV2::registrarSolicitud))
            .build();
        }
}
