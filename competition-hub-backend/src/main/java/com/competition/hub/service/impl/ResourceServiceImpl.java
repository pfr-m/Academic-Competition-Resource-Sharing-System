package com.competition.hub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.competition.hub.entity.Resource;
import com.competition.hub.mapper.ResourceMapper;
import com.competition.hub.service.ResourceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource> implements ResourceService {
    @Override
    public List<Resource> listApproved(String keyword, String contest, String type) {
        return list(approvedWrapper(keyword, contest, type));
    }

    @Override
    public IPage<Resource> pageApproved(long current, long size, String keyword, String contest, String type) {
        return page(Page.of(current, size), approvedWrapper(keyword, contest, type));
    }

    private LambdaQueryWrapper<Resource> approvedWrapper(String keyword, String contest, String type) {
        LambdaQueryWrapper<Resource> wrapper = new LambdaQueryWrapper<Resource>()
                .eq(Resource::getStatus, "APPROVED");
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query
                    .like(Resource::getTitle, keyword)
                    .or()
                    .like(Resource::getSummary, keyword)
                    .or()
                    .like(Resource::getContest, keyword));
        }
        if (StringUtils.hasText(contest)) {
            wrapper.like(Resource::getContest, contest);
        }
        if (StringUtils.hasText(type)) {
            wrapper.like(Resource::getType, type);
        }
        return wrapper
                .orderByDesc(Resource::getDownloadCount)
                .orderByDesc(Resource::getCreatedAt);
    }

    @Override
    public Resource createResource(Resource resource) {
        LocalDateTime now = LocalDateTime.now();
        resource.setDownloadCount(0);
        resource.setCreatedAt(now);
        resource.setUpdatedAt(now);
        resource.setDeleted(0);
        save(resource);
        return resource;
    }

    @Override
    @Transactional
    public Resource increaseDownloadCount(Long id) {
        Resource resource = requireApproved(id);
        resource.setDownloadCount(resource.getDownloadCount() + 1);
        updateById(resource);
        return resource;
    }

    @Override
    public Resource requireApproved(Long id) {
        Resource resource = requireExisting(id);
        if (!"APPROVED".equals(resource.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "资源不存在或未通过审核");
        }
        return resource;
    }

    @Override
    public Resource requireExisting(Long id) {
        Resource resource = getById(id);
        if (resource == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "资源不存在");
        }
        return resource;
    }

    @Override
    public void updateStatus(Long id, String status) {
        Resource resource = requireExisting(id);
        resource.setStatus(status);
        resource.setUpdatedAt(LocalDateTime.now());
        updateById(resource);
    }
}
