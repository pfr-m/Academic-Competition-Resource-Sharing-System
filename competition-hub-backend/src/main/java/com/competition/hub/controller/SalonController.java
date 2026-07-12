package com.competition.hub.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.hub.auth.AuthPrincipal;
import com.competition.hub.entity.Salon;
import com.competition.hub.entity.SalonAppointment;
import com.competition.hub.service.ReviewService;
import com.competition.hub.service.SalonService;
import com.competition.hub.service.UserService;
import com.competition.hub.store.DomainModels.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @PreAuthorize("hasAnyRole('MENTOR','ADMIN','SUPER_ADMIN')")
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
                .status(principal.role() == Role.ADMIN || principal.role() == Role.SUPER_ADMIN ? "APPROVED" : "PENDING")
                .build();
        Salon saved = salonService.createSalon(salon);
        reviewService.createReview("salon", saved.getId(), saved.getTitle(), principal.id());
        return saved;
    }

    @PostMapping("/{id}/book")
    public SalonAppointment book(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long id) {
        return salonService.book(id, principal.id(), userService.requireUser(principal.id()).getNickname());
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
        return Map.of(
                "id", salon.getId(),
                "title", salon.getTitle(),
                "description", salon.getDescription(),
                "speaker", salon.getSpeakerName(),
                "startsAt", salon.getStartsAt().toString(),
                "startsAtText", salon.getStartsAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                "place", salon.getPlace(),
                "capacity", salon.getCapacity(),
                "booked", salon.getBookedCount(),
                "remaining", salon.getCapacity() - salon.getBookedCount()
        );
    }

    private LocalDateTime parseStartsAt(String value) {
        String normalized = value.length() == 16 ? value + ":00" : value;
        return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public record CreateSalonRequest(@NotBlank String title, @NotBlank String description, @NotBlank String startsAt, @NotBlank String place, @Min(1) int capacity) {
    }
}