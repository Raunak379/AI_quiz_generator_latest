package com.quizgen.dto;

public class AuthResponse {
    private boolean success;
    private String message;
    private String username;
    private String role;
    private String fullName;
    private Long userId;

    public AuthResponse() {}

    public AuthResponse(boolean success, String message, String username, String role, String fullName, Long userId) {
        this.success = success;
        this.message = message;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
        this.userId = userId;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
