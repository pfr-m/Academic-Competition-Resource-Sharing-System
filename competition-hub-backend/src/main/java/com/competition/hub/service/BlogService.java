package com.competition.hub.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.competition.hub.entity.Blog;

import java.util.List;

public interface BlogService extends IService<Blog> {
    List<Blog> listVisible(Long viewerId, boolean admin, String keyword);

    IPage<Blog> pageVisible(Long viewerId, boolean admin, long current, long size, String keyword, String contestTag, String visibility);

    Blog createBlog(Blog blog);

    Blog requireReadable(Long id, Long viewerId, boolean admin);

    Blog requireExisting(Long id);

    void updateStatus(Long id, String status);

    void increaseCommentCount(Long id);

    void updateViewAndLikeSnapshot(Long id, Integer viewCount, Integer likeCount);
}
