package co.com.pragma.usecase.user;

import co.com.pragma.model.user.AuthResponse;
import co.com.pragma.model.user.AuthenticatedUser;
import co.com.pragma.model.user.Login;
import co.com.pragma.model.user.User;
import co.com.pragma.model.user.exceptions.InvalidCredentialsException;
import co.com.pragma.model.user.gateways.JwtTokenGateway;
import co.com.pragma.model.user.gateways.PasswordEncoder;
import co.com.pragma.model.user.gateways.UserRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class AuthUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenGateway jwtTokenGateway;

    public Mono<AuthResponse> login(Login login) {
        return userRepository.findByCorreoElectronico(login.getCorreoElectronico())
                .switchIfEmpty(Mono.error(new InvalidCredentialsException("Credenciales inválidas")))
                .flatMap(user -> validatePassword(login.getPassword(), user.getPassword())
                        .flatMap(isValid -> {
                            if (Boolean.TRUE.equals(isValid)) {
                                return generateTokens(user);
                            } else {
                                return Mono.error(new InvalidCredentialsException("Credenciales inválidas"));
                            }
                        }));
    }

    public Mono<AuthenticatedUser> validateToken(String token) {
        return jwtTokenGateway.validateToken(token);
    }

    private Mono<Boolean> validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    private Mono<AuthResponse> generateTokens(User user) {
        AuthenticatedUser authUser = AuthenticatedUser.builder()
                .userId(user.getId())
                .correoElectronico(user.getCorreoElectronico())
                .rol(user.getRol())
                .build();

        String token = jwtTokenGateway.generateToken(authUser);
        String refreshToken = jwtTokenGateway.generateRefreshToken(authUser);

        return Mono.just(AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .usuario(user)
                .expiresIn(3600) // 1 hora en segundos
                .build());
    }
}
