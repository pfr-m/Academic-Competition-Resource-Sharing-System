package com.competition.hub.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.competition.hub.entity.Team;
import com.competition.hub.entity.TeamApplication;
import com.competition.hub.entity.TeamMember;
import com.competition.hub.mapper.TeamApplicationMapper;
import com.competition.hub.mapper.TeamMemberMapper;
import com.competition.hub.service.impl.TeamServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeamServiceDuplicateSubmitTest {

    @Test
    void applyReturnsExistingApplicationWhenConcurrentDuplicateInsertHitsUniqueKey() {
        TeamMemberMapper teamMemberMapper = mock(TeamMemberMapper.class);
        TeamApplicationMapper applicationMapper = mock(TeamApplicationMapper.class);
        Team team = Team.builder()
                .id(11L)
                .leaderId(1L)
                .status("APPROVED")
                .build();
        TeamServiceImpl service = new TeamServiceImpl(teamMemberMapper, applicationMapper) {
            @Override
            public Team requireApproved(Long id) {
                return team;
            }
        };

        TeamApplication existing = TeamApplication.builder()
                .id(23L)
                .teamId(11L)
                .applicantId(7L)
                .applicantNickname("student")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleted(0)
                .build();
        when(applicationMapper.selectOne(any(Wrapper.class))).thenReturn(null, existing);
        when(applicationMapper.insert(any(TeamApplication.class))).thenThrow(new DuplicateKeyException("uk_team_applicant"));

        TeamApplication result = service.apply(11L, 7L, "student");

        assertThat(result.getId()).isEqualTo(23L);
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void manageableTeamIdsIncludeTeamsOwnedByLeaderIdEvenWithoutLeaderMemberRecord() {
        TeamMemberMapper teamMemberMapper = mock(TeamMemberMapper.class);
        TeamApplicationMapper applicationMapper = mock(TeamApplicationMapper.class);
        ManageableTeamIdsService service = new ManageableTeamIdsService(teamMemberMapper, applicationMapper);
        when(teamMemberMapper.selectList(any(Wrapper.class))).thenReturn(List.<TeamMember>of());

        List<Long> teamIds = service.exposedManageableTeamIds(9L);

        assertThat(teamIds).containsExactly(31L, 32L);
    }

    private static class ManageableTeamIdsService extends TeamServiceImpl {
        ManageableTeamIdsService(TeamMemberMapper teamMemberMapper, TeamApplicationMapper applicationMapper) {
            super(teamMemberMapper, applicationMapper);
        }

        @Override
        public List<Team> list(Wrapper<Team> queryWrapper) {
            return List.of(
                    Team.builder().id(31L).leaderId(9L).build(),
                    Team.builder().id(32L).leaderId(9L).build()
            );
        }

        List<Long> exposedManageableTeamIds(Long operatorId) {
            return manageableTeamIds(operatorId, false);
        }
    }
}
