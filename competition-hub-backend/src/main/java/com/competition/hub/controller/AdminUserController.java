package com.competition.hub.controller;

import com.competition.hub.auth.AuthPrincipal;
import com.competition.hub.entity.User;
import com.competition.hub.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserController {
    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<Map<String, Object>> listAdmins() {
        return userService.lambdaQuery()
                .in(User::getRole, List.of("ADMIN", "SUPER_ADMIN"))
                .orderByDesc(User::getRole)
                .orderByDesc(User::getCreatedAt)
                .list()
                .stream()
                .map(this::presentAdmin)
                .toList();
    }

    @PostMapping
    public Map<String, Object> createAdmin(@Valid @RequestBody AdminUserRequest request) {
        User user = userService.promoteExistingAdmin(
                request.studentNo().trim(),
                request.username().trim()
        );
        return presentAdmin(user);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateAdmin(
            @PathVariable long id,
            @Valid @RequestBody AdminUserUpdateRequest request
    ) {
        User user = userService.requireUser(id);
        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持编辑普通管理员信息");
        }
        userService.findByEmail(request.email().trim())
                .filter(owner -> !owner.getId().equals(user.getId()))
                .ifPresent(owner -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已被其他账号使用");
                });
        user.setNickname(request.nickname().trim());
        user.setRealName(request.nickname().trim());
        user.setEmail(request.email().trim());
        user.setPhone(request.phone() == null ? "" : request.phone().trim());
        userService.updateById(user);
        return presentAdmin(userService.requireUser(id));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> removeAdmin(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable long id
    ) {
        if (principal.id() == id) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能取消自己的超级管理员权限");
        }
        userService.removeAdmin(id);
        return Map.of("updated", true, "userId", id, "newRole", "STUDENT");
    }

    private Map<String, Object> presentAdmin(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("studentNo", user.getStudentNo());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("email", user.getEmail() == null ? "" : user.getEmail());
        result.put("phone", user.getPhone() == null ? "" : user.getPhone());
        result.put("role", user.getRole());
        result.put("createdAt", user.getCreatedAt() == null ? "" : user.getCreatedAt().toString());
        return result;
    }

    public record AdminUserRequest(
            @NotBlank String studentNo,
            @NotBlank String username
    ) {
    }

    public record AdminUserUpdateRequest(
            @NotBlank String nickname,
            @NotBlank @Email String email,
            String phone
    ) {
    }
}
