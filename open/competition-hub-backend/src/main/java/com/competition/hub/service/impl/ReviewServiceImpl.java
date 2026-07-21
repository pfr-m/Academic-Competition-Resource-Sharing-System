package com.competition.hub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.competition.hub.entity.Review;
import com.competition.hub.mapper.ReviewMapper;
import com.competition.hub.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {
    @Override
    public Review createReview(String targetType, Long targetId, String title, Long ownerId) {
        LocalDateTime now = LocalDateTime.now();
        Review review = Review.builder()
                .targetType(targetType)
                .targetId(targetId)
                .title(title)
                .ownerId(ownerId)
                .status("PENDING")
                .createdAt(now)
                .updatedAt(now)
                .deleted(0)
                .build();
        save(review);
        return review;
    }

    @Override
    public Review requireExisting(Long id) {
        Review review = getById(id);
        if (review == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "审核记录不存在");
        }
        return review;
    }

    @Override
    public List<Review> listLatest() {
        return list(latestWrapper());
    }

    @Override
    public IPage<Review> pageLatest(long current, long size) {
        return page(Page.of(current, size), latestWrapper());
    }

    private LambdaQueryWrapper<Review> latestWrapper() {
        return new LambdaQueryWrapper<Review>()
                .orderByDesc(Review::getCreatedAt);
    }

    @Override
    public Review updateStatus(Long id, String status) {
        Review review = requireExisting(id);
        review.setStatus(status);
        review.setUpdatedAt(LocalDateTime.now());
        updateById(review);
        return review;
    }
}
