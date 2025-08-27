package co.com.pragma.config;

import co.com.pragma.model.solicitud.gateways.EstadoSolicitudRepository;
import co.com.pragma.model.solicitud.gateways.SolicitudRepository;
import co.com.pragma.model.solicitud.gateways.TipoPrestamoRepository;
import co.com.pragma.model.user.gateways.UserRepository;
import co.com.pragma.r2dbc.adapter.EstadoSolicitudReactiveRepositoryAdapter;
import co.com.pragma.r2dbc.adapter.TipoPrestamoReactiveRepositoryAdapter;
import co.com.pragma.r2dbc.repository.EstadoSolicitudReactiveRepository;
import co.com.pragma.r2dbc.repository.TipoPrestamoReactiveRepository;
import co.com.pragma.usecase.solicitud.SolicitudUseCase;
import co.com.pragma.usecase.user.UserUseCase;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(basePackages = "co.com.pragma.usecase",
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+UseCase$")
        },
        useDefaultFilters = false)
public class UseCasesConfig {

        @Bean
        public UserUseCase userUseCase(UserRepository userRepository) {
                return new UserUseCase(userRepository);
        }

        @Bean
        public TipoPrestamoRepository tipoPrestamoRepository(TipoPrestamoReactiveRepository repository, ObjectMapper mapper) {
                return new TipoPrestamoReactiveRepositoryAdapter(repository, mapper);
        }

        @Bean
        public EstadoSolicitudRepository estadoSolicitudRepository(EstadoSolicitudReactiveRepository repository, ObjectMapper mapper) {
                return new EstadoSolicitudReactiveRepositoryAdapter(repository, mapper);
        }




}
