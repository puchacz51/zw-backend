package pl.pbs.zwbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import pl.pbs.zwbackend.model.enums.Role;

@Data
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private Long id;
    private String username;
    private String email;
    private Role role;
    private String tokenType = "Bearer";

    public TokenResponse(String accessToken, String refreshToken, Long id, String username, String email, Role role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }
}