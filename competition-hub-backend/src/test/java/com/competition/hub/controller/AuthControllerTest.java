package com.competition.hub.controller;

import com.competition.hub.auth.JwtService;
import com.competition.hub.entity.User;
import com.competition.hub.service.EmailCodeService;
import com.competition.hub.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;
    @Mock
    private EmailCodeService emailCodeService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        // 使用 Standalone MockMvc 独立构建，无需加载 Spring 完整上下文，毫秒级运行
        this.mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    @DisplayName("用户登录 - 密码错误时返回 401 UNAUTHORIZED")
    void login_WhenPasswordIncorrect_Returns401() throws Exception {
        User user = User.builder()
                .id(1L)
                .username("student1")
                .passwordHash("encoded_pwd")
                .build();

        when(userService.findByUsername("student1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        String jsonBody = """
                {
                    "username": "student1",
                    "password": "wrong_password"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("用户登录 - 账号不存在时返回 401 UNAUTHORIZED")
    void login_WhenUserNotFound_Returns401() throws Exception {
        when(userService.findByUsername("not_found")).thenReturn(Optional.empty());

        String jsonBody = """
                {
                    "username": "not_found",
                    "password": "any_password"
                }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isUnauthorized());
    }
}
