package org.example.gestiondeslieux.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(max = 100)
    @Schema(example = "Alice")
    private String firstName;

    @Size(max = 100)
    @Schema(example = "Dupont")
    private String lastName;

    @Email
    @Size(max = 100)
    @Schema(example = "alice.new@test.com")
    private String email;
}
