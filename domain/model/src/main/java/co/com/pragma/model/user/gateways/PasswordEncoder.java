package co.com.pragma.model.user.gateways;

import reactor.core.publisher.Mono;

public interface PasswordEncoder {
    Mono<String> encode(String password);
    Mono<Boolean> matches(String rawPassword, String encodedPassword);
}
