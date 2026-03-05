package com.rightmanage.controller;

import com.rightmanage.common.Result;
import com.rightmanage.dto.LoginRequest;
import com.rightmanage.dto.LoginResponse;
import com.rightmanage.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private AuthService authService;
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }
    @PostMapping("/logout")
    public Result<?> logout() {
        authService.logout();
        return Result.success();
    }
    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo() {
        // 从SecurityContext获取当前用户信息
        Map<String, Object> data = new HashMap<>();
        return Result.success(data);
    }
}
