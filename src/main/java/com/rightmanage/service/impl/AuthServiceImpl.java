package com.rightmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rightmanage.common.JwtUtil;
import com.rightmanage.dto.LoginRequest;
import com.rightmanage.dto.LoginResponse;
import com.rightmanage.entity.*;
import com.rightmanage.mapper.*;
import com.rightmanage.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
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

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        String normalizedRawPassword = rawPassword == null ? null : rawPassword.trim();
        String normalizedStoredPassword = storedPassword == null ? null : storedPassword.trim();
        if (normalizedStoredPassword == null) {
            return false;
        }
        if (normalizedStoredPassword.startsWith("$2a$") || normalizedStoredPassword.startsWith("$2b$") || normalizedStoredPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(normalizedRawPassword, normalizedStoredPassword);
        }
        return Objects.equals(normalizedRawPassword, normalizedStoredPassword);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("[auth.login] start, requestNull={}", request == null);
        if (request == null) {
            throw new RuntimeException("登录参数不能为空");
        }
        String normalizedUsername = request.getUsername() == null ? null : request.getUsername().trim();
        String normalizedRawPassword = request.getPassword() == null ? null : request.getPassword().trim();
        log.info("[auth.login] username='{}', rawPwdLen={}",
                normalizedUsername,
                normalizedRawPassword == null ? 0 : normalizedRawPassword.length());
        if (normalizedUsername == null || normalizedUsername.isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (normalizedRawPassword == null || normalizedRawPassword.isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, normalizedUsername);
        List<SysUser> users = sysUserMapper.selectList(wrapper);
        log.info("[auth.login] user query by username count={}", users == null ? 0 : users.size());
        if (users == null || users.isEmpty()) {
            log.warn("[auth.login] user not found by username='{}'", normalizedUsername);
            throw new RuntimeException("用户不存在");
        }

        SysUser user = null;
        for (SysUser candidate : users) {
            String storedPassword = candidate.getPassword();
            int storedLen = storedPassword == null ? 0 : storedPassword.length();
            String storedPrefix;
            if (storedPassword == null || storedPassword.isEmpty()) {
                storedPrefix = "null_or_empty";
            } else if (storedPassword.length() <= 4) {
                storedPrefix = storedPassword;
            } else {
                storedPrefix = storedPassword.substring(0, 4) + "***";
            }
            boolean matched = passwordMatches(normalizedRawPassword, storedPassword);
            log.info("[auth.login] candidate check, userId={}, username='{}', status={}, storedPwdLen={}, storedPwdPrefix={}, matched={}",
                    candidate.getId(), candidate.getUsername(), candidate.getStatus(), storedLen, storedPrefix, matched);
            if (matched) {
                user = candidate;
                break;
            }
        }
        // 兜底：按数据库原始字段直接匹配（应对历史脏数据/映射异常）
        if (user == null) {
            QueryWrapper<SysUser> exactMatch = new QueryWrapper<>();
            exactMatch.eq("usrnam", normalizedUsername).eq("usrpwd", normalizedRawPassword);
            List<SysUser> exactUsers = sysUserMapper.selectList(exactMatch);
            log.info("[auth.login] exact db match count={}", exactUsers == null ? 0 : exactUsers.size());
            if (exactUsers != null && !exactUsers.isEmpty()) {
                user = exactUsers.get(0);
            }
        }
        if (user == null) {
            QueryWrapper<SysUser> trimMatch = new QueryWrapper<>();
            trimMatch.apply("TRIM(usrnam) = {0}", normalizedUsername)
                    .apply("TRIM(usrpwd) = {0}", normalizedRawPassword);
            List<SysUser> trimUsers = sysUserMapper.selectList(trimMatch);
            log.info("[auth.login] trim db match count={}", trimUsers == null ? 0 : trimUsers.size());
            if (trimUsers != null && !trimUsers.isEmpty()) {
                user = trimUsers.get(0);
            }
        }
        if (user == null) {
            log.warn("[auth.login] password mismatch, username='{}'", normalizedUsername);
            throw new RuntimeException("密码错误");
        }
        Integer status = user.getStatus();
        log.info("[auth.login] matched userId={}, status={}", user.getId(), status);
        if (status != null && status == 0) {
            log.warn("[auth.login] user disabled, userId={}", user.getId());
            throw new RuntimeException("用户已被禁用");
        }
        // 获取用户角色
        LambdaQueryWrapper<SysUserRole> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.eq(SysUserRole::getUserId, user.getId());
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(userRoleWrapper);
        log.info("[auth.login] user role count={}, userId={}", userRoles == null ? 0 : userRoles.size(), user.getId());
        if (userRoles.isEmpty()) {
            log.warn("[auth.login] no role assigned, userId={}", user.getId());
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
        log.info("[auth.login] success, userId={}, username='{}', modules={}", user.getId(), user.getUsername(), modules.size());
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
