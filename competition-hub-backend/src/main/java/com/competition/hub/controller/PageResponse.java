package com.competition.hub.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public record PageResponse<T>(List<T> records, long total, long current, long size, long pages) {
    public static <T> PageResponse<T> from(IPage<?> page, List<T> records) {
        return new PageResponse<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }
}
