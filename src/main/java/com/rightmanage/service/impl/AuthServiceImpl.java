package com.rightmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rightmanage.common.JwtUtil;
import com.rightmanage.dto.LoginRequest;
import com.rightmanage.dto.LoginResponse;
import com.rightmanage.entity.*;
import com.rightmanage.mapper.*;
import com.rightmanage.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;
    @Autowired
    private SysRoleMapper sysRoleMapper;
    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Override
    public LoginResponse login(LoginRequest request) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, request.getUsername());
        SysUser user = sysUserMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        String rawPassword = request.getPassword();
        String storedPassword = user.getPassword();
        String normalizedRawPassword = rawPassword == null ? null : rawPassword.trim();
        String normalizedStoredPassword = storedPassword == null ? null : storedPassword.trim();
        boolean passwordOk = false;
        if (normalizedStoredPassword != null) {
            // 兼容：历史数据可能为明文；新数据可能为 BCrypt
            if (normalizedStoredPassword.startsWith("$2a$") || normalizedStoredPassword.startsWith("$2b$") || normalizedStoredPassword.startsWith("$2y$")) {
                passwordOk = passwordEncoder.matches(normalizedRawPassword, normalizedStoredPassword);
            } else {
                passwordOk = Objects.equals(normalizedRawPassword, normalizedStoredPassword);
            }
        }
        if (!passwordOk) {
            throw new RuntimeException("密码错误");
        }
        Integer status = user.getStatus();
        if (status != null && status == 0) {
            throw new RuntimeException("用户已被禁用");
        }
        // 获取用户角色
        LambdaQueryWrapper<SysUserRole> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.eq(SysUserRole::getUserId, user.getId());
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(userRoleWrapper);
        if (userRoles.isEmpty()) {
            throw new RuntimeException("用户未分配角色");
        }
        List<String> userRoleCodes = userRoles.stream().map(SysUserRole::getRoleCode).collect(Collectors.toList());
        List<SysRole> roles = sysRoleMapper.selectBatchIds(userRoleCodes);
        List<String> roleCodes = roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList());
        // 获取用户模块权限
        List<String> modules = new ArrayList<>();
        for (String roleCode : roleCodes) {
            LambdaQueryWrapper<SysRoleMenu> roleMenuWrapper = new LambdaQueryWrapper<>();
            roleMenuWrapper.eq(SysRoleMenu::getRoleCode, roleCode);
            List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(roleMenuWrapper);
            for (SysRoleMenu rm : roleMenus) {
                if (rm.getModuleCode() != null && !modules.contains(rm.getModuleCode())) {
                    modules.add(rm.getModuleCode());
                }
            }
        }
        // 生成token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        LoginResponse response = new LoginResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setToken(token);
        response.setRoles(roleCodes);
        response.setModules(modules);
        return response;
    }
    @Override
    public void logout() {
    }
}
