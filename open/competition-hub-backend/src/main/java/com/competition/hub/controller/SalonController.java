package com.competition.hub.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.hub.auth.AuthPrincipal;
import com.competition.hub.entity.Salon;
import com.competition.hub.entity.SalonAppointment;
import com.competition.hub.service.ReviewService;
import com.competition.hub.service.SalonService;
import com.competition.hub.service.UserService;
import com.competition.hub.store.DomainModels.Role;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/salons")
public class SalonController {
    private final SalonService salonService;
    private final ReviewService reviewService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final Path uploadRoot = Paths.get("uploads", "salons").toAbsolutePath().normalize();

    public SalonController(SalonService salonService, ReviewService reviewService, UserService userService, ObjectMapper objectMapper) {
        this.salonService = salonService;
        this.reviewService = reviewService;
        this.userService = userService;
        this.objectMapper = objectMapper;
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
        String imagesJson = null;
        if (request.images() != null && !request.images().isEmpty()) {
            try {
                imagesJson = objectMapper.writeValueAsString(request.images());
            } catch (JsonProcessingException ignored) {}
        }
        Salon salon = Salon.builder()
                .title(request.title())
                .description(request.description())
                .speakerId(principal.id())
                .speakerName(userService.requireUser(principal.id()).getNickname())
                .startsAt(parseStartsAt(request.startsAt()))
                .place(request.place())
                .capacity(request.capacity())
                .images(imagesJson)
                .status(principal.role() == Role.ADMIN || principal.role() == Role.SUPER_ADMIN ? "APPROVED" : "PENDING")
                .build();
        Salon saved = salonService.createSalon(salon);
        reviewService.createReview("salon", saved.getId(), saved.getTitle(), principal.id());
        return saved;
    }

    @PreAuthorize("hasAnyRole('MENTOR','ADMIN','SUPER_ADMIN')")
    @PostMapping(path = "/upload-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadImages(@RequestParam List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择要上传的图片");
        }
        List<String> urls = files.stream().map(file -> {
            if (file.isEmpty()) return null;
            String originalName = file.getOriginalFilename() == null ? "salon-image" : Paths.get(file.getOriginalFilename()).getFileName().toString();
            String storedName = UUID.randomUUID() + "-" + originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
            try {
                Files.createDirectories(uploadRoot);
                file.transferTo(uploadRoot.resolve(storedName));
            } catch (IOException error) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "图片保存失败");
            }
            return "/uploads/salons/" + storedName;
        }).filter(u -> u != null).toList();
        return Map.of("urls", urls);
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
        List<String> images = List.of();
        if (salon.getImages() != null && !salon.getImages().isBlank()) {
            try {
                images = objectMapper.readValue(salon.getImages(), List.class);
            } catch (JsonProcessingException ignored) {}
        }
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
                "remaining", salon.getCapacity() - salon.getBookedCount(),
                "images", images
        );
    }

    private LocalDateTime parseStartsAt(String value) {
        String normalized = value.length() == 16 ? value + ":00" : value;
        return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public record CreateSalonRequest(@NotBlank String title, @NotBlank String description, @NotBlank String startsAt, @NotBlank String place, @Min(1) int capacity, List<String> images) {
    }
}