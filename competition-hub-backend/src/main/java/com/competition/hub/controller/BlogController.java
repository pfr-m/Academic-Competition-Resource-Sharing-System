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
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/blogs")
public class BlogController {
    private final BlogService blogService;
    private final BlogCommentService blogCommentService;
    private final BlogSocialService blogSocialService;
    private final RedisCacheService redisCacheService;
    private final ReviewService reviewService;
    private final UserService userService;
    private final Path imageRoot = Paths.get("uploads", "blog-images").toAbsolutePath().normalize();
    private final HttpClient imageHttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

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
        IPage<Blog> result = blogService.pageVisible(principal.id(), isReviewer(principal), page, size, keyword, contestTag, visibility);
        List<Map<String, Object>> records = result.getRecords().stream()
                .map(blog -> presentBlog(blog, principal.id()))
                .toList();
        return PageResponse.from(result, records);
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long id) {
        Blog blog = blogService.requireReadable(id, principal.id(), isReviewer(principal));
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
        blogService.requireReadable(id, principal.id(), isReviewer(principal));
        Map<String, Object> result = blogSocialService.toggleLikeCollect(id, principal.id(), request.type());
        redisCacheService.evictBlogDetail(id);
        return result;
    }

    @PostMapping("/{id}/follow-author")
    public Map<String, Object> toggleFollowAuthor(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long id) {
        Blog blog = blogService.requireReadable(id, principal.id(), isReviewer(principal));
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
                .status(isReviewer(principal) ? "APPROVED" : "PENDING")
                .build();
        Blog saved = blogService.createBlog(blog);
        if (!isReviewer(principal)) {
            reviewService.createReview("blog", saved.getId(), saved.getTitle(), principal.id());
        }
        return presentBlog(saved, principal.id());
    }

    @PostMapping("/upload-md")
    public Map<String, Object> uploadMarkdown(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "files", required = false) List<MultipartFile> files
    ) {
        return parseMarkdownUpload(principal, file, files == null ? List.of() : files);
    }

    @PostMapping(path = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择要上传的图片");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持上传图片文件");
        }
        String originalName = file.getOriginalFilename() == null ? "blog-image" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String storedName = UUID.randomUUID() + "-" + originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
        try {
            Files.createDirectories(imageRoot);
            file.transferTo(imageRoot.resolve(storedName));
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "图片保存失败");
        }
        return Map.of("url", "/api/blogs/images/" + storedName, "fileName", originalName);
    }

    @GetMapping("/images/{fileName:.+}")
    public ResponseEntity<org.springframework.core.io.Resource> image(@PathVariable String fileName) {
        Path filePath = imageRoot.resolve(fileName).normalize();
        if (!filePath.startsWith(imageRoot) || !Files.exists(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图片不存在");
        }
        try {
            org.springframework.core.io.Resource image = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            MediaType mediaType = contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(fileName, StandardCharsets.UTF_8).build().toString())
                    .contentType(mediaType)
                    .body(image);
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "图片读取失败");
        }
    }

    public Map<String, Object> parseMarkdownUpload(AuthPrincipal principal, MultipartFile file, List<MultipartFile> attachments) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择要上传的博客文件");
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!isSupportedBlogFile(originalName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "格式错误：仅支持上传 .md、.txt、.docx、.pdf 文件");
        }
        String markdown = readBlogFile(file, originalName);
        if (originalName.endsWith(".md")) {
            markdown = rewriteUploadedMarkdownImages(markdown, attachments);
            markdown = rewriteRemoteMarkdownImages(markdown);
        }
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
        deleteBlogImages(blog.getContent());
        redisCacheService.evictBlogDetail(id);
        return Map.of("deleted", true);
    }

    @PostMapping("/{id}/comments")
    public Map<String, Object> comment(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable long id, @Valid @RequestBody CreateCommentRequest request) {
        Blog blog = blogService.requireReadable(id, principal.id(), isReviewer(principal));
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
        Blog blog = blogService.requireReadable(blogId, principal.id(), isReviewer(principal));
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

    private boolean isReviewer(AuthPrincipal principal) {
        return principal.role() == Role.ADMIN || principal.role() == Role.SUPER_ADMIN;
    }

    private boolean isSupportedBlogFile(String originalName) {
        return originalName.endsWith(".md") || originalName.endsWith(".txt") || originalName.endsWith(".docx") || originalName.endsWith(".pdf");
    }

    private String readBlogFile(MultipartFile file, String originalName) {
        if (originalName.endsWith(".docx")) {
            return readDocx(file);
        }
        if (originalName.endsWith(".pdf")) {
            return readPdf(file);
        }
        return readText(file);
    }

    private String readText(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String utf8 = stripBom(new String(bytes, StandardCharsets.UTF_8));
            if (looksMisdecoded(utf8)) {
                return stripBom(new String(bytes, Charset.forName("GBK")));
            }
            return utf8;
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法读取文件内容");
        }
    }

    private String stripBom(String text) {
        return text == null ? "" : text.replaceFirst("^\\uFEFF", "");
    }

    private boolean looksMisdecoded(String text) {
        if (text == null || text.isBlank()) return false;
        long replacement = text.chars().filter(ch -> ch == '\uFFFD').count();
        long mojibake = text.chars().filter(ch -> ch == '锟' || ch == '�').count();
        return replacement + mojibake > 0 || text.contains("鍩") || text.contains("涓") || text.contains("銆");
    }

    private String readDocx(MultipartFile file) {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            StringBuilder content = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                StringBuilder paragraphContent = new StringBuilder();
                for (XWPFRun run : paragraph.getRuns()) {
                    String text = run.text();
                    if (text != null && !text.isEmpty()) {
                        paragraphContent.append(text);
                    }
                    for (XWPFPicture picture : run.getEmbeddedPictures()) {
                        XWPFPictureData pictureData = picture.getPictureData();
                        if (pictureData == null) {
                            continue;
                        }
                        String imageUrl = storeBlogImage(
                                pictureData.getData(),
                                pictureData.getFileName()
                        );
                        String alt = pictureData.getFileName() == null || pictureData.getFileName().isBlank()
                                ? "Word 图片"
                                : pictureData.getFileName();
                        paragraphContent.append("![")
                                .append(escapeMarkdownLabel(alt))
                                .append("](")
                                .append(imageUrl)
                                .append(")");
                    }
                }
                if (!paragraphContent.isEmpty()) {
                    if (!content.isEmpty()) {
                        content.append("\n\n");
                    }
                    content.append(paragraphContent);
                }
            }
            return content.toString();
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法解析 Word 文件内容");
        }
    }

    private String readPdf(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            return new PDFTextStripper().getText(document).trim();
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法解析 PDF 文件内容");
        }
    }

    private String rewriteUploadedMarkdownImages(String markdown, List<MultipartFile> attachments) {
        Map<String, String> uploadedImages = attachments.stream()
                .filter(file -> file != null && !file.isEmpty() && isImageUpload(file))
                .collect(Collectors.toMap(file -> Paths.get(file.getOriginalFilename() == null ? "" : file.getOriginalFilename()).getFileName().toString(), this::storeBlogImage, (first, second) -> first));
        if (uploadedImages.isEmpty()) {
            return markdown;
        }
        String rewritten = replaceMarkdownImageLinks(markdown, uploadedImages);
        return replaceHtmlImageLinks(rewritten, uploadedImages);
    }

    private boolean isImageUpload(MultipartFile file) {
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        return contentType.startsWith("image/") || name.matches(".*\\.(png|jpg|jpeg|gif|webp|bmp)$");
    }

    private String storeBlogImage(MultipartFile file) {
        String originalName = file.getOriginalFilename() == null ? "blog-image" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        try {
            return storeBlogImage(file.getBytes(), originalName);
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "图片保存失败");
        }
    }

    private String storeBlogImage(byte[] data, String originalName) {
        String safeOriginalName = originalName == null || originalName.isBlank() ? "blog-image" : Paths.get(originalName).getFileName().toString();
        String storedName = UUID.randomUUID() + "-" + safeOriginalName.replaceAll("[\\\\/:*?\"<>|]", "_");
        try {
            Files.createDirectories(imageRoot);
            Files.write(imageRoot.resolve(storedName), data);
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "图片保存失败");
        }
        return "/api/blogs/images/" + storedName;
    }

    private String escapeMarkdownLabel(String label) {
        return label.replace("[", "\\[").replace("]", "\\]");
    }

    private String replaceMarkdownImageLinks(String markdown, Map<String, String> uploadedImages) {
        Matcher matcher = Pattern.compile("!\\[([^\\]]*)]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)").matcher(markdown);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String imageName = Paths.get(matcher.group(2).replace("\\", "/")).getFileName().toString();
            String serverUrl = uploadedImages.get(imageName);
            if (serverUrl == null) continue;
            matcher.appendReplacement(output, Matcher.quoteReplacement("![" + matcher.group(1) + "](" + serverUrl + ")"));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private String replaceHtmlImageLinks(String markdown, Map<String, String> uploadedImages) {
        Matcher matcher = Pattern.compile("(?i)<img\\b([^>]*?)\\bsrc\\s*=\\s*(['\"])(.*?)\\2([^>]*)>").matcher(markdown);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String imageName = Paths.get(matcher.group(3).replace("\\", "/")).getFileName().toString();
            String serverUrl = uploadedImages.get(imageName);
            if (serverUrl == null) continue;
            matcher.appendReplacement(output, Matcher.quoteReplacement("<img" + matcher.group(1) + "src=" + matcher.group(2) + serverUrl + matcher.group(2) + matcher.group(4) + ">"));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private String rewriteRemoteMarkdownImages(String markdown) {
        String rewritten = replaceRemoteMarkdownImageLinks(markdown);
        return replaceRemoteHtmlImageLinks(rewritten);
    }

    private String replaceRemoteMarkdownImageLinks(String markdown) {
        Matcher matcher = Pattern.compile("!\\[([^\\]]*)]\\((https?://[^)\\s]+)(?:\\s+\"[^\"]*\")?\\)").matcher(markdown);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String serverUrl = downloadRemoteImage(matcher.group(2));
            if (serverUrl == null) continue;
            matcher.appendReplacement(output, Matcher.quoteReplacement("![" + matcher.group(1) + "](" + serverUrl + ")"));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private String replaceRemoteHtmlImageLinks(String markdown) {
        Matcher matcher = Pattern.compile("(?i)<img\\b([^>]*?)\\bsrc\\s*=\\s*(['\"])(https?://.*?)\\2([^>]*)>").matcher(markdown);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String serverUrl = downloadRemoteImage(matcher.group(3));
            if (serverUrl == null) continue;
            matcher.appendReplacement(output, Matcher.quoteReplacement("<img" + matcher.group(1) + "src=" + matcher.group(2) + serverUrl + matcher.group(2) + matcher.group(4) + ">"));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private String downloadRemoteImage(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = imageHttpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            String contentType = response.headers().firstValue("content-type").orElse("").toLowerCase();
            String extension = imageExtension(url, contentType);
            String storedName = UUID.randomUUID() + "-remote" + extension;
            Files.createDirectories(imageRoot);
            Files.copy(response.body(), imageRoot.resolve(storedName));
            return "/api/blogs/images/" + storedName;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String imageExtension(String url, String contentType) {
        String lowerUrl = url.toLowerCase();
        for (String extension : List.of(".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp")) {
            if (lowerUrl.contains(extension)) return extension;
        }
        if (contentType.contains("png")) return ".png";
        if (contentType.contains("jpeg") || contentType.contains("jpg")) return ".jpg";
        if (contentType.contains("gif")) return ".gif";
        if (contentType.contains("webp")) return ".webp";
        if (contentType.contains("bmp")) return ".bmp";
        return ".png";
    }

    private void deleteBlogImages(String content) {
        if (content == null || content.isBlank()) return;
        Matcher matcher = Pattern.compile("/api/blogs/images/([^\\s)\"'>]+)").matcher(content);
        while (matcher.find()) {
            Path imagePath = imageRoot.resolve(matcher.group(1)).normalize();
            if (imagePath.startsWith(imageRoot)) {
                try {
                    Files.deleteIfExists(imagePath);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private ParsedMarkdown extractMarkdown(String markdown) {
        String normalized = markdown == null ? "" : markdown.replaceFirst("^\\uFEFF", "").trim();
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
