package com.competition.hub.controller;

import com.competition.hub.entity.User;
import com.competition.hub.service.UserService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserControllerTest {

    @Test
    void createAdminPromotesExistingUserByStudentNoAndUsername() {
        UserService userService = mock(UserService.class);
        AdminUserController controller = new AdminUserController(userService);
        User promoted = User.builder()
                .id(5L)
                .studentNo("20261234")
                .username("zhangsan")
                .nickname("张三")
                .role("ADMIN")
                .build();
        when(userService.promoteExistingAdmin("20261234", "zhangsan")).thenReturn(promoted);

        Map<String, Object> result = controller.createAdmin(new AdminUserController.AdminUserRequest(" 20261234 ", " zhangsan "));

        verify(userService).promoteExistingAdmin("20261234", "zhangsan");
        assertThat(result).containsEntry("studentNo", "20261234")
                .containsEntry("username", "zhangsan")
                .containsEntry("role", "ADMIN");
    }
}
