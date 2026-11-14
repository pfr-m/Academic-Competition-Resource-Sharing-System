package com.competition.hub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.competition.hub.entity.Team;
import com.competition.hub.entity.TeamApplication;
import com.competition.hub.entity.TeamMember;
import com.competition.hub.mapper.TeamApplicationMapper;
import com.competition.hub.mapper.TeamMapper;
import com.competition.hub.mapper.TeamMemberMapper;
import com.competition.hub.service.TeamService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {
    private final TeamMemberMapper teamMemberMapper;
    private final TeamApplicationMapper teamApplicationMapper;

    public TeamServiceImpl(TeamMemberMapper teamMemberMapper, TeamApplicationMapper teamApplicationMapper) {
        this.teamMemberMapper = teamMemberMapper;
        this.teamApplicationMapper = teamApplicationMapper;
    }

    @Override
    public List<Team> listVisible(Long viewerId, boolean admin) {
        return list(visibleWrapper(viewerId, admin, null, null));
    }

    @Override
    public IPage<Team> pageVisible(Long viewerId, boolean admin, long current, long size, String keyword, String contest) {
        return page(Page.of(current, size), visibleWrapper(viewerId, admin, keyword, contest));
    }

    private LambdaQueryWrapper<Team> visibleWrapper(Long viewerId, boolean admin, String keyword, String contest) {
        LambdaQueryWrapper<Team> wrapper = new LambdaQueryWrapper<Team>()
                .orderByDesc(Team::getCreatedAt);
        if (!admin) {
            wrapper.and(query -> query.eq(Team::getStatus, "APPROVED").or().eq(Team::getLeaderId, viewerId));
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query
                    .like(Team::getTitle, keyword)
                    .or()
                    .like(Team::getNeedText, keyword)
                    .or()
                    .like(Team::getContest, keyword));
        }
        if (StringUtils.hasText(contest)) {
            wrapper.like(Team::getContest, contest);
        }
        return wrapper;
    }

    @Override
    @Transactional
    public Team createTeam(Team team) {
        LocalDateTime now = LocalDateTime.now();
        team.setCreatedAt(now);
        team.setUpdatedAt(now);
        team.setDeleted(0);
        save(team);
        TeamMember leader = TeamMember.builder()
                .teamId(team.getId())
                .userId(team.getLeaderId())
                .memberRole("LEADER")
                .contactUnlocked(true)
                .joinedAt(now)
                .deleted(0)
                .build();
        teamMemberMapper.insert(leader);
        return team;
    }

    @Override
    public Team requireExisting(Long id) {
        Team team = getById(id);
        if (team == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "队伍不存在");
        }
        return team;
    }

    @Override
    public Team requireApproved(Long id) {
        Team team = requireExisting(id);
        if (!"APPROVED".equals(team.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "队伍不存在或未通过审核");
        }
        return team;
    }

    @Override
    @Transactional
    public TeamApplication apply(Long teamId, Long applicantId, String applicantNickname) {
        Team team = requireApproved(teamId);
        if (team.getLeaderId().equals(applicantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不能申请加入自己发布的队伍");
        }
        TeamApplication existing = teamApplicationMapper.selectOne(new LambdaQueryWrapper<TeamApplication>()
                .eq(TeamApplication::getTeamId, teamId)
                .eq(TeamApplication::getApplicantId, applicantId));
        if (existing != null) {
            if ("REJECTED".equals(existing.getStatus())) {
                existing.setStatus("PENDING");
                existing.setUpdatedAt(LocalDateTime.now());
                teamApplicationMapper.updateById(existing);
            }
            return existing;
        }
        LocalDateTime now = LocalDateTime.now();
        TeamApplication application = TeamApplication.builder()
                .teamId(teamId)
                .applicantId(applicantId)
                .applicantNickname(applicantNickname)
                .status("PENDING")
                .createdAt(now)
                .updatedAt(now)
                .deleted(0)
                .build();
        try {
            teamApplicationMapper.insert(application);
            return application;
        } catch (DuplicateKeyException error) {
            TeamApplication duplicate = teamApplicationMapper.selectOne(new LambdaQueryWrapper<TeamApplication>()
                    .eq(TeamApplication::getTeamId, teamId)
                    .eq(TeamApplication::getApplicantId, applicantId));
            if (duplicate != null) {
                return duplicate;
            }
            throw error;
        }
    }

    @Override
    @Transactional
    public TeamApplication approveApplication(Long teamId, Long applicationId, Long operatorId, boolean operatorIsAdmin) {
        Team team = requireApproved(teamId);
        if (!canManageTeam(team, operatorId, operatorIsAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有队长或管理员可以同意申请");
        }
        TeamApplication application = teamApplicationMapper.selectById(applicationId);
        if (application == null || !application.getTeamId().equals(teamId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "申请不存在");
        }
        application.setStatus("APPROVED");
        application.setUpdatedAt(LocalDateTime.now());
        teamApplicationMapper.updateById(application);

        TeamMember member = teamMemberMapper.selectOne(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId)
                .eq(TeamMember::getUserId, application.getApplicantId()));
        if (member == null) {
            teamMemberMapper.insert(TeamMember.builder()
                    .teamId(teamId)
                    .userId(application.getApplicantId())
                    .memberRole("MEMBER")
                    .contactUnlocked(true)
                    .joinedAt(LocalDateTime.now())
                    .deleted(0)
                    .build());
        } else {
            member.setContactUnlocked(true);
            teamMemberMapper.updateById(member);
        }
        return application;
    }

    @Override
    @Transactional
    public List<TeamApplication> approveApplications(List<Long> applicationIds, Long operatorId, boolean operatorIsAdmin) {
        return applicationIds.stream()
                .map(applicationId -> {
                    TeamApplication application = teamApplicationMapper.selectById(applicationId);
                    if (application == null) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "申请不存在：" + applicationId);
                    }
                    return approveApplication(application.getTeamId(), applicationId, operatorId, operatorIsAdmin);
                })
                .toList();
    }

    @Override
    @Transactional
    public List<TeamApplication> rejectApplications(List<Long> applicationIds, Long operatorId, boolean operatorIsAdmin) {
        return applicationIds.stream()
                .map(applicationId -> {
                    TeamApplication application = teamApplicationMapper.selectById(applicationId);
                    if (application == null) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "申请不存在：" + applicationId);
                    }
                    Team team = requireExisting(application.getTeamId());
                    if (!canManageTeam(team, operatorId, operatorIsAdmin)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有队长或管理员可以拒绝申请");
                    }
                    application.setStatus("REJECTED");
                    application.setUpdatedAt(LocalDateTime.now());
                    teamApplicationMapper.updateById(application);
                    return application;
                })
                .toList();
    }

    @Override
    @Transactional
    public void deleteApplications(List<Long> applicationIds, Long operatorId, boolean operatorIsAdmin) {
        List<TeamApplication> applications = applicationIds.stream()
                .map(applicationId -> {
                    TeamApplication application = teamApplicationMapper.selectById(applicationId);
                    if (application == null) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "申请不存在：" + applicationId);
                    }
                    Team team = requireExisting(application.getTeamId());
                    if (!canManageTeam(team, operatorId, operatorIsAdmin)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有队长或管理员可以删除申请记录");
                    }
                    return application;
                })
                .toList();
        teamApplicationMapper.deleteBatchIds(applications.stream().map(TeamApplication::getId).toList());
    }

    @Override
    public List<TeamApplication> listApplications(Long teamId) {
        return teamApplicationMapper.selectList(new LambdaQueryWrapper<TeamApplication>()
                .eq(TeamApplication::getTeamId, teamId)
                .orderByDesc(TeamApplication::getCreatedAt));
    }

    @Override
    public IPage<TeamApplication> pageManageableApplications(Long operatorId, boolean operatorIsAdmin, long current, long size) {
        List<Long> teamIds = manageableTeamIds(operatorId, operatorIsAdmin);
        LambdaQueryWrapper<TeamApplication> wrapper = new LambdaQueryWrapper<TeamApplication>()
                .orderByDesc(TeamApplication::getCreatedAt);
        if (teamIds.isEmpty()) {
            wrapper.eq(TeamApplication::getTeamId, -1L);
        } else {
            wrapper.in(TeamApplication::getTeamId, teamIds);
        }
        return teamApplicationMapper.selectPage(Page.of(current, size), wrapper);
    }

    @Override
    public List<TeamMember> listMembers(Long teamId) {
        return teamMemberMapper.selectList(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId)
                .orderByAsc(TeamMember::getJoinedAt));
    }

    @Override
    public IPage<TeamMember> pageManageableMembers(Long operatorId, boolean operatorIsAdmin, long current, long size) {
        List<Long> teamIds = manageableTeamIds(operatorId, operatorIsAdmin);
        LambdaQueryWrapper<TeamMember> wrapper = new LambdaQueryWrapper<TeamMember>()
                .orderByDesc(TeamMember::getJoinedAt);
        if (teamIds.isEmpty()) {
            wrapper.eq(TeamMember::getTeamId, -1L);
        } else {
            wrapper.in(TeamMember::getTeamId, teamIds);
        }
        return teamMemberMapper.selectPage(Page.of(current, size), wrapper);
    }

    @Override
    public boolean isContactUnlocked(Long teamId, Long viewerId) {
        Long count = teamMemberMapper.selectCount(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId)
                .eq(TeamMember::getUserId, viewerId)
                .eq(TeamMember::getContactUnlocked, true));
        return count != null && count > 0;
    }

    @Override
    public boolean hasUnlockedContactBetween(Long viewerId, Long ownerId) {
        List<Long> sharedTeams = teamMemberMapper.selectList(new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, viewerId)
                        .eq(TeamMember::getContactUnlocked, true))
                .stream()
                .map(TeamMember::getTeamId)
                .toList();
        if (sharedTeams.isEmpty()) {
            return false;
        }
        Long count = teamMemberMapper.selectCount(new LambdaQueryWrapper<TeamMember>()
                .in(TeamMember::getTeamId, sharedTeams)
                .eq(TeamMember::getUserId, ownerId)
                .eq(TeamMember::getContactUnlocked, true));
        return count != null && count > 0;
    }

    @Override
    public void removeMember(Long teamId, Long memberUserId, Long operatorId, boolean operatorIsAdmin) {
        Team team = requireApproved(teamId);
        if (!canManageTeam(team, operatorId, operatorIsAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有队长或管理员可以管理队员");
        }
        if (team.getLeaderId().equals(memberUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不能移除队长本人");
        }
        TeamMember member = teamMemberMapper.selectOne(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, teamId)
                .eq(TeamMember::getUserId, memberUserId));
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "队员不存在");
        }
        teamMemberMapper.deleteById(member.getId());
    }

    @Override
    public Team updateTeam(Team nextTeam, Long operatorId, boolean operatorIsAdmin) {
        Team team = requireExisting(nextTeam.getId());
        if (!canManageTeam(team, operatorId, operatorIsAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有队长或管理员可以管理队伍，当前用户ID：" + operatorId + "，队长ID：" + team.getLeaderId());
        }
        team.setTitle(nextTeam.getTitle());
        team.setContest(nextTeam.getContest());
        team.setNeedText(nextTeam.getNeedText());
        team.setSkills(nextTeam.getSkills());
        team.setUpdatedAt(LocalDateTime.now());
        updateById(team);
        return team;
    }

    @Override
    @Transactional
    public void deleteTeam(Long teamId, Long operatorId, boolean operatorIsAdmin) {
        Team team = requireExisting(teamId);
        if (!canManageTeam(team, operatorId, operatorIsAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有队长或管理员可以管理队伍，当前用户ID：" + operatorId + "，队长ID：" + team.getLeaderId());
        }
        removeById(teamId);
    }

    private boolean canManageTeam(Team team, Long operatorId, boolean operatorIsAdmin) {
        if (operatorIsAdmin || team.getLeaderId().equals(operatorId)) {
            return true;
        }
        Long leaderMemberCount = teamMemberMapper.selectCount(new LambdaQueryWrapper<TeamMember>()
                .eq(TeamMember::getTeamId, team.getId())
                .eq(TeamMember::getUserId, operatorId)
                .eq(TeamMember::getMemberRole, "LEADER"));
        return leaderMemberCount != null && leaderMemberCount > 0;
    }

    protected List<Long> manageableTeamIds(Long operatorId, boolean operatorIsAdmin) {
        if (operatorIsAdmin) {
            return list(new LambdaQueryWrapper<Team>()
                    .select(Team::getId))
                    .stream()
                    .map(Team::getId)
                    .toList();
        }
        List<Long> ownedTeamIds = list(new LambdaQueryWrapper<Team>()
                .select(Team::getId)
                .eq(Team::getLeaderId, operatorId))
                .stream()
                .map(Team::getId)
                .toList();
        List<Long> memberLeaderTeamIds = teamMemberMapper.selectList(new LambdaQueryWrapper<TeamMember>()
                        .eq(TeamMember::getUserId, operatorId)
                        .eq(TeamMember::getMemberRole, "LEADER"))
                .stream()
                .map(TeamMember::getTeamId)
                .toList();
        List<Long> leaderTeamIds = java.util.stream.Stream.concat(ownedTeamIds.stream(), memberLeaderTeamIds.stream())
                .distinct()
                .toList();
        if (leaderTeamIds.isEmpty()) {
            return List.of();
        }
        return leaderTeamIds;
    }

    @Override
    public void updateStatus(Long id, String status) {
        Team team = requireExisting(id);
        team.setStatus(status);
        updateById(team);
    }
}
