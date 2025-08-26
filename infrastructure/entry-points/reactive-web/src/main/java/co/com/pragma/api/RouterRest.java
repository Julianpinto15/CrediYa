package co.com.pragma.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;


@Configuration
public class RouterRest {

    @Bean
    public RouterFunction<ServerResponse> routerFunction(UserHandler handler) {
        return RouterFunctions
                .route()
                .path("/api/v1/usuarios", builder -> builder
                        .POST("", handler::registrarUsuario)
                )
                .build();
    }

}
