package com.competition.hub.task;

import com.competition.hub.entity.Blog;
import com.competition.hub.service.BlogService;
import com.competition.hub.service.impl.RedisCacheServiceImpl;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class BlogRedisFlushTask {
    private final StringRedisTemplate stringRedisTemplate;
    private final BlogService blogService;

    public BlogRedisFlushTask(StringRedisTemplate stringRedisTemplate, BlogService blogService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.blogService = blogService;
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void flushBlogCounters() {
        try {
            Set<Long> blogIds = new HashSet<>();
            scanIds("blog:view:*", "blog:view:", blogIds);
            scanIds("blog:likes:*", "blog:likes:", blogIds);
            for (Long blogId : blogIds) {
                Blog blog = blogService.getById(blogId);
                if (blog == null) {
                    continue;
                }
                String views = stringRedisTemplate.opsForValue().get(RedisCacheServiceImpl.blogViewKey(blogId));
                Long likes = stringRedisTemplate.opsForSet().size(RedisCacheServiceImpl.blogLikesKey(blogId));
                int viewCount = views == null ? safeInt(blog.getViewCount()) : Integer.parseInt(views);
                int likeCount = likes == null ? safeInt(blog.getLikeCount()) : likes.intValue();
                blogService.updateViewAndLikeSnapshot(blogId, viewCount, likeCount);
            }
        } catch (RuntimeException ignored) {
            // Redis may be unavailable in local demos; skip this cycle and retry later.
        }
    }

    private void scanIds(String pattern, String prefix, Set<Long> target) {
        stringRedisTemplate.scan(ScanOptions.scanOptions().match(pattern).count(200).build()).forEachRemaining(key -> {
            try {
                target.add(Long.valueOf(key.substring(prefix.length())));
            } catch (NumberFormatException ignored) {
                // Ignore malformed keys from manual Redis operations.
            }
        });
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
