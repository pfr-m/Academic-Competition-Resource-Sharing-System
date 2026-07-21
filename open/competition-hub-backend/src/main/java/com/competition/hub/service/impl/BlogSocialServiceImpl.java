package com.competition.hub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.competition.hub.entity.Blog;
import com.competition.hub.entity.BlogLikeCollect;
import com.competition.hub.entity.UserFollow;
import com.competition.hub.mapper.BlogLikeCollectMapper;
import com.competition.hub.mapper.UserFollowMapper;
import com.competition.hub.service.BlogService;
import com.competition.hub.service.BlogSocialService;
import com.competition.hub.service.RedisCacheService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class BlogSocialServiceImpl implements BlogSocialService {
    private final BlogLikeCollectMapper blogLikeCollectMapper;
    private final UserFollowMapper userFollowMapper;
    private final BlogService blogService;
    private final RedisCacheService redisCacheService;

    public BlogSocialServiceImpl(BlogLikeCollectMapper blogLikeCollectMapper, UserFollowMapper userFollowMapper, BlogService blogService, RedisCacheService redisCacheService) {
        this.blogLikeCollectMapper = blogLikeCollectMapper;
        this.userFollowMapper = userFollowMapper;
        this.blogService = blogService;
        this.redisCacheService = redisCacheService;
    }

    @Override
    public Map<String, Object> toggleLikeCollect(Long blogId, Long userId, String type) {
        String normalized = normalizeType(type);
        if (LIKE.equals(normalized)) {
            boolean active = redisCacheService.toggleBlogLike(blogId, userId);
            return Map.of(
                    "active", active,
                    "type", normalized,
                    "stats", blogStats(blogId, userId, blogService.requireExisting(blogId).getAuthorId())
            );
        }
        LambdaQueryWrapper<BlogLikeCollect> wrapper = new LambdaQueryWrapper<BlogLikeCollect>()
                .eq(BlogLikeCollect::getBlogId, blogId)
                .eq(BlogLikeCollect::getUserId, userId)
                .eq(BlogLikeCollect::getType, normalized);
        BlogLikeCollect existing = blogLikeCollectMapper.selectOne(wrapper);
        boolean active;
        if (existing == null) {
            blogLikeCollectMapper.insert(BlogLikeCollect.builder()
                    .blogId(blogId)
                    .userId(userId)
                    .type(normalized)
                    .createdAt(LocalDateTime.now())
                    .build());
            active = true;
        } else {
            blogLikeCollectMapper.deleteById(existing.getId());
            active = false;
        }
        return Map.of(
                "active", active,
                "type", normalized,
                "stats", blogStats(blogId, userId, blogService.requireExisting(blogId).getAuthorId())
        );
    }

    @Override
    public Map<String, Object> toggleFollow(Long authorId, Long followerId) {
        if (authorId.equals(followerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能关注自己");
        }
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getAuthorId, authorId)
                .eq(UserFollow::getFollowerId, followerId);
        UserFollow existing = userFollowMapper.selectOne(wrapper);
        boolean active;
        if (existing == null) {
            userFollowMapper.insert(UserFollow.builder()
                    .authorId(authorId)
                    .followerId(followerId)
                    .createdAt(LocalDateTime.now())
                    .build());
            active = true;
        } else {
            userFollowMapper.deleteById(existing.getId());
            active = false;
        }
        return Map.of("active", active, "stats", creatorStats(authorId));
    }

    @Override
    public Map<String, Object> blogStats(Long blogId, Long viewerId, Long authorId) {
        long likes = redisCacheService.blogLikeSize(blogId);
        long collects = countBlogAction(blogId, COLLECT);
        boolean liked = redisCacheService.hasBlogLike(blogId, viewerId);
        boolean collected = hasBlogAction(blogId, viewerId, COLLECT);
        boolean followed = hasFollow(authorId, viewerId);
        return Map.of(
                "likes", likes,
                "collects", collects,
                "liked", liked,
                "collected", collected,
                "followed", followed
        );
    }

    @Override
    public Map<String, Object> creatorStats(Long authorId) {
        List<Long> blogIds = blogService.list(new LambdaQueryWrapper<Blog>().eq(Blog::getAuthorId, authorId))
                .stream()
                .map(Blog::getId)
                .toList();
        long likes = blogIds.stream().mapToLong(redisCacheService::blogLikeSize).sum();
        long collects = blogIds.isEmpty() ? 0 : blogLikeCollectMapper.selectCount(new LambdaQueryWrapper<BlogLikeCollect>()
                .in(BlogLikeCollect::getBlogId, blogIds)
                .eq(BlogLikeCollect::getType, COLLECT));
        long followers = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getAuthorId, authorId));
        return Map.of(
                "totalLikes", likes,
                "totalCollects", collects,
                "followers", followers,
                "works", blogIds.size()
        );
    }

    private long countBlogAction(Long blogId, String type) {
        return blogLikeCollectMapper.selectCount(new LambdaQueryWrapper<BlogLikeCollect>()
                .eq(BlogLikeCollect::getBlogId, blogId)
                .eq(BlogLikeCollect::getType, type));
    }

    private boolean hasBlogAction(Long blogId, Long userId, String type) {
        return blogLikeCollectMapper.selectCount(new LambdaQueryWrapper<BlogLikeCollect>()
                .eq(BlogLikeCollect::getBlogId, blogId)
                .eq(BlogLikeCollect::getUserId, userId)
                .eq(BlogLikeCollect::getType, type)) > 0;
    }

    private boolean hasFollow(Long authorId, Long followerId) {
        return userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getAuthorId, authorId)
                .eq(UserFollow::getFollowerId, followerId)) > 0;
    }

    private String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase();
        if (!LIKE.equals(normalized) && !COLLECT.equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "互动类型只能是 LIKE 或 COLLECT");
        }
        return normalized;
    }
}
