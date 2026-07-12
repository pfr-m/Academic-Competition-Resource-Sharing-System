package com.competition.hub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.competition.hub.entity.BlogComment;

import java.util.List;

public interface BlogCommentService extends IService<BlogComment> {
    List<BlogComment> listByBlogId(Long blogId);

    BlogComment createComment(BlogComment comment);

    BlogComment requireInBlog(Long id, Long blogId);

    void deleteCommentTree(Long id, Long blogId);

    void deleteByBlogId(Long blogId);
}
