package com.competition.hub.service.impl;

import com.competition.hub.entity.Resource;
import com.competition.hub.service.RedisCacheService;
import com.competition.hub.service.ResourceService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RedisCacheServiceImpl implements RedisCacheService {
    private static final Duration RESOURCE_CONTEST_TTL = Duration.ofMinutes(10);
    private static final Duration BLOG_DETAIL_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final ResourceService resourceService;

    public RedisCacheServiceImpl(RedisTemplate<String, Object> redisTemplate, StringRedisTemplate stringRedisTemplate, ResourceService resourceService) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.resourceService = resourceService;
    }

    @Override
    public void incrementResourceDownload(Long resourceId) {
        try {
            redisTemplate.opsForZSet().incrementScore(RESOURCE_DOWNLOAD_LEADERBOARD, resourceId.toString(), 1);
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public List<Map<String, Object>> hotResourceRanking() {
        Set<ZSetOperations.TypedTuple<Object>> tuples;
        try {
            tuples = redisTemplate.opsForZSet().reverseRangeWithScores(RESOURCE_DOWNLOAD_LEADERBOARD, 0, 4);
        } catch (RuntimeException ignored) {
            return List.of();
        }
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            if (tuple.getValue() == null) continue;
            Long id = Long.valueOf(tuple.getValue().toString());
            Resource resource = resourceService.getById(id);
            if (resource == null) continue;
            ranking.add(Map.of(
                    "id", resource.getId(),
                    "title", resource.getTitle(),
                    "contest", resource.getContest(),
                    "type", resource.getType(),
                    "downloads", tuple.getScore() == null ? 0 : tuple.getScore().longValue()
            ));
        }
        return ranking;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getContestResourceCache(String contest) {
        Object cached;
        try {
            cached = redisTemplate.opsForValue().get(resourceContestKey(contest));
        } catch (RuntimeException ignored) {
            return null;
        }
        if (cached instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return null;
    }

    @Override
    public void putContestResourceCache(String contest, List<Map<String, Object>> records) {
        try {
            redisTemplate.opsForValue().set(resourceContestKey(contest), records, RESOURCE_CONTEST_TTL);
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void evictContestResourceCache(String contest) {
        if (StringUtils.hasText(contest)) {
            try {
                redisTemplate.delete(resourceContestKey(contest));
            } catch (RuntimeException ignored) {
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getBlogDetail(Long blogId) {
        Object cached;
        try {
            cached = redisTemplate.opsForValue().get(blogDetailKey(blogId));
        } catch (RuntimeException ignored) {
            return null;
        }
        if (cached instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    @Override
    public void putBlogDetail(Long blogId, Map<String, Object> detail) {
        try {
            redisTemplate.opsForValue().set(blogDetailKey(blogId), detail, BLOG_DETAIL_TTL);
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void evictBlogDetail(Long blogId) {
        try {
            redisTemplate.delete(blogDetailKey(blogId));
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void incrementBlogView(Long blogId) {
        try {
            stringRedisTemplate.opsForValue().increment(blogViewKey(blogId));
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public boolean toggleBlogLike(Long blogId, Long userId) {
        String key = blogLikesKey(blogId);
        String value = userId.toString();
        Boolean liked;
        try {
            liked = stringRedisTemplate.opsForSet().isMember(key, value);
        } catch (RuntimeException ignored) {
            return false;
        }
        if (Boolean.TRUE.equals(liked)) {
            stringRedisTemplate.opsForSet().remove(key, value);
            return false;
        }
        stringRedisTemplate.opsForSet().add(key, value);
        return true;
    }

    @Override
    public boolean hasBlogLike(Long blogId, Long userId) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(blogLikesKey(blogId), userId.toString()));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @Override
    public long blogLikeSize(Long blogId) {
        Long size;
        try {
            size = stringRedisTemplate.opsForSet().size(blogLikesKey(blogId));
        } catch (RuntimeException ignored) {
            return 0;
        }
        return size == null ? 0 : size;
    }

    private String resourceContestKey(String contest) {
        return "resource:cache:contest:" + contest;
    }

    private String blogDetailKey(Long blogId) {
        return "blog:detail:" + blogId;
    }

    public static String blogViewKey(Long blogId) {
        return "blog:view:" + blogId;
    }

    public static String blogLikesKey(Long blogId) {
        return "blog:likes:" + blogId;
    }
}
