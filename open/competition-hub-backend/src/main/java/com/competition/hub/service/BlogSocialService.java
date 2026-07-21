package com.competition.hub.service;

import java.util.Map;

public interface BlogSocialService {
    String LIKE = "LIKE";
    String COLLECT = "COLLECT";

    Map<String, Object> toggleLikeCollect(Long blogId, Long userId, String type);

    Map<String, Object> toggleFollow(Long authorId, Long followerId);

    Map<String, Object> blogStats(Long blogId, Long viewerId, Long authorId);

    Map<String, Object> creatorStats(Long authorId);
}
