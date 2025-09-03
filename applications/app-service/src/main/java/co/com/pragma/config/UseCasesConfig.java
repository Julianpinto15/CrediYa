package co.com.pragma.config;

import co.com.pragma.model.solicitud.gateways.ClienteGateway;
import co.com.pragma.model.solicitud.gateways.EstadoSolicitudRepository;
import co.com.pragma.model.solicitud.gateways.SolicitudRepository;
import co.com.pragma.model.user.gateways.JwtTokenGateway;
import co.com.pragma.model.user.gateways.PasswordEncoder;
import co.com.pragma.model.user.gateways.UserRepository;
import co.com.pragma.usecase.solicitud.SolicitudUseCase;
import co.com.pragma.usecase.user.AuthUseCase;
import co.com.pragma.usecase.user.UserUseCase;
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
        public UserUseCase userUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
                return new UserUseCase(userRepository, passwordEncoder);
        }

        @Bean
        public SolicitudUseCase solicitudUseCase(SolicitudRepository solicitudRepository,
                                                 EstadoSolicitudRepository estadoSolicitudRepository,
                                                 ClienteGateway clienteGateway) {
                return new SolicitudUseCase(solicitudRepository, estadoSolicitudRepository, clienteGateway);
        }

        @Bean
        public AuthUseCase authUseCase(UserRepository userRepository,
                                       PasswordEncoder passwordEncoder,
                                       JwtTokenGateway jwtTokenGateway) {
                return new AuthUseCase(userRepository, passwordEncoder, jwtTokenGateway);
        }

}