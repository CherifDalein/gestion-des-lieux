package org.example.gestiondeslieux.service.user;

import lombok.RequiredArgsConstructor;
import org.example.gestiondeslieux.dto.UserDto;
import org.example.gestiondeslieux.dto.UserStatsDto;
import org.example.gestiondeslieux.controller.api.UserApiController;
import org.example.gestiondeslieux.enums.ResourceType;
import org.example.gestiondeslieux.exceptions.AlreadyExistsException;
import org.example.gestiondeslieux.exceptions.ResourceNotFoundException;
import org.example.gestiondeslieux.exceptions.UnauthorizedAccessException;
import org.example.gestiondeslieux.model.Role;
import org.example.gestiondeslieux.model.User;
import org.example.gestiondeslieux.repository.AccessTokenRepository;
import org.example.gestiondeslieux.repository.CollectionRepository;
import org.example.gestiondeslieux.repository.PlaceRepository;
import org.example.gestiondeslieux.repository.RoleRepository;
import org.example.gestiondeslieux.repository.UserRepository;
import org.example.gestiondeslieux.request.ChangePasswordRequest;
import org.example.gestiondeslieux.request.RegisterRequest;
import org.example.gestiondeslieux.request.UpdateUserRequest;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CollectionRepository collectionRepository;
    private final PlaceRepository placeRepository;
    private final AccessTokenRepository accessTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public User registerUser(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AlreadyExistsException("User", "email", normalizedEmail);
        }
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
        User user = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .active(true)
                .roles(Set.of(userRole))
                .build();
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findOptionalByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    @Override
    public User findByEmail(String email) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", normalizedEmail));
    }

    @Override
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto convertToDto(User user) {
        UserDto dto = modelMapper.map(user, UserDto.class);
        dto.setRoles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
        try {
            dto.add(linkTo(methodOn(UserApiController.class).getMe(null)).withSelfRel());
            dto.add(linkTo(methodOn(UserApiController.class).updateMe(null, null)).withRel("update"));
            dto.add(linkTo(methodOn(UserApiController.class).changePassword(null, null)).withRel("changePassword"));
        } catch (Exception ignored) {
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatsDto getUserStats(Long userId) {
        // Keep current behavior consistent with other endpoints: 404 if user does not exist.
        findById(userId);

        long collectionCount = collectionRepository.countByUserId(userId);
        long ownedPlaceCount = placeRepository.countByUserId(userId);
        long sharedCollectionCount = accessTokenRepository
                .countDistinctResourceIdByCreatedByIdAndResourceType(userId, ResourceType.COLLECTION);

        return new UserStatsDto(collectionCount, ownedPlaceCount, sharedCollectionCount);
    }

    @Override
    @Transactional
    public User updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getEmail() != null) {
            String normalizedEmail = request.getEmail().trim().toLowerCase();
            if (!normalizedEmail.equals(user.getEmail()) &&
                    userRepository.existsByEmail(normalizedEmail)) {
                throw new AlreadyExistsException("User", "email", normalizedEmail);
            }
            user.setEmail(normalizedEmail);
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedAccessException("changer le mot de passe : mot de passe actuel incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
