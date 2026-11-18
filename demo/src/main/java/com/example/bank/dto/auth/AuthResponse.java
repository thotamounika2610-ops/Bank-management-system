package com.example.bank.dto.auth;

import com.example.bank.enums.Role;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, Role role) {
}
