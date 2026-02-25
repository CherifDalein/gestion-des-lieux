package org.example.gestiondeslieux.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.exceptions.TokenInvalidException;
import org.example.gestiondeslieux.model.Role;
import org.example.gestiondeslieux.model.User;
import org.example.gestiondeslieux.request.LoginRequest;
import org.example.gestiondeslieux.request.RefreshTokenRequest;
import org.example.gestiondeslieux.request.RegisterRequest;
import org.example.gestiondeslieux.response.JwtResponse;
import org.example.gestiondeslieux.security.JwtUtil;
import org.example.gestiondeslieux.service.user.IUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentification JWT")
@RequiredArgsConstructor
public class AuthApiController {

    private final IUserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    @Operation(summary = "Créer un compte")
    public ResponseEntity<JwtResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerUser(request);
        return buildJwtResponse(user, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Se connecter")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.findByUsername(request.getUsername());
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return buildJwtResponse(user, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rafraîchir le token")
    public ResponseEntity<JwtResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtUtil.isTokenValid(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new TokenInvalidException(refreshToken);
        }
        String username = jwtUtil.getUsernameFromToken(refreshToken);
        User user = userService.findByUsername(username);
        return buildJwtResponse(user, HttpStatus.OK);
    }

    @PostMapping("/logout")
    @Operation(summary = "Se déconnecter")
    public ResponseEntity<Void> logout(Authentication authentication) {
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<JwtResponse> buildJwtResponse(User user, HttpStatus status) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
        JwtResponse response = new JwtResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRoles(user.getRoles().stream()
                .map(Role::getName).collect(Collectors.toSet()));
        return ResponseEntity.status(status).body(response);
    }
}
