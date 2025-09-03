package co.com.pragma.r2dbc.adapter;

import co.com.pragma.model.user.gateways.PasswordEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
public class PasswordEncoderAdapter implements PasswordEncoder {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public PasswordEncoderAdapter() {
        this.bCryptPasswordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public Mono<String> encode(String password) {
        log.debug("Encriptando contraseña");

        return Mono.fromCallable(() -> bCryptPasswordEncoder.encode(password))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(encoded -> log.debug("Contraseña encriptada exitosamente"));
    }

    @Override
    public Mono<Boolean> matches(String rawPassword, String encodedPassword) {
        log.debug("Verificando contraseña");

        return Mono.fromCallable(() -> bCryptPasswordEncoder.matches(rawPassword, encodedPassword))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(matches -> log.debug("Verificación de contraseña: {}", matches ? "exitosa" : "fallida"));
    }
}