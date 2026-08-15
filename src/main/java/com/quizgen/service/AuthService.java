package com.quizgen.service;

import com.quizgen.dto.AuthResponse;
import com.quizgen.dto.LoginRequest;
import com.quizgen.dto.RegisterRequest;
import com.quizgen.model.User;
import com.quizgen.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * Register a new user.
     */
    public AuthResponse register(RegisterRequest request) {
        // Validate fields
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return new AuthResponse(false, "Username is required", null, null, null, null);
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return new AuthResponse(false, "Email is required", null, null, null, null);
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return new AuthResponse(false, "Password must be at least 6 characters", null, null, null, null);
        }

        // Check for existing username / email
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            return new AuthResponse(false, "Username already taken", null, null, null, null);
        }
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            return new AuthResponse(false, "Email already registered", null, null, null, null);
        }

        // Determine role
        User.Role role;
        try {
            role = User.Role.valueOf(
                    request.getRole() != null ? request.getRole().toUpperCase() : "STUDENT"
            );
        } catch (IllegalArgumentException e) {
            role = User.Role.STUDENT;
        }

        // Build and save user
        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(hashPassword(request.getPassword()));
        user.setFullName(request.getFullName() != null ? request.getFullName().trim() : "");
        user.setRole(role);
        user.setActive(true);

        User saved = userRepository.save(user);
        logger.info("New user registered: {} ({})", saved.getUsername(), saved.getRole());

        return new AuthResponse(true, "Registration successful", saved.getUsername(),
                saved.getRole().name(), saved.getFullName(), saved.getId());
    }

    /**
     * Authenticate an existing user.
     */
    public AuthResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return new AuthResponse(false, "Username is required", null, null, null, null);
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            return new AuthResponse(false, "Password is required", null, null, null, null);
        }

        Optional<User> userOpt = userRepository.findByUsername(request.getUsername().trim());

        // Also allow login via email
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(request.getUsername().trim().toLowerCase());
        }

        if (userOpt.isEmpty()) {
            return new AuthResponse(false, "Invalid username or password", null, null, null, null);
        }

        User user = userOpt.get();

        if (!user.isActive()) {
            return new AuthResponse(false, "Your account has been disabled", null, null, null, null);
        }

        if (!verifyPassword(request.getPassword(), user.getPassword())) {
            return new AuthResponse(false, "Invalid username or password", null, null, null, null);
        }

        logger.info("User logged in: {} ({})", user.getUsername(), user.getRole());
        return new AuthResponse(true, "Login successful", user.getUsername(),
                user.getRole().name(), user.getFullName(), user.getId());
    }

    // ─── Password Hashing (SHA-256 + Base64) ───────────────────────────────────
    // For production, replace with BCrypt via spring-security-crypto.

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(rawPassword.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private boolean verifyPassword(String rawPassword, String storedHash) {
        return hashPassword(rawPassword).equals(storedHash);
    }
}
