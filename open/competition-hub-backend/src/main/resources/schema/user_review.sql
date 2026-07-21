CREATE DATABASE IF NOT EXISTS competition_hub
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE competition_hub;

CREATE TABLE IF NOT EXISTS competition_user (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  student_no VARCHAR(40) NOT NULL COMMENT '学号/账号编号',
  username VARCHAR(60) NOT NULL COMMENT '登录账号',
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt加密后的密码',
  real_name VARCHAR(60) NOT NULL COMMENT '真实姓名',
  nickname VARCHAR(60) NOT NULL COMMENT '昵称',
  phone VARCHAR(30) DEFAULT NULL COMMENT '手机号',
  email VARCHAR(120) DEFAULT NULL COMMENT '邮箱',
  wechat VARCHAR(80) DEFAULT NULL COMMENT '微信号',
  skills JSON NOT NULL COMMENT '技术栈标签',
  badges JSON NOT NULL COMMENT '荣誉勋章',
  role VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT '角色：STUDENT/MENTOR/ADMIN/SUPER_ADMIN',
  real_name_visibility VARCHAR(20) NOT NULL DEFAULT 'TEAM_ONLY' COMMENT '真实姓名可见度：HIDDEN/TEAM_ONLY/PUBLIC',
  phone_visibility VARCHAR(20) NOT NULL DEFAULT 'HIDDEN' COMMENT '手机号可见度：HIDDEN/TEAM_ONLY/PUBLIC',
  wechat_visibility VARCHAR(20) NOT NULL DEFAULT 'TEAM_ONLY' COMMENT '微信可见度：HIDDEN/TEAM_ONLY/PUBLIC',
  first_login_prompted TINYINT NOT NULL DEFAULT 0 COMMENT '是否已展示首次登录提示',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  super_admin_guard TINYINT
    GENERATED ALWAYS AS (
      CASE
        WHEN role = 'SUPER_ADMIN' AND deleted = 0 THEN 1
        ELSE NULL
      END
    ) STORED COMMENT '唯一超级管理员约束辅助列',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_username (username),
  UNIQUE KEY uk_user_student_no (student_no),
  UNIQUE KEY uk_user_email (email),
  UNIQUE KEY uk_single_super_admin (super_admin_guard),
  KEY idx_user_role (role)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='系统用户';

CREATE TABLE IF NOT EXISTS competition_review (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审核ID',
  target_type VARCHAR(20) NOT NULL COMMENT '目标类型：resource/blog/team/salon',
  target_id BIGINT UNSIGNED NOT NULL COMMENT '目标ID',
  title VARCHAR(180) NOT NULL COMMENT '审核内容标题',
  owner_id BIGINT UNSIGNED NOT NULL COMMENT '发起人ID',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态：PENDING/APPROVED/REJECTED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (id),
  KEY idx_review_status_time (status, created_at),
  KEY idx_review_target (target_type, target_id),
  KEY idx_review_owner (owner_id),
  CONSTRAINT fk_review_owner
    FOREIGN KEY (owner_id) REFERENCES competition_user (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='内容审核记录';

INSERT INTO competition_user
  (id, student_no, username, password_hash, real_name, nickname, phone, wechat, skills, badges, role, real_name_visibility, phone_visibility, wechat_visibility)
VALUES
  (1, '20230118', 'student', '$2a$10$DTcFddZkkWQvaJ/UY1pHYeffr2sS.UpE3RCwfakGfYRqgtx60HNRi', '陈嘉禾', '陈同学', '13800002026', 'chen_compete', JSON_ARRAY('Java', '算法', 'Python'), JSON_ARRAY(), 'STUDENT', 'TEAM_ONLY', 'HIDDEN', 'TEAM_ONLY'),
  (2, 'M2026', 'mentor', '$2a$10$2TRkDpgoS37F.xFwtfiQCOmZSjOZaDqCfz.Tb2sogWj5ureivJ65.', '林清予', '林学姐', '13900002026', 'lin_modeling', JSON_ARRAY('数学建模', '论文写作', 'Python'), JSON_ARRAY('国奖得主', '特邀专家'), 'MENTOR', 'PUBLIC', 'HIDDEN', 'TEAM_ONLY'),
  (3, 'ADMIN', 'admin', '$2a$10$2yJDuDBZmzMY2v8YMRPeM.Bjqv0SdrTTukyoaYT6f0h6Fd80rXfg6', '竞赛管理员', '管理员', '13700002026', 'competition_admin', JSON_ARRAY('审核', '人工撮合'), JSON_ARRAY('平台管理员'), 'ADMIN', 'PUBLIC', 'TEAM_ONLY', 'PUBLIC'),
  (4, 'PFR', 'PFR', '$2a$10$kCkwbLTyxdfxsSN0AbGKxec5IzpCzAnqAervMTSFj0r0PN0cHk0Lq', 'PFR', 'PFR', '13600002026', 'pfr_super_admin', JSON_ARRAY('系统管理', '权限管理'), JSON_ARRAY('平台超级管理员'), 'SUPER_ADMIN', 'PUBLIC', 'TEAM_ONLY', 'PUBLIC')
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  password_hash = VALUES(password_hash),
  role = VALUES(role),
  updated_at = CURRENT_TIMESTAMP;
