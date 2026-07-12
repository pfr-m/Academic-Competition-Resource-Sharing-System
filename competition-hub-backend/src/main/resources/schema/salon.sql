CREATE DATABASE IF NOT EXISTS competition_hub
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE competition_hub;

CREATE TABLE IF NOT EXISTS competition_salon (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '沙龙ID',
  title VARCHAR(160) NOT NULL COMMENT '活动主题',
  description VARCHAR(1000) NOT NULL COMMENT '活动介绍',
  speaker_id BIGINT UNSIGNED NOT NULL COMMENT '主讲人用户ID',
  speaker_name VARCHAR(60) NOT NULL COMMENT '主讲人昵称快照',
  starts_at DATETIME NOT NULL COMMENT '沙龙开始时间',
  place VARCHAR(160) NOT NULL COMMENT '地点',
  capacity INT UNSIGNED NOT NULL COMMENT '最大人数上限',
  booked_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已预约人数',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态：PENDING/APPROVED/REJECTED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (id),
  KEY idx_salon_status_time (status, starts_at),
  KEY idx_salon_speaker (speaker_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='线下沙龙活动';

CREATE TABLE IF NOT EXISTS competition_salon_appointment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  salon_id BIGINT UNSIGNED NOT NULL COMMENT '沙龙ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '预约用户ID',
  user_nickname VARCHAR(60) NOT NULL COMMENT '预约用户昵称快照',
  status VARCHAR(20) NOT NULL DEFAULT 'BOOKED' COMMENT '预约状态：BOOKED/CANCELLED',
  certificate_code VARCHAR(80) NOT NULL COMMENT '预约凭证码',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '预约时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_salon_user (salon_id, user_id),
  UNIQUE KEY uk_certificate_code (certificate_code),
  KEY idx_appointment_user (user_id),
  CONSTRAINT fk_appointment_salon
    FOREIGN KEY (salon_id) REFERENCES competition_salon (id)
    ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='沙龙预约记录';
