package com.rightmanage.dto;

import lombok.Data;
import java.util.List;
@Data
public class LoginResponse {
    private Long userId;
    private String username;
    private String token;
    private List<String> roles;
    private List<String> modules;
}
