package com.competition.hub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.competition.hub.entity.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserService extends IService<User> {
    User requireUser(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByStudentNo(String studentNo);

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    User registerStudent(String studentNo, String passwordHash, String nickname, String phone, String email);

    Map<String, Object> safeUser(User user, Long viewerId);

    User updatePrivacy(Long userId, String realNameVisibility, String wechatVisibility, String phoneVisibility);

    boolean canSeeSensitive(Long viewerId, Long ownerId, String visibility);

    List<String> skills(User user);

    List<String> badges(User user);

    void setAdmin(Long userId);

    User promoteExistingAdmin(String studentNo, String username);

    void removeAdmin(Long userId);

    void transferSuperAdmin(Long fromUserId, Long toUserId);

    long countByRole(String role);

    boolean isFirstLoginPrompted(Long userId);

    void markFirstLoginPrompted(Long userId);
}
