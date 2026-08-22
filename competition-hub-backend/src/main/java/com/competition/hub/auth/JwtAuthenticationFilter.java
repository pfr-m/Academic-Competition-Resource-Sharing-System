package com.competition.hub.auth;

import com.competition.hub.controller.AuthController;
import com.competition.hub.entity.User;
import com.competition.hub.service.UserService;
import com.competition.hub.store.DomainModels.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                AuthPrincipal tokenPrincipal = jwtService.parseToken(token);
                User user = userService.requireUser(tokenPrincipal.id());
                AuthPrincipal principal = new AuthPrincipal(user.getId(), user.getUsername(), Role.valueOf(user.getRole()));
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // 1. 优先从 httpOnly cookie 读取（前端走 cookie 凭证）
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AuthController.AUTH_COOKIE_NAME.equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (token != null && !token.isBlank()) {
                        return token;
                    }
                }
            }
        }
        // 2. 回退到 Authorization: Bearer 头（兼容 legacy 前端）
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
