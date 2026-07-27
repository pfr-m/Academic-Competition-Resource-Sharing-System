package com.competition.hub.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.hub.auth.AuthPrincipal;
import com.competition.hub.entity.Salon;
import com.competition.hub.entity.SalonAppointment;
import com.competition.hub.service.ReviewService;
import com.competition.hub.service.SalonService;
import com.competition.hub.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/salons")
public class SalonController {
    private final SalonService salonService;
    private final ReviewService reviewService;
    private final UserService userService;

    public SalonController(SalonService salonService, ReviewService reviewService, UserService userService) {
        this.salonService = salonService;
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @GetMapping
    public PageResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "6") long size
    ) {
        IPage<Salon> result = salonService.pageApproved(page, size);
        List<Map<String, Object>> records = result.getRecords().stream()
                .map(this::presentSalon)
                .toList();
        return PageResponse.from(result, records);
    }

    @PreAuthorize("hasAnyRole('STUDENT','MENTOR','ADMIN','SUPER_ADMIN')")
    @PostMapping
    public Salon create(@AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody CreateSalonRequest request) {
        Salon salon = Salon.builder()
                .title(request.title())
                .description(request.description())
                .speakerId(principal.id())
                .speakerName(userService.requireUser(principal.id()).getNickname())
                .startsAt(parseStartsAt(request.startsAt()))
                .place(request.place())
                .capacity(request.capacity())
                .status(isReviewer(principal) ? "APPROVED" : "PENDING")
                .build();
        Salon saved = salonService.createSalon(salon);
        if (!isReviewer(principal)) {
            reviewService.createReview("salon", saved.getId(), saved.getTitle(), principal.id());
        }
        return saved;
    }

    @PostMapping("/{id}/book")
    public SalonAppointment book(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long id) {
        return salonService.book(id, principal.id(), userService.requireUser(principal.id()).getNickname());
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long id) {
        Salon salon = salonService.getById(id);
        if (salon == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "沙龙不存在");
        }
        boolean admin = "ADMIN".equals(principal.role().name()) || "SUPER_ADMIN".equals(principal.role().name());
        boolean owner = salon.getSpeakerId() != null && salon.getSpeakerId().equals(principal.id());
        if (!admin && !owner) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "只能删除自己发布的沙龙");
        }
        salonService.deleteSalon(id);
        return Map.of("deleted", true);
    }

    @GetMapping("/me/bookings")
    public List<Map<String, Object>> myBookings(@AuthenticationPrincipal AuthPrincipal principal) {
        return salonService.listAppointmentsByUser(principal.id()).stream()
                .map(appointment -> Map.<String, Object>of(
                        "id", appointment.getId(),
                        "certificateCode", appointment.getCertificateCode(),
                        "createdAt", appointment.getCreatedAt().toString(),
                        "salon", presentSalon(salonService.getById(appointment.getSalonId()))
                ))
                .toList();
    }

    private Map<String, Object> presentSalon(Salon salon) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", salon.getId());
        payload.put("title", salon.getTitle());
        payload.put("description", salon.getDescription());
        payload.put("speakerId", salon.getSpeakerId());
        payload.put("speaker", salon.getSpeakerName());
        payload.put("startsAt", salon.getStartsAt().toString());
        payload.put("startsAtText", salon.getStartsAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        payload.put("place", salon.getPlace());
        payload.put("capacity", salon.getCapacity());
        payload.put("booked", salon.getBookedCount());
        payload.put("remaining", salon.getCapacity() - salon.getBookedCount());
        return payload;
    }

    private LocalDateTime parseStartsAt(String value) {
        String normalized = value.length() == 16 ? value + ":00" : value;
        return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private boolean isReviewer(AuthPrincipal principal) {
        return "ADMIN".equals(principal.role().name()) || "SUPER_ADMIN".equals(principal.role().name());
    }

    public record CreateSalonRequest(@NotBlank String title, @NotBlank String description, @NotBlank String startsAt, @NotBlank String place, @Min(1) int capacity) {
    }
}
