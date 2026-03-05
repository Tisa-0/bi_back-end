package com.rightmanage.service;

import com.rightmanage.dto.LoginRequest;
import com.rightmanage.dto.LoginResponse;
public interface AuthService {
    LoginResponse login(LoginRequest request);
    void logout();
}
