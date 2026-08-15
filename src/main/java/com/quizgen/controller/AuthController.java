package com.quizgen.controller;

import com.quizgen.dto.AuthResponse;
import com.quizgen.dto.LoginRequest;
import com.quizgen.dto.RegisterRequest;
import com.quizgen.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    /**
     * POST /api/auth/login
     * Body: { "username": "...", "password": "..." }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        logger.info("Login attempt for user: {}", request.getUsername());
        AuthResponse response = authService.login(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).body(response);
    }

    /**
     * POST /api/auth/register
     * Body: { "username": "...", "email": "...", "password": "...", "fullName": "...", "role": "STUDENT|TEACHER" }
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        logger.info("Registration attempt for user: {}", request.getUsername());
        AuthResponse response = authService.register(request);
        if (response.isSuccess()) {
            return ResponseEntity.status(201).body(response);
        }
        return ResponseEntity.status(400).body(response);
    }
}
