package org.example.gestiondeslieux.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format email invalide")
    @Schema(example = "alice@test.com")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Schema(example = "password123")
    private String password;
}
