package com.competition.hub.controller;

import com.competition.hub.auth.AuthPrincipal;
import com.competition.hub.entity.Blog;
import com.competition.hub.entity.Resource;
import com.competition.hub.entity.Salon;
import com.competition.hub.entity.User;
import com.competition.hub.service.BlogCommentService;
import com.competition.hub.service.BlogService;
import com.competition.hub.service.BlogSocialService;
import com.competition.hub.service.RedisCacheService;
import com.competition.hub.service.ResourceService;
import com.competition.hub.service.ReviewService;
import com.competition.hub.service.SalonService;
import com.competition.hub.service.UserService;
import com.competition.hub.store.DomainModels.Role;
import com.competition.hub.store.DomainModels.Visibility;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContentSubmissionControllerTest {

    @Test
    void studentCanSubmitSalonForReview() throws NoSuchMethodException {
        PreAuthorize authorization = org.springframework.core.annotation.AnnotationUtils.findAnnotation(
                SalonController.class.getMethod("create", AuthPrincipal.class, SalonController.CreateSalonRequest.class),
                PreAuthorize.class
        );
        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).contains("STUDENT");

        SalonService salonService = mock(SalonService.class);
        ReviewService reviewService = mock(ReviewService.class);
        UserService userService = mock(UserService.class);
        SalonController controller = new SalonController(salonService, reviewService, userService);
        AuthPrincipal student = new AuthPrincipal(7L, "student", Role.STUDENT);
        when(userService.requireUser(7L)).thenReturn(User.builder().id(7L).nickname("学生").build());
        when(salonService.createSalon(any(Salon.class))).thenAnswer(invocation -> {
            Salon salon = invocation.getArgument(0);
            salon.setId(21L);
            return salon;
        });

        Salon saved = controller.create(student, new SalonController.CreateSalonRequest(
                "算法沙龙", "普通学生提交沙龙", "2026-08-01T19:00", "A204", 30
        ));

        assertThat(saved.getSpeakerId()).isEqualTo(7L);
        assertThat(saved.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void adminBlogSubmissionIsApprovedImmediately() {
        BlogService blogService = mock(BlogService.class);
        BlogCommentService blogCommentService = mock(BlogCommentService.class);
        BlogSocialService blogSocialService = mock(BlogSocialService.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        ReviewService reviewService = mock(ReviewService.class);
        UserService userService = mock(UserService.class);
        BlogController controller = new BlogController(blogService, blogCommentService, blogSocialService, redisCacheService, reviewService, userService);
        AuthPrincipal admin = new AuthPrincipal(3L, "admin", Role.ADMIN);
        when(userService.requireUser(3L)).thenReturn(User.builder().id(3L).nickname("管理员").build());
        when(blogService.createBlog(any(Blog.class))).thenAnswer(invocation -> {
            Blog blog = invocation.getArgument(0);
            blog.setId(31L);
            blog.setCreatedAt(LocalDateTime.now());
            return blog;
        });
        when(blogCommentService.listByBlogId(31L)).thenReturn(java.util.List.of());
        when(blogSocialService.blogStats(31L, 3L, 3L)).thenReturn(Map.of());
        when(userService.safeUser(any(User.class), org.mockito.ArgumentMatchers.eq(3L))).thenReturn(Map.of("nickname", "管理员"));

        Map<String, Object> saved = controller.create(admin, new BlogController.CreateBlogRequest(
                "管理员博客", "内容", "蓝桥杯", false, Visibility.PUBLIC, true
        ));

        assertThat(saved).containsEntry("status", "APPROVED");
    }

    @Test
    void adminResourceSubmissionIsApprovedImmediately() {
        ResourceService resourceService = mock(ResourceService.class);
        ReviewService reviewService = mock(ReviewService.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        UserService userService = mock(UserService.class);
        ResourceController controller = new ResourceController(resourceService, reviewService, redisCacheService, userService);
        AuthPrincipal admin = new AuthPrincipal(3L, "admin", Role.ADMIN);
        when(userService.requireUser(3L)).thenReturn(User.builder().id(3L).nickname("管理员").build());
        when(resourceService.createResource(any(Resource.class))).thenAnswer(invocation -> {
            Resource resource = invocation.getArgument(0);
            resource.setId(41L);
            return resource;
        });

        Resource saved = controller.create(admin, new ResourceController.CreateResourceRequest(
                "备赛资料", "蓝桥杯", "学习笔记", "资料说明", "demo.pdf"
        ));

        assertThat(saved.getStatus()).isEqualTo("APPROVED");
    }
}
