package com.competition.hub.controller;

import com.competition.hub.auth.AuthPrincipal;
import com.competition.hub.auth.JwtService;
import com.competition.hub.entity.User;
import com.competition.hub.service.EmailCodeService;
import com.competition.hub.service.UserService;
import com.competition.hub.store.DomainModels.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;
    private final EmailCodeService emailCodeService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserService userService, EmailCodeService emailCodeService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userService = userService;
        this.emailCodeService = emailCodeService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userService.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        return buildLoginResponse(user);
    }

    @PostMapping("/login/phone")
    public LoginResponse loginByPhone(@Valid @RequestBody PhoneLoginRequest request) {
        if (!"123456".equals(request.code())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "短信验证码错误");
        }
        User user = userService.findByPhone(request.phone())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "手机号未注册"));
        return buildLoginResponse(user);
    }

    @PostMapping("/email/code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendEmailCode(@Valid @RequestBody EmailCodeRequest request) {
        emailCodeService.sendRegisterCode(request.email());
    }

    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        emailCodeService.verifyCode(request.email(), request.code());
        User user = userService.registerStudent(
                request.studentNo(),
                passwordEncoder.encode(request.password()),
                request.nickname(),
                request.phone(),
                request.email()
        );
        return buildLoginResponse(user);
    }

    @PutMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        emailCodeService.verifyCode(request.email(), request.code());
        User user = userService.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "邮箱未注册"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userService.updateById(user);
    }

    @PostMapping("/first-login-acknowledged")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acknowledgeFirstLogin(@AuthenticationPrincipal AuthPrincipal principal) {
        userService.markFirstLoginPrompted(principal.id());
    }

    private LoginResponse buildLoginResponse(User user) {
        AuthPrincipal principal = new AuthPrincipal(user.getId(), user.getUsername(), Role.valueOf(user.getRole()));
        Map<String, Object> userMap = userService.safeUser(user, user.getId());
        boolean firstLoginPrompted = user.getFirstLoginPrompted() != null && user.getFirstLoginPrompted() == 1;
        boolean privileged = "ADMIN".equals(user.getRole()) || "SUPER_ADMIN".equals(user.getRole());
        boolean roleNoticeRequired = privileged && !firstLoginPrompted;
        return new LoginResponse(
                jwtService.createToken(principal),
                userMap,
                firstLoginPrompted,
                roleNoticeRequired,
                user.getRole()
        );
    }

    public record LoginRequest(@NotBlank String username, @NotBlank @Size(min = 6) String password) {
    }

    public record PhoneLoginRequest(@NotBlank String phone, @NotBlank String code) {
    }

    public record EmailCodeRequest(@NotBlank String email) {
    }

    public record RegisterRequest(@NotBlank String studentNo, @NotBlank @Size(min = 6) String password, @NotBlank String nickname, @NotBlank String phone, @NotBlank String email, @NotBlank String code) {
    }

    public record ResetPasswordRequest(@NotBlank String email, @NotBlank String code, @NotBlank @Size(min = 6) String newPassword) {
    }

    public record LoginResponse(String accessToken, Map<String, Object> user, boolean firstLoginPrompted, boolean roleNoticeRequired, String roleNoticeType) {
    }
}
