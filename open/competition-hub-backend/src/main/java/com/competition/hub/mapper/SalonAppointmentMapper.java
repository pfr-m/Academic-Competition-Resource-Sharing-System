package com.competition.hub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.competition.hub.entity.SalonAppointment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface SalonAppointmentMapper extends BaseMapper<SalonAppointment> {
    @Delete("DELETE FROM competition_salon_appointment WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
