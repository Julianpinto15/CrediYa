package co.com.pragma.model.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AuthResponse {
    private String token;
    private String refreshToken;
    private User usuario;
    private long expiresIn;
}