package com.competition.hub.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.competition.hub.entity.Blog;
import com.competition.hub.mapper.BlogMapper;
import com.competition.hub.service.BlogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements BlogService {
    @Override
    public List<Blog> listVisible(Long viewerId, String keyword) {
        LambdaQueryWrapper<Blog> wrapper = visibleWrapper(viewerId, keyword, null, null);
        return list(wrapper);
    }

    @Override
    public IPage<Blog> pageVisible(Long viewerId, long current, long size, String keyword, String contestTag, String visibility) {
        long safeCurrent = Math.max(current, 1);
        long safeSize = Math.min(Math.max(size, 1), 50);
        return page(Page.of(safeCurrent, safeSize), visibleWrapper(viewerId, keyword, contestTag, visibility));
    }

    @Override
    public Blog createBlog(Blog blog) {
        LocalDateTime now = LocalDateTime.now();
        blog.setViewCount(0);
        blog.setLikeCount(0);
        blog.setCommentCount(0);
        if (!StringUtils.hasText(blog.getContestTag())) {
            blog.setContestTag("经验分享");
        }
        blog.setCreatedAt(now);
        blog.setUpdatedAt(now);
        blog.setDeleted(0);
        save(blog);
        return blog;
    }

    @Override
    public Blog requireReadable(Long id, Long viewerId) {
        Blog blog = requireExisting(id);
        if (!"APPROVED".equals(blog.getStatus()) && !blog.getAuthorId().equals(viewerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "博客不存在或未通过审核");
        }
        boolean readable = "PUBLIC".equals(blog.getVisibility()) || blog.getAuthorId().equals(viewerId);
        if (!readable) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权查看该博客");
        }
        return blog;
    }

    @Override
    public Blog requireExisting(Long id) {
        Blog blog = getById(id);
        if (blog == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "博客不存在");
        }
        return blog;
    }

    @Override
    public void updateStatus(Long id, String status) {
        Blog blog = requireExisting(id);
        blog.setStatus(status);
        updateById(blog);
    }

    @Override
    @Transactional
    public void increaseCommentCount(Long id) {
        Blog blog = getById(id);
        if (blog == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "博客不存在");
        }
        blog.setCommentCount(blog.getCommentCount() + 1);
        updateById(blog);
    }

    @Override
    public void updateViewAndLikeSnapshot(Long id, Integer viewCount, Integer likeCount) {
        Blog blog = requireExisting(id);
        blog.setViewCount(viewCount);
        blog.setLikeCount(likeCount);
        blog.setUpdatedAt(LocalDateTime.now());
        updateById(blog);
    }

    private LambdaQueryWrapper<Blog> visibleWrapper(Long viewerId, String keyword, String contestTag, String visibility) {
        LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<Blog>();
        String normalizedVisibility = StringUtils.hasText(visibility) ? visibility.trim().toUpperCase() : "";
        if ("PRIVATE".equals(normalizedVisibility)) {
            wrapper.eq(Blog::getAuthorId, viewerId).eq(Blog::getVisibility, "PRIVATE");
        } else if ("PUBLIC".equals(normalizedVisibility)) {
            wrapper.eq(Blog::getStatus, "APPROVED").eq(Blog::getVisibility, "PUBLIC");
        } else {
            wrapper.and(query -> query
                    .and(publicQuery -> publicQuery.eq(Blog::getStatus, "APPROVED").eq(Blog::getVisibility, "PUBLIC"))
                    .or(ownQuery -> ownQuery.eq(Blog::getAuthorId, viewerId)));
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query
                    .like(Blog::getTitle, keyword)
                    .or()
                    .like(Blog::getContent, keyword)
                    .or()
                    .like(Blog::getAuthorNickname, keyword));
        }
        if (StringUtils.hasText(contestTag)) {
            wrapper.like(Blog::getContestTag, contestTag);
        }
        return wrapper.orderByDesc(Blog::getCreatedAt);
    }
}
