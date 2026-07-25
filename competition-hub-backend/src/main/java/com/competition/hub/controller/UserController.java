package com.competition.hub.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.competition.hub.auth.AuthPrincipal;
import com.competition.hub.store.DomainModels.PrivacyLevel;
import com.competition.hub.entity.User;
import com.competition.hub.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public PageResponse<Map<String, Object>> pageUsers(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "10") long size,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String role) {
        String normalizedRole = normalizeRoleOrNull(role);
        Page<User> userPage = userService.lambdaQuery()
                .and(keyword != null && !keyword.isBlank(), wrapper -> wrapper
                        .like(User::getStudentNo, keyword.trim())
                        .or()
                        .like(User::getUsername, keyword.trim())
                        .or()
                        .like(User::getNickname, keyword.trim())
                        .or()
                        .like(User::getEmail, keyword.trim())
                        .or()
                        .like(User::getPhone, keyword.trim()))
                .eq(normalizedRole != null, User::getRole, normalizedRole)
                .orderByDesc(User::getCreatedAt)
                .page(new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100)));
        List<Map<String, Object>> records = userPage.getRecords().stream().map(this::adminUserView).toList();
        return PageResponse.from(userPage, records);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Map<String, Object> deleteUser(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long id) {
        ensureUsersDeletable(principal.id(), List.of(id));
        if (!userService.removeById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在或已删除");
        }
        return Map.of("success", true);
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Map<String, Object> deleteUsers(@AuthenticationPrincipal AuthPrincipal principal, @RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择要删除的用户");
        }
        ensureUsersDeletable(principal.id(), ids);
        userService.removeByIds(ids);
        return Map.of("success", true, "deleted", ids.size());
    }
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal AuthPrincipal principal) {
        return userService.safeUser(userService.requireUser(principal.id()), principal.id());
    }

    @PatchMapping("/me/privacy")
    public Map<String, Object> updatePrivacy(@AuthenticationPrincipal AuthPrincipal principal, @RequestBody UpdatePrivacyRequest request) {
        User updated = userService.updatePrivacy(
                principal.id(),
                request.realName() == null ? null : request.realName().name(),
                request.wechat() == null ? null : request.wechat().name(),
                request.phone() == null ? null : request.phone().name()
        );
        return userService.safeUser(updated, principal.id());
    }

    @GetMapping("/{id}/profile")
    public Map<String, Object> profile(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long id) {
        return userService.safeUser(userService.requireUser(id), principal.id());
    }
    private void ensureUsersDeletable(Long currentUserId, List<Long> ids) {
        if (currentUserId != null && ids.stream().anyMatch(id -> id != null && id.equals(currentUserId))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能删除当前登录的账号！");
        }
        List<User> users = userService.listByIds(ids);
        if (users.size() != ids.stream().distinct().count()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在或已删除");
        }
        boolean containsAdmin = users.stream().anyMatch(user -> "ADMIN".equals(user.getRole()) || "SUPER_ADMIN".equals(user.getRole()));
        if (containsAdmin) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "管理员账号不能在用户管理中删除，请使用管理员权限管理。");
        }
    }

    private String normalizeRoleOrNull(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        String normalized = role.trim().replace("ROLE_", "").replace("-", "_").toUpperCase();
        if ("SUPERADMIN".equals(normalized)) return "SUPER_ADMIN";
        if ("ADMIN".equals(normalized) || "SUPER_ADMIN".equals(normalized) || "MENTOR".equals(normalized) || "STUDENT".equals(normalized)) return normalized;
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色参数不正确");
    }

    private Map<String, Object> adminUserView(User user) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", user.getId());
        row.put("studentNo", user.getStudentNo());
        row.put("username", user.getUsername());
        row.put("nickname", user.getNickname());
        row.put("email", user.getEmail());
        row.put("phone", user.getPhone());
        row.put("role", user.getRole());
        row.put("createdAt", user.getCreatedAt());
        return row;
    }


    public record UpdatePrivacyRequest(PrivacyLevel realName, PrivacyLevel wechat, PrivacyLevel phone) {
    }
}


