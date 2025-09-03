package co.com.pragma.model.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AuthenticatedUser {
    private String userId;
    private String correoElectronico;
    private User.Rol rol;
    private String token;
}