package com.weekahead.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.weekahead.auth.dto.RegisterRequest;
import com.weekahead.auth.dto.RegisterResponse;
import com.weekahead.auth.entity.Role;
import com.weekahead.auth.entity.User;
import com.weekahead.auth.entity.UserStatus;
import com.weekahead.auth.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public RegisterResponse register(RegisterRequest request) {

        String email = request.email().trim().toLowerCase();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }

        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(
                email,
                passwordHash,
                Role.USER,
                UserStatus.ACTIVE
        );

        userRepository.save(user);

        return new RegisterResponse("Registration successful");
    }
}