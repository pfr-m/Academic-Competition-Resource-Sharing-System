package com.competition.hub.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.competition.hub.entity.Salon;
import com.competition.hub.entity.SalonAppointment;

import java.util.List;

public interface SalonService extends IService<Salon> {
    List<Salon> listApproved();

    IPage<Salon> pageApproved(long current, long size);

    Salon createSalon(Salon salon);

    Salon requireApproved(Long id);

    SalonAppointment book(Long salonId, Long userId, String userNickname);

    List<SalonAppointment> listAppointmentsByUser(Long userId);

    List<SalonAppointment> listAllAppointments();

    void cancelAppointment(Long appointmentId);

    void deleteAppointment(Long appointmentId);

    void updateStatus(Long id, String status);
}
