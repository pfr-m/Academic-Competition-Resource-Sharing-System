package com.competition.hub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.competition.hub.entity.Salon;
import com.competition.hub.entity.SalonAppointment;
import com.competition.hub.mapper.SalonAppointmentMapper;
import com.competition.hub.mapper.SalonMapper;
import com.competition.hub.service.SalonService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SalonServiceImpl extends ServiceImpl<SalonMapper, Salon> implements SalonService {
    private final SalonAppointmentMapper appointmentMapper;

    public SalonServiceImpl(SalonAppointmentMapper appointmentMapper) {
        this.appointmentMapper = appointmentMapper;
    }

    @Override
    public List<Salon> listApproved() {
        return list(approvedWrapper());
    }

    @Override
    public IPage<Salon> pageApproved(long current, long size) {
        return page(Page.of(current, size), approvedWrapper());
    }

    private LambdaQueryWrapper<Salon> approvedWrapper() {
        return new LambdaQueryWrapper<Salon>()
                .eq(Salon::getStatus, "APPROVED")
                .orderByAsc(Salon::getStartsAt);
    }

    @Override
    public Salon createSalon(Salon salon) {
        LocalDateTime now = LocalDateTime.now();
        salon.setBookedCount(0);
        salon.setCreatedAt(now);
        salon.setUpdatedAt(now);
        salon.setDeleted(0);
        save(salon);
        return salon;
    }

    @Override
    public Salon requireApproved(Long id) {
        Salon salon = getById(id);
        if (salon == null || !"APPROVED".equals(salon.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "沙龙不存在或未通过审核");
        }
        return salon;
    }

    @Override
    @Transactional
    public SalonAppointment book(Long salonId, Long userId, String userNickname) {
        requireApproved(salonId);
        SalonAppointment existing = appointmentMapper.selectOne(new LambdaQueryWrapper<SalonAppointment>()
                .eq(SalonAppointment::getSalonId, salonId)
                .eq(SalonAppointment::getUserId, userId));
        if (existing != null && "BOOKED".equals(existing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "你已经预约过该沙龙");
        }
        boolean increased = update(new LambdaUpdateWrapper<Salon>()
                .eq(Salon::getId, salonId)
                .apply("booked_count < capacity")
                .setSql("booked_count = booked_count + 1"));
        if (!increased) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该沙龙名额已满");
        }
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setUserNickname(userNickname);
            existing.setStatus("BOOKED");
            existing.setCertificateCode("SALON-" + salonId + "-" + userId + "-" + System.currentTimeMillis());
            existing.setUpdatedAt(now);
            existing.setDeleted(0);
            appointmentMapper.updateById(existing);
            return existing;
        }
        SalonAppointment appointment = SalonAppointment.builder()
                .salonId(salonId)
                .userId(userId)
                .userNickname(userNickname)
                .status("BOOKED")
                .certificateCode("SALON-" + salonId + "-" + userId + "-" + System.currentTimeMillis())
                .createdAt(now)
                .updatedAt(now)
                .deleted(0)
                .build();
        appointmentMapper.insert(appointment);
        return appointment;
    }

    @Override
    public List<SalonAppointment> listAppointmentsByUser(Long userId) {
        return appointmentMapper.selectList(new LambdaQueryWrapper<SalonAppointment>()
                .eq(SalonAppointment::getUserId, userId)
                .eq(SalonAppointment::getStatus, "BOOKED")
                .orderByDesc(SalonAppointment::getCreatedAt));
    }

    @Override
    public List<SalonAppointment> listAllAppointments() {
        return appointmentMapper.selectList(new LambdaQueryWrapper<SalonAppointment>()
                .orderByDesc(SalonAppointment::getCreatedAt));
    }

    @Override
    @Transactional
    public void cancelAppointment(Long appointmentId) {
        SalonAppointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null || !"BOOKED".equals(appointment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "预约不存在或已取消");
        }
        appointment.setStatus("CANCELLED");
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentMapper.updateById(appointment);
        update(new LambdaUpdateWrapper<Salon>()
                .eq(Salon::getId, appointment.getSalonId())
                .gt(Salon::getBookedCount, 0)
                .setSql("booked_count = booked_count - 1"));
    }

    @Override
    public void deleteAppointment(Long appointmentId) {
        SalonAppointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "预约记录不存在");
        }
        if ("BOOKED".equals(appointment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "请先取消预约，释放名额后再删除记录");
        }
        appointmentMapper.physicalDeleteById(appointmentId);
    }

    @Override
    public void updateStatus(Long id, String status) {
        Salon salon = getById(id);
        if (salon == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "沙龙不存在");
        }
        salon.setStatus(status);
        salon.setUpdatedAt(LocalDateTime.now());
        updateById(salon);
    }
}
