package org.example.gestiondeslieux.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.dto.UserDto;
import org.example.gestiondeslieux.model.User;
import org.example.gestiondeslieux.request.ChangePasswordRequest;
import org.example.gestiondeslieux.request.UpdateUserRequest;
import org.example.gestiondeslieux.service.user.IUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Gestion du profil utilisateur")
@RequiredArgsConstructor
public class UserApiController {

    private final IUserService userService;

    @GetMapping("/me")
    @Operation(summary = "Profil courant")
    public ResponseEntity<UserDto> getMe(Authentication auth) {
        User user = userService.findByUsername((String) auth.getCredentials());
        return ResponseEntity.ok(userService.toDto(user));
    }

    @PutMapping("/me")
    @Operation(summary = "Mettre à jour le profil")
    public ResponseEntity<UserDto> updateMe(@Valid @RequestBody UpdateUserRequest request,
                                            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User updated = userService.updateUser(userId, request);
        return ResponseEntity.ok(userService.toDto(updated));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Changer le mot de passe")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        userService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }
}
