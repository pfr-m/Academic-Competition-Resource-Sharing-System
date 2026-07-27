package com.competition.hub.controller;

import com.competition.hub.auth.AuthPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/blog")
public class BlogUploadController {
    private final BlogController blogController;

    public BlogUploadController(BlogController blogController) {
        this.blogController = blogController;
    }

    @PostMapping("/upload-md")
    public Map<String, Object> uploadMarkdown(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "files", required = false) List<MultipartFile> files
    ) {
        return blogController.uploadMarkdown(principal, file, files);
    }
}
