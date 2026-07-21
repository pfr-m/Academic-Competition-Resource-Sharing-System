package com.competition.hub.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.competition.hub.entity.Review;

import java.util.List;

public interface ReviewService extends IService<Review> {
    Review createReview(String targetType, Long targetId, String title, Long ownerId);

    Review requireExisting(Long id);

    List<Review> listLatest();

    IPage<Review> pageLatest(long current, long size);

    Review updateStatus(Long id, String status);
}
