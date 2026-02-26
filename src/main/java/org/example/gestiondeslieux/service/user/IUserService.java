package org.example.gestiondeslieux.service.user;

import org.example.gestiondeslieux.dto.UserStatsDto;
import org.example.gestiondeslieux.dto.UserDto;
import org.example.gestiondeslieux.model.User;
import org.example.gestiondeslieux.request.ChangePasswordRequest;
import org.example.gestiondeslieux.request.RegisterRequest;
import org.example.gestiondeslieux.request.UpdateUserRequest;

import java.util.Optional;

public interface IUserService {
    User registerUser(RegisterRequest request);
    Optional<User> findOptionalByEmail(String email);
    User findByEmail(String email);
    User findById(Long userId);
    UserDto convertToDto(User user);
    UserStatsDto getUserStats(Long userId);
    User updateUser(Long userId, UpdateUserRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
}
