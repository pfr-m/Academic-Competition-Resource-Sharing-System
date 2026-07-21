package com.competition.hub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.competition.hub.entity.BlogComment;
import com.competition.hub.mapper.BlogCommentMapper;
import com.competition.hub.service.BlogCommentService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BlogCommentServiceImpl extends ServiceImpl<BlogCommentMapper, BlogComment> implements BlogCommentService {
    @Override
    public List<BlogComment> listByBlogId(Long blogId) {
        return list(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getBlogId, blogId)
                .orderByAsc(BlogComment::getCreatedAt));
    }

    @Override
    public BlogComment createComment(BlogComment comment) {
        LocalDateTime now = LocalDateTime.now();
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        comment.setDeleted(0);
        save(comment);
        return comment;
    }

    @Override
    public BlogComment requireInBlog(Long id, Long blogId) {
        BlogComment comment = getById(id);
        if (comment == null || !comment.getBlogId().equals(blogId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在");
        }
        return comment;
    }

    @Override
    @Transactional
    public void deleteCommentTree(Long id, Long blogId) {
        requireInBlog(id, blogId);
        removeById(id);
        remove(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getBlogId, blogId)
                .eq(BlogComment::getParentId, id));
    }

    @Override
    public void deleteByBlogId(Long blogId) {
        remove(new LambdaUpdateWrapper<BlogComment>()
                .eq(BlogComment::getBlogId, blogId));
    }
}
