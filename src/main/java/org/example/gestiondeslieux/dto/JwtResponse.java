package org.example.gestiondeslieux.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtResponse {
    @Schema(example = "eyJhbGciOiJIUzUxMiJ9.access.token")
    private String accessToken;
    @Schema(example = "eyJhbGciOiJIUzUxMiJ9.refresh.token")
    private String refreshToken;
    @Schema(example = "Bearer")
    private String tokenType = "Bearer";
    @Schema(example = "1")
    private Long userId;
    @Schema(example = "alice@test.com")
    private String email;
    @Schema(example = "[\"ROLE_USER\"]")
    private Set<String> roles;
}
