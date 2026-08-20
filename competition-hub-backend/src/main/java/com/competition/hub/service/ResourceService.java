package com.competition.hub.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.competition.hub.entity.Resource;

import java.util.List;

public interface ResourceService extends IService<Resource> {
    List<Resource> listApproved(String keyword, String contest, String type);

    IPage<Resource> pageApproved(long current, long size, String keyword, String contest, String type);

    long countApproved();

    Resource createResource(Resource resource);

    Resource increaseDownloadCount(Long id);

    Resource requireApproved(Long id);

    Resource requireExisting(Long id);

    void updateStatus(Long id, String status);
}
