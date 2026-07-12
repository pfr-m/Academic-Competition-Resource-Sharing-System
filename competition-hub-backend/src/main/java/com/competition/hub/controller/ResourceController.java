package com.competition.hub.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.hub.auth.AuthPrincipal;
import com.competition.hub.entity.Resource;
import com.competition.hub.service.ReviewService;
import com.competition.hub.service.RedisCacheService;
import com.competition.hub.service.ResourceService;
import com.competition.hub.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/resources")
public class ResourceController {
    private final ResourceService resourceService;
    private final ReviewService reviewService;
    private final RedisCacheService redisCacheService;
    private final UserService userService;
    private final Path uploadRoot = Paths.get("uploads", "resources").toAbsolutePath().normalize();

    public ResourceController(ResourceService resourceService, ReviewService reviewService, RedisCacheService redisCacheService, UserService userService) {
        this.resourceService = resourceService;
        this.reviewService = reviewService;
        this.redisCacheService = redisCacheService;
        this.userService = userService;
    }

    @GetMapping
    public PageResponse<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String contest,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "8") long size
    ) {
        if (contest != null && !contest.isBlank()
                && (keyword == null || keyword.isBlank())
                && (type == null || type.isBlank())
                && page == 1) {
            List<Map<String, Object>> cached = redisCacheService.getContestResourceCache(contest);
            if (cached != null) {
                long end = Math.min(size, cached.size());
                return new PageResponse<>(cached.subList(0, (int) end), cached.size(), page, size, (long) Math.ceil(cached.size() / (double) size));
            }
            List<Map<String, Object>> records = resourceService.listApproved(null, contest, null).stream()
                    .map(this::presentResource)
                    .toList();
            redisCacheService.putContestResourceCache(contest, records);
            long end = Math.min(size, records.size());
            return new PageResponse<>(records.subList(0, (int) end), records.size(), page, size, (long) Math.ceil(records.size() / (double) size));
        }
        IPage<Resource> result = resourceService.pageApproved(page, size, keyword, contest, type);
        List<Map<String, Object>> records = result.getRecords().stream()
                .map(this::presentResource)
                .toList();
        return PageResponse.from(result, records);
    }

    @PreAuthorize("hasAnyRole('MENTOR','ADMIN','SUPER_ADMIN')")
    @PostMapping
    public Resource create(@AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody CreateResourceRequest request) {
        Resource item = new Resource();
        item.setTitle(request.title());
        item.setContest(request.contest());
        item.setType(request.type());
        item.setSummary(request.summary());
        item.setUploaderId(principal.id());
        item.setUploaderName(userService.requireUser(principal.id()).getNickname());
        item.setFileKey(request.fileKey());
        item.setOriginalFileName(request.fileKey());
        item.setFileSize(0L);
        item.setContentType("application/octet-stream");
        item.setStatus(principal.role().name().equals("ADMIN") || principal.role().name().equals("SUPER_ADMIN") ? "APPROVED" : "PENDING");
        Resource saved = resourceService.createResource(item);
        redisCacheService.evictContestResourceCache(saved.getContest());
        reviewService.createReview("resource", saved.getId(), saved.getTitle(), principal.id());
        return saved;
    }

    @PreAuthorize("hasAnyRole('MENTOR','ADMIN','SUPER_ADMIN')")
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Resource upload(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam String title,
            @RequestParam String contest,
            @RequestParam String type,
            @RequestParam String summary,
            @RequestParam MultipartFile file
    ) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择要上传的文件");
        }
        String originalName = file.getOriginalFilename() == null ? "resource-file" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String storedName = UUID.randomUUID() + "-" + originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
        try {
            Files.createDirectories(uploadRoot);
            file.transferTo(uploadRoot.resolve(storedName));
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "文件保存失败");
        }

        Resource item = new Resource();
        item.setTitle(title);
        item.setContest(contest);
        item.setType(type);
        item.setSummary(summary);
        item.setUploaderId(principal.id());
        item.setUploaderName(userService.requireUser(principal.id()).getNickname());
        item.setFileKey(storedName);
        item.setOriginalFileName(originalName);
        item.setFileSize(file.getSize());
        item.setContentType(file.getContentType());
        item.setStatus(principal.role().name().equals("ADMIN") || principal.role().name().equals("SUPER_ADMIN") ? "APPROVED" : "PENDING");
        Resource saved = resourceService.createResource(item);
        redisCacheService.evictContestResourceCache(saved.getContest());
        reviewService.createReview("resource", saved.getId(), saved.getTitle(), principal.id());
        return saved;
    }

    @PostMapping("/{id}/download")
    public Map<String, Object> download(@PathVariable long id) {
        Resource updated = resourceService.increaseDownloadCount(id);
        redisCacheService.incrementResourceDownload(updated.getId());
        return Map.of(
                "downloadUrl", "/api/resources/" + updated.getId() + "/file",
                "expiresInSeconds", 300,
                "downloadCount", updated.getDownloadCount()
        );
    }

    @PutMapping("/{id}")
    public Resource update(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long id, @Valid @RequestBody UpdateResourceRequest request) {
        Resource item = resourceService.requireExisting(id);
        assertCanManage(principal, item);
        String oldContest = item.getContest();
        item.setTitle(request.title());
        item.setContest(request.contest());
        item.setType(request.type());
        item.setSummary(request.summary());
        item.setUpdatedAt(LocalDateTime.now());
        resourceService.updateById(item);
        redisCacheService.evictContestResourceCache(oldContest);
        redisCacheService.evictContestResourceCache(item.getContest());
        return item;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long id) {
        Resource item = resourceService.requireExisting(id);
        assertCanManage(principal, item);
        resourceService.removeById(id);
        redisCacheService.evictContestResourceCache(item.getContest());
        return Map.of("deleted", true);
    }

    @GetMapping("/hot-ranking")
    public List<Map<String, Object>> hotRanking() {
        return redisCacheService.hotResourceRanking();
    }

    @PostMapping("/batch-delete")
    public Map<String, Object> deleteBatch(@AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody BatchDeleteRequest request) {
        return doDeleteBatch(principal, request.ids());
    }

    @DeleteMapping("/batch")
    public Map<String, Object> deleteBatchByDelete(@AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody BatchDeleteRequest request) {
        return doDeleteBatch(principal, request.ids());
    }

    private Map<String, Object> doDeleteBatch(AuthPrincipal principal, List<Long> ids) {
        List<Resource> items = ids.stream()
                .map(resourceService::requireExisting)
                .toList();
        items.forEach(item -> assertCanManage(principal, item));
        resourceService.removeByIds(ids);
        items.forEach(item -> redisCacheService.evictContestResourceCache(item.getContest()));
        return Map.of("deleted", ids.size());
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<org.springframework.core.io.Resource> file(@PathVariable long id) {
        Resource item = resourceService.requireApproved(id);
        Path filePath = uploadRoot.resolve(item.getFileKey()).normalize();
        if (!filePath.startsWith(uploadRoot) || !Files.exists(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文件不存在");
        }
        try {
            org.springframework.core.io.Resource resource = new UrlResource(filePath.toUri());
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            if (item.getContentType() != null) {
                try {
                    mediaType = MediaType.parseMediaType(item.getContentType());
                } catch (Exception e) {
                }
            }
            String contentDisposition = ContentDisposition.attachment()
                    .filename(item.getOriginalFileName(), java.nio.charset.StandardCharsets.UTF_8)
                    .build()
                    .toString();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .contentType(mediaType)
                    .body(resource);
        } catch (MalformedURLException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "文件读取失败");
        }
    }

    private Map<String, Object> presentResource(Resource item) {
        return Map.of(
                "id", item.getId(),
                "title", item.getTitle(),
                "contest", item.getContest(),
                "type", item.getType(),
                "summary", item.getSummary(),
                "uploaderId", item.getUploaderId(),
                "uploader", item.getUploaderName(),
                "downloadCount", item.getDownloadCount(),
                "createdAt", item.getCreatedAt().toString()
        );
    }

    private void assertCanManage(AuthPrincipal principal, Resource item) {
        boolean admin = "ADMIN".equals(principal.role().name()) || "SUPER_ADMIN".equals(principal.role().name());
        boolean owner = item.getUploaderId() != null && item.getUploaderId().equals(principal.id());
        if (!admin && !owner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能管理自己上传的资源");
        }
    }

    public record CreateResourceRequest(
            @NotBlank String title,
            @NotBlank String contest,
            @NotBlank String type,
            @NotBlank String summary,
            @NotBlank String fileKey
    ) {
    }

    public record UpdateResourceRequest(
            @NotBlank String title,
            @NotBlank String contest,
            @NotBlank String type,
            @NotBlank String summary
    ) {
    }

    public record BatchDeleteRequest(@NotEmpty List<Long> ids) {
    }
}