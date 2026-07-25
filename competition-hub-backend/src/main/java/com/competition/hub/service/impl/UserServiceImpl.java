package com.competition.hub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.competition.hub.entity.User;
import com.competition.hub.mapper.UserMapper;
import com.competition.hub.service.TeamService;
import com.competition.hub.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final ObjectMapper objectMapper;
    private final TeamService teamService;

    public UserServiceImpl(ObjectMapper objectMapper, @Lazy TeamService teamService) {
        this.objectMapper = objectMapper;
        this.teamService = teamService;
    }

    @Override
    public User requireUser(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1")));
    }

    @Override
    public Optional<User> findByStudentNo(String studentNo) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<User>()
                .eq(User::getStudentNo, studentNo)
                .last("LIMIT 1")));
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, phone)
                .last("LIMIT 1")));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(getOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .last("LIMIT 1")));
    }

    @Override
    public User registerStudent(String studentNo, String passwordHash, String nickname, String phone, String email) {
        if (findByUsername(studentNo).isPresent() || findByEmail(email).isPresent() || findByPhone(phone).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "账号、邮箱或手机号已被注册");
        }
        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .studentNo(studentNo)
                .username(studentNo)
                .passwordHash(passwordHash)
                .realName(nickname)
                .nickname(nickname)
                .phone(phone)
                .email(email)
                .wechat("")
                .skills("[]")
                .badges("[]")
                .role("STUDENT")
                .realNameVisibility("TEAM_ONLY")
                .phoneVisibility("HIDDEN")
                .wechatVisibility("TEAM_ONLY")
                .firstLoginPrompted(0)
                .createdAt(now)
                .updatedAt(now)
                .deleted(0)
                .build();
        save(user);
        return user;
    }

    @Override
    public Map<String, Object> safeUser(User user, Long viewerId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("studentNo", maskStudentNo(user.getStudentNo()));
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("realName", maskField(user.getRealName(), user.getRealNameVisibility(), user.getId(), viewerId));
        result.put("role", user.getRole());
        result.put("email", user.getId().equals(viewerId) ? user.getEmail() : "");
        result.put("badges", badges(user));
        result.put("phone", maskField(user.getPhone(), user.getPhoneVisibility(), user.getId(), viewerId));
        result.put("wechat", maskField(user.getWechat(), user.getWechatVisibility(), user.getId(), viewerId));
        result.put("skills", skills(user));
        result.put("privacy", user.getId().equals(viewerId)
                ? Map.of(
                        "realName", user.getRealNameVisibility(),
                        "wechat", user.getWechatVisibility(),
                        "phone", user.getPhoneVisibility()
                )
                : null);
        result.put("firstLoginPrompted", user.getFirstLoginPrompted());
        return result;
    }

    @Override
    public User updatePrivacy(Long userId, String realNameVisibility, String wechatVisibility, String phoneVisibility) {
        User user = requireUser(userId);
        if (realNameVisibility != null) {
            user.setRealNameVisibility(realNameVisibility);
        }
        if (wechatVisibility != null) {
            user.setWechatVisibility(wechatVisibility);
        }
        if (phoneVisibility != null) {
            user.setPhoneVisibility(phoneVisibility);
        }
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
        return user;
    }

    @Override
    public boolean canSeeSensitive(Long viewerId, Long ownerId, String visibility) {
        if (viewerId.equals(ownerId) || "PUBLIC".equals(visibility)) {
            return true;
        }
        if ("HIDDEN".equals(visibility)) {
            return false;
        }
        return teamService.hasUnlockedContactBetween(viewerId, ownerId);
    }

    @Override
    public List<String> skills(User user) {
        return parseStringList(user.getSkills());
    }

    @Override
    public List<String> badges(User user) {
        return parseStringList(user.getBadges());
    }

    @Override
    @Transactional
    public void setAdmin(Long userId) {
        User user = requireUser(userId);
        if ("SUPER_ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "超级管理员已是最高权限");
        }
        if ("ADMIN".equals(user.getRole())) {
            return;
        }
        user.setRole("ADMIN");
        user.setFirstLoginPrompted(0);
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
    }

    @Override
    @Transactional
    public User promoteExistingAdmin(String studentNo, String username) {
        User user = findByStudentNo(studentNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "学号对应的用户不存在"));
        if (!username.equals(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学号和用户名与数据库记录不一致");
        }
        if ("SUPER_ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "超级管理员无需重复设置");
        }
        if (!"STUDENT".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持将普通学生账号设置为管理员");
        }
        user.setRole("ADMIN");
        user.setFirstLoginPrompted(0);
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
        return user;
    }

    @Override
    @Transactional
    public void removeAdmin(Long userId) {
        User user = requireUser(userId);
        if ("SUPER_ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无法操作超级管理员");
        }
        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该用户不是管理员");
        }
        user.setRole("STUDENT");
        user.setFirstLoginPrompted(0);
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
    }

    @Override
    @Transactional
    public void transferSuperAdmin(Long fromUserId, Long toUserId) {
        User from = requireUser(fromUserId);
        User to = requireUser(toUserId);
        if (!"SUPER_ADMIN".equals(from.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "您不是超级管理员");
        }
        if (from.getId().equals(to.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能转让给自己");
        }
        if ("SUPER_ADMIN".equals(to.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "目标用户已是超级管理员");
        }
        List<User> currentSuperAdmins = list(new LambdaQueryWrapper<User>().eq(User::getRole, "SUPER_ADMIN"));
        if (currentSuperAdmins.size() != 1 || !currentSuperAdmins.get(0).getId().equals(from.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "超级管理员状态异常，请先修复为唯一超级管理员");
        }
        from.setRole("ADMIN");
        from.setFirstLoginPrompted(0);
        from.setUpdatedAt(LocalDateTime.now());
        updateById(from);
        to.setRole("SUPER_ADMIN");
        to.setFirstLoginPrompted(0);
        to.setUpdatedAt(LocalDateTime.now());
        updateById(to);
    }

    @Override
    public long countByRole(String role) {
        return count(new LambdaQueryWrapper<User>().eq(User::getRole, role));
    }

    @Override
    public boolean isFirstLoginPrompted(Long userId) {
        User user = requireUser(userId);
        return user.getFirstLoginPrompted() != null && user.getFirstLoginPrompted() == 1;
    }

    @Override
    public void markFirstLoginPrompted(Long userId) {
        User user = requireUser(userId);
        user.setFirstLoginPrompted(1);
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
    }

    private String maskField(String value, String visibility, Long ownerId, Long viewerId) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return canSeeSensitive(viewerId, ownerId, visibility) ? value : "已隐藏";
    }

    private String maskStudentNo(String studentNo) {
        if (studentNo == null || studentNo.length() < 6 || !Character.isDigit(studentNo.charAt(0))) {
            return studentNo;
        }
        return studentNo.substring(0, 4) + "****" + studentNo.substring(studentNo.length() - 2);
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException error) {
            return List.of();
        }
    }
}
