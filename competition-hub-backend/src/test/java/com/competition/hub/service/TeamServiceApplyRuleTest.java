package com.competition.hub.service;

import com.competition.hub.entity.Team;
import com.competition.hub.mapper.TeamApplicationMapper;
import com.competition.hub.mapper.TeamMemberMapper;
import com.competition.hub.service.impl.TeamServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class TeamServiceApplyRuleTest {

    @Mock
    private TeamMemberMapper teamMemberMapper;
    @Mock
    private TeamApplicationMapper teamApplicationMapper;

    @Test
    @DisplayName("申请入队 - 边界校验：队长申请自己发布的队伍时必须被 403 拒绝")
    void apply_WhenApplicantIsLeader_ThrowsForbidden() {
        Long leaderUserId = 100L;
        Team team = Team.builder()
                .id(1L)
                .leaderId(leaderUserId)
                .status("APPROVED")
                .build();

        // 隔离 requireApproved，专注测试 apply 内部校验逻辑
        TeamServiceImpl service = new TeamServiceImpl(teamMemberMapper, teamApplicationMapper) {
            @Override
            public Team requireApproved(Long id) {
                return team;
            }
        };

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            service.apply(1L, leaderUserId, "队长本人");
        });

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ex.getReason()).contains("不能申请加入自己发布的队伍");
    }
}
