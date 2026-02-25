package org.example.gestiondeslieux.service.user;

import org.example.gestiondeslieux.dto.UserDto;
import org.example.gestiondeslieux.model.User;
import org.example.gestiondeslieux.request.ChangePasswordRequest;
import org.example.gestiondeslieux.request.RegisterRequest;
import org.example.gestiondeslieux.request.UpdateUserRequest;

public interface IUserService {
    User registerUser(RegisterRequest request);
    User findByUsername(String username);
    UserDto convertToDto(User user);
    User updateUser(Long userId, UpdateUserRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
}
