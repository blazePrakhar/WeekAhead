package com.weekahead.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.weekahead.audit.service.AuditLogService;
import com.weekahead.auth.dto.LoginRequest;
import com.weekahead.auth.dto.LoginResponse;
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
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditLogService auditLogService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
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

        User savedUser = userRepository.save(user);

        auditLogService.log(
                savedUser,
                "REGISTER",
                "USER",
                savedUser.getId(),
                "User registration successful"
        );

        return new RegisterResponse("Registration successful");
    }

    public LoginResponse login(LoginRequest request) {

        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(()
                        -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash())) {

            throw new IllegalArgumentException("Invalid email or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("User account is disabled");
        }

        String accessToken
                = jwtService.generateAccessToken(
                        user.getId(),
                        user.getEmail()
                );

        auditLogService.log(
                user,
                "LOGIN",
                "USER",
                user.getId(),
                "User login successful"
        );

        return new LoginResponse(
                "Login successful",
                accessToken
        );
    }

}
