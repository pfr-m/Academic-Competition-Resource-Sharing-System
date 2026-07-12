package com.competition.hub.controller;

import com.competition.hub.entity.Salon;
import com.competition.hub.entity.SalonAppointment;
import com.competition.hub.entity.User;
import com.competition.hub.service.SalonService;
import com.competition.hub.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/salon/appointments")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class SalonAppointmentExportController {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final SalonService salonService;
    private final UserService userService;

    public SalonAppointmentExportController(SalonService salonService, UserService userService) {
        this.salonService = salonService;
        this.userService = userService;
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        String fileName = URLEncoder.encode("沙龙预约数据.xlsx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("沙龙预约数据");
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = {"预约ID", "沙龙ID", "沙龙主题", "预约人ID", "工号/学号", "预约人昵称", "预约状态", "凭证码", "预约时间"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
                headerRow.getCell(i).setCellStyle(headerStyle);
            }

            List<SalonAppointment> appointments = salonService.listAllAppointments();
            for (int i = 0; i < appointments.size(); i++) {
                SalonAppointment appointment = appointments.get(i);
                Salon salon = salonService.getById(appointment.getSalonId());
                User user = userService.getById(appointment.getUserId());
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(appointment.getId());
                row.createCell(1).setCellValue(appointment.getSalonId());
                row.createCell(2).setCellValue(salon == null ? "沙龙已删除" : salon.getTitle());
                row.createCell(3).setCellValue(appointment.getUserId());
                row.createCell(4).setCellValue(user == null ? "" : user.getStudentNo());
                row.createCell(5).setCellValue(appointment.getUserNickname());
                row.createCell(6).setCellValue(statusText(appointment.getStatus()));
                row.createCell(7).setCellValue(appointment.getCertificateCode());
                row.createCell(8).setCellValue(appointment.getCreatedAt() == null ? "" : appointment.getCreatedAt().format(DATE_TIME_FORMATTER));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(response.getOutputStream());
        }
    }

    private String statusText(String status) {
        if ("BOOKED".equals(status)) {
            return "已预约";
        }
        if ("CANCELLED".equals(status)) {
            return "已取消";
        }
        return status == null ? "" : status;
    }
}