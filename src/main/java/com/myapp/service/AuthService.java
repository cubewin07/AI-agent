package com.myapp.service;

import com.myapp.security.dto.AuthResponse;
import com.myapp.security.dto.LoginRequest;
import com.myapp.security.dto.RefreshTokenRequest;
import com.myapp.security.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);
}
