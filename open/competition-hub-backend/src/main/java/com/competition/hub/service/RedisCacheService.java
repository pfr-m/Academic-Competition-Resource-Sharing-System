package com.competition.hub.service;

import com.competition.hub.entity.Resource;

import java.util.List;
import java.util.Map;

public interface RedisCacheService {
    String RESOURCE_DOWNLOAD_LEADERBOARD = "resource:download:leaderboard";

    void incrementResourceDownload(Long resourceId);

    List<Map<String, Object>> hotResourceRanking();

    List<Map<String, Object>> getContestResourceCache(String contest);

    void putContestResourceCache(String contest, List<Map<String, Object>> records);

    void evictContestResourceCache(String contest);

    Map<String, Object> getBlogDetail(Long blogId);

    void putBlogDetail(Long blogId, Map<String, Object> detail);

    void evictBlogDetail(Long blogId);

    void incrementBlogView(Long blogId);

    boolean toggleBlogLike(Long blogId, Long userId);

    boolean hasBlogLike(Long blogId, Long userId);

    long blogLikeSize(Long blogId);
}
