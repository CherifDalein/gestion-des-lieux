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
import org.example.gestiondeslieux.response.ApiResponse;
import org.example.gestiondeslieux.response.JwtResponse;
import org.example.gestiondeslieux.security.JwtUtil;
import org.example.gestiondeslieux.service.user.IUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @PostMapping(value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Créer un compte")
    public ResponseEntity<JwtResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerUser(request);
        return buildJwtResponse(user, HttpStatus.CREATED);
    }

    @PostMapping(value = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Se connecter")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        return userService.findOptionalByEmail(request.getEmail())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .map(user -> buildJwtResponse(user, HttpStatus.OK))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping(value = "/refresh",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Rafraîchir le token")
    public ResponseEntity<JwtResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtUtil.isTokenValid(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new TokenInvalidException(refreshToken);
        }
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = userService.findById(userId);
        return buildJwtResponse(user, HttpStatus.OK);
    }

    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Se déconnecter")
    public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>("Déconnexion effectuée avec succès", null));
    }

    private ResponseEntity<JwtResponse> buildJwtResponse(User user, HttpStatus status) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());
        JwtResponse response = new JwtResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setRoles(user.getRoles().stream()
                .map(Role::getName).collect(Collectors.toSet()));
        return ResponseEntity.status(status).body(response);
    }
}
