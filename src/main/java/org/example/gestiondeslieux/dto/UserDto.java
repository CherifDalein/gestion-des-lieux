package org.example.gestiondeslieux.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = false)
public class UserDto extends RepresentationModel<UserDto> {
    @Schema(example = "1")
    private Long id;
    @Schema(example = "alice@test.com")
    private String email;
    @Schema(example = "Alice")
    private String firstName;
    @Schema(example = "Dupont")
    private String lastName;
    @Schema(example = "2026-02-25T22:30:00")
    private LocalDateTime createdAt;
    @Schema(example = "true")
    private Boolean active;
    @Schema(example = "[\"ROLE_USER\"]")
    private Set<String> roles;
}
