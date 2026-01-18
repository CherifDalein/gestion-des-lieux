package org.example.gestiondeslieux.service;

import org.example.gestiondeslieux.model.User;
import org.example.gestiondeslieux.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class UserService {
    private UserRepository userRepository;
    private BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository) {
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.userRepository = userRepository;
    }

    public User registerUser(User user) throws Exception {
        if(userRepository.findByEmail(user.getEmail()).isPresent()){
            throw new Exception("Email already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRegistrationDate(LocalDate.now());
        return userRepository.save(user);
    }
}
