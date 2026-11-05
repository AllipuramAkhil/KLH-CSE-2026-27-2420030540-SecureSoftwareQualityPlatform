package com.assqp.platform.service;

import com.assqp.platform.dto.auth.AuthResponse;
import com.assqp.platform.dto.auth.LoginRequest;
import com.assqp.platform.dto.auth.RegisterRequest;
import com.assqp.platform.dto.user.UserResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(String refreshToken);
    UserResponse getCurrentUser();
}
