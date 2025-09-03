package co.com.pragma.model.user.gateways;

import co.com.pragma.model.user.AuthenticatedUser;
import reactor.core.publisher.Mono;

public interface JwtTokenGateway {
    String generateToken(AuthenticatedUser user);
    Mono<AuthenticatedUser> validateToken(String token);
    String generateRefreshToken(AuthenticatedUser user);
}
