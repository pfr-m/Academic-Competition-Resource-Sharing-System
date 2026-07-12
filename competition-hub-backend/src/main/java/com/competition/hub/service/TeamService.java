package com.competition.hub.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.competition.hub.entity.Team;
import com.competition.hub.entity.TeamApplication;
import com.competition.hub.entity.TeamMember;

import java.util.List;

public interface TeamService extends IService<Team> {
    List<Team> listVisible(Long viewerId, boolean admin);

    IPage<Team> pageVisible(Long viewerId, boolean admin, long current, long size, String keyword, String contest);

    Team createTeam(Team team);

    Team requireExisting(Long id);

    Team requireApproved(Long id);

    TeamApplication apply(Long teamId, Long applicantId, String applicantNickname);

    TeamApplication approveApplication(Long teamId, Long applicationId, Long operatorId, boolean operatorIsAdmin);

    List<TeamApplication> approveApplications(List<Long> applicationIds, Long operatorId, boolean operatorIsAdmin);

    List<TeamApplication> rejectApplications(List<Long> applicationIds, Long operatorId, boolean operatorIsAdmin);

    void deleteApplications(List<Long> applicationIds, Long operatorId, boolean operatorIsAdmin);

    List<TeamApplication> listApplications(Long teamId);

    IPage<TeamApplication> pageManageableApplications(Long operatorId, boolean operatorIsAdmin, long current, long size);

    List<TeamMember> listMembers(Long teamId);

    IPage<TeamMember> pageManageableMembers(Long operatorId, boolean operatorIsAdmin, long current, long size);

    boolean isContactUnlocked(Long teamId, Long viewerId);

    boolean hasUnlockedContactBetween(Long viewerId, Long ownerId);

    void removeMember(Long teamId, Long memberUserId, Long operatorId, boolean operatorIsAdmin);

    Team updateTeam(Team nextTeam, Long operatorId, boolean operatorIsAdmin);

    void deleteTeam(Long teamId, Long operatorId, boolean operatorIsAdmin);

    void updateStatus(Long id, String status);
}
