package pl.pbs.zwbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.pbs.zwbackend.model.enums.Role;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private String tokenType = "Bearer";

    public TokenResponse(String accessToken, String refreshToken, Long id, String firstName, String lastName, String email, Role role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.tokenType = "Bearer";
    }
}