package com.competition.hub.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.competition.hub.auth.AuthPrincipal;
import com.competition.hub.entity.Team;
import com.competition.hub.entity.TeamApplication;
import com.competition.hub.entity.TeamMember;
import com.competition.hub.entity.User;
import com.competition.hub.service.ReviewService;
import com.competition.hub.service.TeamService;
import com.competition.hub.service.UserService;
import com.competition.hub.store.DomainModels.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/teams")
public class TeamController {
    private final TeamService teamService;
    private final ReviewService reviewService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public TeamController(TeamService teamService, ReviewService reviewService, UserService userService, ObjectMapper objectMapper) {
        this.teamService = teamService;
        this.reviewService = reviewService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public PageResponse<Map<String, Object>> list(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "6") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String contest
    ) {
        IPage<Team> result = teamService.pageVisible(principal.id(), isPlatformAdmin(principal), page, size, keyword, contest);
        List<Map<String, Object>> records = result.getRecords().stream()
                .map(team -> presentTeam(team, principal.id()))
                .toList();
        return PageResponse.from(result, records);
    }

    @PostMapping
    public Map<String, Object> create(@AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody CreateTeamRequest request) {
        Team team = Team.builder()
                .title(request.title())
                .contest(request.contest())
                .needText(request.need())
                .leaderId(principal.id())
                .leaderNickname(userService.requireUser(principal.id()).getNickname())
                .skills(toJson(request.skills()))
                .status(isPlatformAdmin(principal) ? "APPROVED" : "PENDING")
                .build();
        Team saved = teamService.createTeam(team);
        reviewService.createReview("team", saved.getId(), saved.getTitle(), principal.id());
        return presentTeam(saved, principal.id());
    }

    @PostMapping("/{id}/apply")
    public Map<String, Object> apply(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long id) {
        TeamApplication application = teamService.apply(id, principal.id(), userService.requireUser(principal.id()).getNickname());
        return Map.of("application", application, "applicantProfile", skillProfile(principal.id()));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long id, @Valid @RequestBody CreateTeamRequest request) {
        Team nextTeam = Team.builder()
                .id(id)
                .title(request.title())
                .contest(request.contest())
                .needText(request.need())
                .skills(toJson(request.skills()))
                .build();
        Team saved = teamService.updateTeam(nextTeam, principal.id(), isPlatformAdmin(principal));
        return presentTeam(saved, principal.id());
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long id) {
        teamService.deleteTeam(id, principal.id(), isPlatformAdmin(principal));
        return Map.of("deleted", true);
    }

    @PostMapping("/{teamId}/applications/{applicationId}/approve")
    public Map<String, Object> approve(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long teamId, @PathVariable long applicationId) {
        TeamApplication application = teamService.approveApplication(teamId, applicationId, principal.id(), isPlatformAdmin(principal));
        return presentTeam(teamService.requireApproved(teamId), application.getApplicantId());
    }

    @PostMapping("/applications/batch-approve")
    public Map<String, Object> approveApplications(@AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody BatchApplicationRequest request) {
        List<TeamApplication> applications = teamService.approveApplications(request.applicationIds(), principal.id(), isPlatformAdmin(principal));
        return Map.of("approved", applications.size());
    }

    @PostMapping("/applications/batch-reject")
    public Map<String, Object> rejectApplications(@AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody BatchApplicationRequest request) {
        List<TeamApplication> applications = teamService.rejectApplications(request.applicationIds(), principal.id(), isPlatformAdmin(principal));
        return Map.of("rejected", applications.size());
    }

    @DeleteMapping("/applications/batch")
    public Map<String, Object> deleteApplications(@AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody BatchApplicationRequest request) {
        teamService.deleteApplications(request.applicationIds(), principal.id(), isPlatformAdmin(principal));
        return Map.of("deleted", request.applicationIds().size());
    }

    @GetMapping("/applications/manageable")
    public PageResponse<Map<String, Object>> manageableApplications(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "8") long size
    ) {
        IPage<TeamApplication> result = teamService.pageManageableApplications(principal.id(), isPlatformAdmin(principal), page, size);
        List<Map<String, Object>> records = result.getRecords().stream()
                .map(application -> Map.<String, Object>of(
                        "application", application,
                        "team", teamService.requireExisting(application.getTeamId()),
                        "applicantProfile", skillProfile(application.getApplicantId())
                ))
                .toList();
        return PageResponse.from(result, records);
    }

    @GetMapping("/members/manageable")
    public PageResponse<Map<String, Object>> manageableMembers(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "8") long size
    ) {
        IPage<TeamMember> result = teamService.pageManageableMembers(principal.id(), isPlatformAdmin(principal), page, size);
        List<Map<String, Object>> records = result.getRecords().stream()
                .map(member -> Map.<String, Object>of(
                        "team", teamService.requireExisting(member.getTeamId()),
                        "member", presentMember(member)
                ))
                .toList();
        return PageResponse.from(result, records);
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public Map<String, Object> removeMember(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long teamId, @PathVariable long userId) {
        teamService.removeMember(teamId, userId, principal.id(), isPlatformAdmin(principal));
        return Map.of("deleted", true);
    }

    @GetMapping("/matchmaking/candidates")
    public List<Map<String, Object>> candidates() {
        return userService.lambdaQuery()
                .eq(User::getRole, "STUDENT")
                .list()
                .stream()
                .map(user -> skillProfile(user.getId()))
                .toList();
    }

    private Map<String, Object> presentTeam(Team team, long viewerId) {
        User leader = userService.requireUser(team.getLeaderId());
        User viewer = userService.requireUser(viewerId);
        boolean unlocked = teamService.isContactUnlocked(team.getId(), viewerId) || leader.getId().equals(viewerId) || "ADMIN".equals(viewer.getRole()) || "SUPER_ADMIN".equals(viewer.getRole());
        return Map.of(
                "id", team.getId(),
                "title", team.getTitle(),
                "contest", team.getContest(),
                "need", team.getNeedText(),
                "skills", parseSkills(team.getSkills()),
                "status", team.getStatus(),
                "leader", Map.of(
                        "id", leader.getId(),
                        "nickname", leader.getNickname(),
                        "badges", userService.badges(leader),
                        "realName", unlocked ? leader.getRealName() : "双向确认前隐藏",
                        "phone", unlocked ? leader.getPhone() : "双向确认前隐藏",
                        "wechat", unlocked ? leader.getWechat() : "双向确认前隐藏"
                ),
                "applications", teamService.listApplications(team.getId()).stream()
                        .map(application -> Map.of("application", application, "applicantProfile", skillProfile(application.getApplicantId())))
                        .toList(),
                "members", teamService.listMembers(team.getId()).stream()
                        .map(this::presentMember)
                        .toList()
        );
    }

    private boolean isPlatformAdmin(AuthPrincipal principal) {
        return principal.role() == Role.ADMIN || principal.role() == Role.SUPER_ADMIN;
    }

    private Map<String, Object> presentMember(TeamMember member) {
        User user = userService.requireUser(member.getUserId());
        return Map.of(
                "userId", user.getId(),
                "nickname", user.getNickname(),
                "maskedName", user.getStudentNo().substring(0, Math.min(4, user.getStudentNo().length())) + "级 · " + user.getRealName().substring(0, 1) + "***",
                "role", member.getMemberRole(),
                "skills", userService.skills(user),
                "joinedAt", member.getJoinedAt().toString(),
                "contactUnlocked", member.getContactUnlocked()
        );
    }

    private Map<String, Object> skillProfile(long userId) {
        User user = userService.requireUser(userId);
        return Map.of(
                "userId", user.getId(),
                "maskedName", user.getStudentNo().substring(0, Math.min(4, user.getStudentNo().length())) + "级 · " + user.getRealName().substring(0, 1) + "***",
                "role", user.getRole(),
                "skills", userService.skills(user),
                "badges", userService.badges(user)
        );
    }

    public record CreateTeamRequest(@NotBlank String title, @NotBlank String contest, @NotBlank String need, @NotEmpty List<String> skills) {
    }

    public record BatchApplicationRequest(@NotEmpty List<Long> applicationIds) {
    }

    private String toJson(List<String> skills) {
        try {
            return objectMapper.writeValueAsString(skills);
        } catch (JsonProcessingException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "技能标签格式错误");
        }
    }

    private List<String> parseSkills(String skills) {
        try {
            return objectMapper.readValue(skills, new TypeReference<>() {
            });
        } catch (JsonProcessingException error) {
            return List.of();
        }
    }
}
