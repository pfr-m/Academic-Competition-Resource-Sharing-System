package com.competition.hub.controller;

import com.competition.hub.auth.AuthPrincipal;
import com.competition.hub.store.DomainModels.PrivacyLevel;
import com.competition.hub.entity.User;
import com.competition.hub.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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

    public record UpdatePrivacyRequest(PrivacyLevel realName, PrivacyLevel wechat, PrivacyLevel phone) {
    }
}
