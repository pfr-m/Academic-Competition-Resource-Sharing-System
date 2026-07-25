package com.competition.hub.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.competition.hub.auth.AuthPrincipal;
import com.competition.hub.entity.Blog;
import com.competition.hub.entity.BlogComment;
import com.competition.hub.service.BlogCommentService;
import com.competition.hub.service.BlogService;
import com.competition.hub.service.BlogSocialService;
import com.competition.hub.service.RedisCacheService;
import com.competition.hub.service.ReviewService;
import com.competition.hub.service.UserService;
import com.competition.hub.store.DomainModels.Role;
import com.competition.hub.store.DomainModels.Visibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/blogs")
public class BlogController {
    private final BlogService blogService;
    private final BlogCommentService blogCommentService;
    private final BlogSocialService blogSocialService;
    private final RedisCacheService redisCacheService;
    private final ReviewService reviewService;
    private final UserService userService;

    public BlogController(BlogService blogService, BlogCommentService blogCommentService, BlogSocialService blogSocialService, RedisCacheService redisCacheService, ReviewService reviewService, UserService userService) {
        this.blogService = blogService;
        this.blogCommentService = blogCommentService;
        this.blogSocialService = blogSocialService;
        this.redisCacheService = redisCacheService;
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @GetMapping
    public PageResponse<Map<String, Object>> list(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "6") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String contestTag,
            @RequestParam(required = false) String visibility
    ) {
        IPage<Blog> result = blogService.pageVisible(principal.id(), page, size, keyword, contestTag, visibility);
        List<Map<String, Object>> records = result.getRecords().stream()
                .map(blog -> presentBlog(blog, principal.id()))
                .toList();
        return PageResponse.from(result, records);
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long id) {
        Blog blog = blogService.requireReadable(id, principal.id());
        redisCacheService.incrementBlogView(id);
        Map<String, Object> cached = redisCacheService.getBlogDetail(id);
        if (cached == null) {
            cached = presentBlog(blog, principal.id(), false);
            redisCacheService.putBlogDetail(id, cached);
        }
        return withViewerSocial(cached, blog, principal.id());
    }

    @GetMapping("/creator-center")
    public Map<String, Object> creatorCenter(@AuthenticationPrincipal AuthPrincipal principal) {
        return blogSocialService.creatorStats(principal.id());
    }

    @PostMapping("/{id}/actions")
    public Map<String, Object> toggleAction(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long id, @Valid @RequestBody BlogActionRequest request) {
        blogService.requireReadable(id, principal.id());
        Map<String, Object> result = blogSocialService.toggleLikeCollect(id, principal.id(), request.type());
        redisCacheService.evictBlogDetail(id);
        return result;
    }

    @PostMapping("/{id}/follow-author")
    public Map<String, Object> toggleFollowAuthor(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long id) {
        Blog blog = blogService.requireReadable(id, principal.id());
        return blogSocialService.toggleFollow(blog.getAuthorId(), principal.id());
    }

    @PostMapping
    public Map<String, Object> create(@AuthenticationPrincipal AuthPrincipal principal, @Valid @RequestBody CreateBlogRequest request) {
        Blog blog = Blog.builder()
                .title(request.title())
                .content(request.content())
                .contestTag(normalizeContestTag(request.contestTag()))
                .authorId(principal.id())
                .authorNickname(userService.requireUser(principal.id()).getNickname())
                .anonymous(request.anonymous())
                .visibility(request.visibility().name())
                .commentsOpen(request.commentsOpen())
                .status("PENDING")
                .build();
        Blog saved = blogService.createBlog(blog);
        reviewService.createReview("blog", saved.getId(), saved.getTitle(), principal.id());
        return presentBlog(saved, principal.id());
    }

    @PostMapping("/upload-md")
    public Map<String, Object> uploadMarkdown(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam("file") MultipartFile file
    ) {
        return parseMarkdownUpload(principal, file);
    }

    private Map<String, Object> parseMarkdownUpload(AuthPrincipal principal, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择要上传的 .md 文件");
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!originalName.endsWith(".md")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "格式错误：仅支持上传标准的 .md 文件");
        }
        String markdown = readMarkdown(file);
        ParsedMarkdown parsed = extractMarkdown(markdown);
        return Map.of(
                "title", parsed.title(),
                "content", parsed.content(),
                "fileName", file.getOriginalFilename() == null ? "" : file.getOriginalFilename(),
                "authorId", principal.id()
        );
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long id, @Valid @RequestBody CreateBlogRequest request) {
        Blog blog = blogService.requireExisting(id);
        if (!blog.getAuthorId().equals(principal.id()) && principal.role() != Role.ADMIN && principal.role() != Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能编辑自己的博客");
        }
        blog.setTitle(request.title());
        blog.setContent(request.content());
        blog.setContestTag(normalizeContestTag(request.contestTag()));
        blog.setAnonymous(request.anonymous());
        blog.setVisibility(request.visibility().name());
        blog.setCommentsOpen(request.commentsOpen());
        if (principal.role() != Role.ADMIN && principal.role() != Role.SUPER_ADMIN) {
            blog.setStatus("PENDING");
            reviewService.createReview("blog", blog.getId(), blog.getTitle(), principal.id());
        }
        blogService.updateById(blog);
        redisCacheService.evictBlogDetail(id);
        return presentBlog(blog, principal.id());
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long id) {
        Blog blog = blogService.requireExisting(id);
        if (!blog.getAuthorId().equals(principal.id()) && principal.role() != Role.ADMIN && principal.role() != Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能删除自己的博客");
        }
        blogService.removeById(id);
        blogCommentService.deleteByBlogId(id);
        redisCacheService.evictBlogDetail(id);
        return Map.of("deleted", true);
    }

    @PostMapping("/{id}/comments")
    public Map<String, Object> comment(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long id, @Valid @RequestBody CreateCommentRequest request) {
        Blog blog = blogService.requireReadable(id, principal.id());
        if (!blog.getCommentsOpen()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "作者已关闭评论区");
        }
        if (request.parentId() != null) {
            BlogComment parent = blogCommentService.requireInBlog(request.parentId(), id);
            if (parent.getParentId() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "暂只支持二级回复，不能回复二级评论");
            }
        }
        BlogComment comment = BlogComment.builder()
                .blogId(id)
                .parentId(request.parentId())
                .authorId(principal.id())
                .authorNickname(userService.requireUser(principal.id()).getNickname())
                .content(request.content())
                .anonymous(request.anonymous())
                .build();
        BlogComment saved = blogCommentService.createComment(comment);
        blogService.increaseCommentCount(id);
        redisCacheService.evictBlogDetail(id);
        return presentComment(saved, List.of());
    }

    @DeleteMapping("/{blogId}/comments/{commentId}")
    public Map<String, Object> deleteComment(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long blogId, @PathVariable long commentId) {
        Blog blog = blogService.requireReadable(blogId, principal.id());
        BlogComment comment = blogCommentService.requireInBlog(commentId, blogId);
        boolean canDelete = comment.getAuthorId().equals(principal.id()) || blog.getAuthorId().equals(principal.id()) || principal.role() == Role.ADMIN || principal.role() == Role.SUPER_ADMIN;
        if (!canDelete) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权删除该评论");
        }
        blogCommentService.deleteCommentTree(commentId, blogId);
        redisCacheService.evictBlogDetail(blogId);
        return Map.of("deleted", true);
    }

    private Map<String, Object> presentBlog(Blog blog, long viewerId) {
        return presentBlog(blog, viewerId, true);
    }

    private Map<String, Object> presentBlog(Blog blog, long viewerId, boolean includeViewerSocial) {
        List<BlogComment> allComments = blogCommentService.listByBlogId(blog.getId());
        List<Map<String, Object>> comments = allComments.stream()
                .filter(comment -> comment.getParentId() == null)
                .sorted(Comparator.comparing(BlogComment::getCreatedAt))
                .map(comment -> presentComment(
                        comment,
                        allComments.stream()
                                .filter(reply -> Objects.equals(reply.getParentId(), comment.getId()))
                                .sorted(Comparator.comparing(BlogComment::getCreatedAt))
                                .map(reply -> presentComment(reply, List.of()))
                                .toList()
                ))
                .toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", blog.getId());
        payload.put("title", blog.getTitle());
        payload.put("content", blog.getContent());
        payload.put("contestTag", blog.getContestTag());
        payload.put("authorId", blog.getAuthorId());
        payload.put("anonymous", blog.getAnonymous());
        payload.put("visibility", blog.getVisibility());
        payload.put("commentsOpen", blog.getCommentsOpen());
        payload.put("status", blog.getStatus());
        payload.put("createdAt", blog.getCreatedAt().toString());
        payload.put("author", blog.getAnonymous() ? "匿名用户" : userService.safeUser(userService.requireUser(blog.getAuthorId()), viewerId));
        if (includeViewerSocial) {
            payload.put("social", blogSocialService.blogStats(blog.getId(), viewerId, blog.getAuthorId()));
        }
        payload.put("comments", comments);
        return payload;
    }

    private Map<String, Object> withViewerSocial(Map<String, Object> detail, Blog blog, long viewerId) {
        Map<String, Object> payload = new LinkedHashMap<>(detail);
        payload.put("social", blogSocialService.blogStats(blog.getId(), viewerId, blog.getAuthorId()));
        return payload;
    }

    private Map<String, Object> presentComment(BlogComment comment, List<Map<String, Object>> replies) {
        return Map.of(
                "id", comment.getId(),
                "content", comment.getContent(),
                "author", comment.getAnonymous() ? "匿名用户" : comment.getAuthorNickname(),
                "createdAt", comment.getCreatedAt().toString(),
                "replies", replies
        );
    }

    private String normalizeContestTag(String contestTag) {
        return contestTag == null || contestTag.isBlank() ? "经验分享" : contestTag.trim();
    }

    private String readMarkdown(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法读取 Markdown 文件内容");
        }
    }

    private ParsedMarkdown extractMarkdown(String markdown) {
        String normalized = markdown == null ? "" : markdown.trim();
        if (normalized.isEmpty()) {
            return new ParsedMarkdown("", "");
        }
        String[] lines = normalized.split("\\R", -1);
        String title = "";
        int contentStart = 0;
        if (lines.length > 0 && lines[0].startsWith("# ")) {
            title = lines[0].substring(2).trim();
            contentStart = 1;
            while (contentStart < lines.length && lines[contentStart].isBlank()) {
                contentStart++;
            }
        }
        StringBuilder content = new StringBuilder();
        for (int i = contentStart; i < lines.length; i++) {
            if (content.length() > 0) {
                content.append('\n');
            }
            content.append(lines[i]);
        }
        if (title.isBlank()) {
            title = inferTitleFromMarkdown(normalized);
        }
        return new ParsedMarkdown(title, content.toString().trim());
    }

    private String inferTitleFromMarkdown(String markdown) {
        for (String line : markdown.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("# ")) {
                return trimmed.substring(2).trim();
            }
            return trimmed.length() > 60 ? trimmed.substring(0, 60) : trimmed;
        }
        return "";
    }

    public record CreateBlogRequest(@NotBlank String title, @NotBlank String content, String contestTag, boolean anonymous, @NotNull Visibility visibility, boolean commentsOpen) {
    }

    public record CreateCommentRequest(@NotBlank String content, boolean anonymous, Long parentId) {
    }

    public record BlogActionRequest(@NotBlank String type) {
    }

    private record ParsedMarkdown(String title, String content) {
    }
}
