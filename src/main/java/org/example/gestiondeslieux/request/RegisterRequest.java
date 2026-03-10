package org.example.gestiondeslieux.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format email invalide")
    @Size(max = 100)
    @Schema(example = "new.user@test.com")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit faire au moins 6 caractères")
    @Schema(example = "password123")
    private String password;

    @NotBlank(message = "La confirmation du mot de passe est obligatoire")
    @Schema(example = "password123")
    private String confirmPassword;

    @Schema(example = "Alice")
    private String firstName;
    @Schema(example = "Dupont")
    private String lastName;

    @AssertTrue(message = "La confirmation du mot de passe ne correspond pas")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isPasswordConfirmed() {
        if (password == null || confirmPassword == null) {
            return true;
        }
        return password.equals(confirmPassword);
    }
}
