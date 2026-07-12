package com.competition.hub.controller;

import com.competition.hub.auth.AuthPrincipal;
import com.competition.hub.entity.User;
import com.competition.hub.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/super")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {
    private final UserService userService;

    public SuperAdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> listUsers(@RequestParam(required = false) String role) {
        return userService.lambdaQuery()
                .eq(role != null && !role.isBlank(), User::getRole, role)
                .list()
                .stream()
                .map(user -> Map.<String, Object>of(
                        "id", user.getId(),
                        "studentNo", user.getStudentNo(),
                        "nickname", user.getNickname(),
                        "role", user.getRole()
                ))
                .toList();
    }

    @PostMapping("/users/{userId}/promote-admin")
    public Map<String, Object> promoteAdmin(@PathVariable long userId) {
        userService.setAdmin(userId);
        return Map.of("updated", true, "userId", userId, "newRole", "ADMIN");
    }

    @PostMapping("/users/{userId}/demote-admin")
    public Map<String, Object> demoteAdmin(@PathVariable long userId) {
        userService.removeAdmin(userId);
        return Map.of("updated", true, "userId", userId, "newRole", "STUDENT");
    }

    @PostMapping("/users/{userId}/transfer")
    public Map<String, Object> transferSuperAdmin(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable long userId
    ) {
        userService.transferSuperAdmin(principal.id(), userId);
        return Map.of("transferred", true, "newSuperAdminId", userId);
    }
}