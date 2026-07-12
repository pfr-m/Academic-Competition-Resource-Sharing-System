package com.competition.hub.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.hub.store.DomainModels.ReviewStatus;
import com.competition.hub.entity.Review;
import com.competition.hub.entity.Salon;
import com.competition.hub.entity.SalonAppointment;
import com.competition.hub.service.BlogService;
import com.competition.hub.service.ReviewService;
import com.competition.hub.service.ResourceService;
import com.competition.hub.service.SalonService;
import com.competition.hub.service.TeamService;
import com.competition.hub.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class AdminController {
    private final ResourceService resourceService;
    private final BlogService blogService;
    private final SalonService salonService;
    private final TeamService teamService;
    private final ReviewService reviewService;
    private final UserService userService;

    public AdminController(ResourceService resourceService, BlogService blogService, SalonService salonService, TeamService teamService, ReviewService reviewService, UserService userService) {
        this.resourceService = resourceService;
        this.blogService = blogService;
        this.salonService = salonService;
        this.teamService = teamService;
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @GetMapping("/reviews")
    public PageResponse<Map<String, Object>> reviews(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "8") long size
    ) {
        IPage<Review> result = reviewService.pageLatest(page, size);
        List<Map<String, Object>> records = result.getRecords().stream()
                .map(review -> Map.<String, Object>of(
                        "id", review.getId(),
                        "targetType", review.getTargetType(),
                        "targetId", review.getTargetId(),
                        "title", review.getTitle(),
                        "owner", userService.requireUser(review.getOwnerId()).getNickname(),
                        "status", review.getStatus(),
                        "createdAt", review.getCreatedAt().toString()
                ))
                .toList();
        return PageResponse.from(result, records);
    }

    @PatchMapping("/reviews/{id}")
    public Review review(@PathVariable long id, @Valid @RequestBody ReviewRequest request) {
        Review updated = reviewService.updateStatus(id, request.status().name());
        applyStatus(updated.getTargetType(), updated.getTargetId(), request.status());
        return updated;
    }

    @PostMapping("/reviews/batch-delete")
    public Map<String, Object> deleteReviews(@Valid @RequestBody BatchDeleteRequest request) {
        reviewService.removeByIds(request.ids());
        return Map.of("deleted", request.ids().size());
    }

    @PatchMapping("/reviews/batch")
    public Map<String, Object> reviewBatch(@Valid @RequestBody BatchReviewRequest request) {
        request.ids().forEach(id -> {
            Review updated = reviewService.updateStatus(id, request.status().name());
            applyStatus(updated.getTargetType(), updated.getTargetId(), request.status());
        });
        return Map.of("updated", request.ids().size(), "status", request.status().name());
    }

    @GetMapping("/matchmaking")
    public List<Map<String, Object>> matchmaking() {
        return userService.lambdaQuery()
                .eq(com.competition.hub.entity.User::getRole, "STUDENT")
                .list()
                .stream()
                .map(this::matchProfile)
                .toList();
    }

    @GetMapping("/salon-appointments")
    public List<Map<String, Object>> salonAppointments() {
        return salonService.listAllAppointments().stream()
                .map(this::presentAppointment)
                .toList();
    }

    @DeleteMapping("/salon-appointments/{id}")
    public Map<String, Object> deleteSalonAppointment(@PathVariable long id) {
        salonService.deleteAppointment(id);
        return Map.of("deleted", true);
    }

    @PatchMapping("/salon-appointments/{id}/cancel")
    public Map<String, Object> cancelSalonAppointment(@PathVariable long id) {
        salonService.cancelAppointment(id);
        return Map.of("cancelled", true);
    }

    private Map<String, Object> matchProfile(com.competition.hub.entity.User user) {
        return Map.of(
                "userId", user.getId(),
                "maskedName", user.getStudentNo().substring(0, Math.min(4, user.getStudentNo().length())) + "级 · " + user.getRealName().substring(0, 1) + "***",
                "skills", userService.skills(user)
        );
    }

    private Map<String, Object> presentAppointment(SalonAppointment appointment) {
        Salon salon = salonService.getById(appointment.getSalonId());
        return Map.of(
                "id", appointment.getId(),
                "salonId", appointment.getSalonId(),
                "salonTitle", salon == null ? "沙龙已删除" : salon.getTitle(),
                "userId", appointment.getUserId(),
                "userNickname", appointment.getUserNickname(),
                "status", appointment.getStatus(),
                "certificateCode", appointment.getCertificateCode(),
                "createdAt", appointment.getCreatedAt().toString()
        );
    }

    private void applyStatus(String targetType, long targetId, ReviewStatus status) {
        if ("resource".equals(targetType)) {
            resourceService.updateStatus(targetId, status.name());
        }
        if ("blog".equals(targetType)) {
            blogService.updateStatus(targetId, status.name());
        }
        if ("salon".equals(targetType)) {
            salonService.updateStatus(targetId, status.name());
        }
        if ("team".equals(targetType)) {
            teamService.updateStatus(targetId, status.name());
        }
    }

    public record ReviewRequest(@NotNull ReviewStatus status) {
    }

    public record BatchDeleteRequest(@NotEmpty List<Long> ids) {
    }

    public record BatchReviewRequest(@NotEmpty List<Long> ids, @NotNull ReviewStatus status) {
    }
}