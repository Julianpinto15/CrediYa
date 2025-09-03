package co.com.pragma.r2dbc.adapter;


import co.com.pragma.model.user.AuthenticatedUser;
import co.com.pragma.model.user.User;

import co.com.pragma.model.user.exceptions.InvalidTokenException;
import co.com.pragma.model.user.gateways.JwtTokenGateway;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenAdapter implements JwtTokenGateway {

    private final SecretKey secretKey;
    private final long tokenValidityInSeconds;
    private final long refreshTokenValidityInSeconds;

    public JwtTokenAdapter(
            @Value("${jwt.secret:mySecretKey1234567890123456789012345678901234567890}") String secret,
            @Value("${jwt.token-validity-in-seconds:3600}") long tokenValidityInSeconds,
            @Value("${jwt.refresh-token-validity-in-seconds:86400}") long refreshTokenValidityInSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.tokenValidityInSeconds = tokenValidityInSeconds;
        this.refreshTokenValidityInSeconds = refreshTokenValidityInSeconds;
    }

    @Override
    public String generateToken(AuthenticatedUser user) {
        log.debug("Generando token para usuario: {}", user.getCorreoElectronico());

        Instant now = Instant.now();
        Instant validity = now.plus(tokenValidityInSeconds, ChronoUnit.SECONDS);

        return Jwts.builder()
                .setSubject(user.getUserId())
                .claim("email", user.getCorreoElectronico())
                .claim("rol", user.getRol().name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(validity))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public String generateRefreshToken(AuthenticatedUser user) {
        log.debug("Generando refresh token para usuario: {}", user.getCorreoElectronico());

        Instant now = Instant.now();
        Instant validity = now.plus(refreshTokenValidityInSeconds, ChronoUnit.SECONDS);

        return Jwts.builder()
                .setSubject(user.getUserId())
                .claim("type", "refresh")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(validity))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public Mono<AuthenticatedUser> validateToken(String token) {
        log.debug("Validando token");

        return Mono.fromCallable(() -> {
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(secretKey)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String userId = claims.getSubject();
                String email = claims.get("email", String.class);
                String rolString = claims.get("rol", String.class);

                if (userId == null || email == null || rolString == null) {
                    throw new InvalidTokenException("Token inválido: datos faltantes");
                }

                User.Rol rol = User.Rol.valueOf(rolString);

                return AuthenticatedUser.builder()
                        .userId(userId)
                        .correoElectronico(email)
                        .rol(rol)
                        .token(token)
                        .build();

            } catch (Exception e) {
                log.warn("Error al validar token: {}", e.getMessage());
                throw new InvalidTokenException("Token inválido o expirado");
            }
        });
    }
}
