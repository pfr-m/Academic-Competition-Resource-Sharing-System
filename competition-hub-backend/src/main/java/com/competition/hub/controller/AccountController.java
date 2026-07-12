package com.competition.hub.controller;

import com.competition.hub.auth.AuthPrincipal;
import com.competition.hub.entity.User;
import com.competition.hub.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/user")
public class AccountController {
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,18}$");

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AccountController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @PutMapping("/password")
    public Map<String, Object> updatePassword(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody UpdatePasswordRequest request
    ) {
        if (request.oldPassword() == null || request.oldPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入旧密码");
        }
        if (request.newPassword() == null || !PASSWORD_PATTERN.matcher(request.newPassword()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新密码需为 6-18 位字母与数字组合");
        }

        User user = userService.requireUser(principal.id());
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "旧密码不正确");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userService.updateById(user);
        return Map.of("updated", true);
    }

    public record UpdatePasswordRequest(String oldPassword, String newPassword) {
    }
}
