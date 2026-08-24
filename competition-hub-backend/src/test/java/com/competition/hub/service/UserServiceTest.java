package com.competition.hub.service;

import com.competition.hub.entity.User;
import com.competition.hub.mapper.UserMapper;
import com.competition.hub.service.impl.UserServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TeamService teamService;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @Test
    @DisplayName("学生注册 - 成功场景：返回新建用户且默认角色为 STUDENT")
    void registerStudent_Success() {
        // 1. Arrange
        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        // 2. Act
        User user = userService.registerStudent(
                "20260001", "hashed_pwd_123", "张三", "13800000000", "zhangsan@univ.edu.cn"
        );

        // 3. Assert
        assertThat(user).isNotNull();
        assertThat(user.getStudentNo()).isEqualTo("20260001");
        assertThat(user.getRole()).isEqualTo("STUDENT");
        assertThat(user.getNickname()).isEqualTo("张三");

        verify(userMapper, times(1)).insert(any(User.class));
    }

    @Test
    @DisplayName("学生注册 - 冲突场景：学号已存在时应抛出 409 CONFLICT 异常")
    void registerStudent_WhenStudentNoExists_ThrowsConflict() {
        // 1. Arrange
        User existingUser = User.builder().studentNo("20260001").build();
        when(userMapper.selectOne(any(), anyBoolean())).thenReturn(existingUser);

        // 2 & 3. Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            userService.registerStudent("20260001", "pwd", "张三", "13800000000", "test@test.com");
        });

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getReason()).contains("已被注册");

        verify(userMapper, never()).insert(any(User.class));
    }
}