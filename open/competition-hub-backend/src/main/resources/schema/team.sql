CREATE DATABASE IF NOT EXISTS competition_hub
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE competition_hub;

CREATE TABLE IF NOT EXISTS competition_team (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '队伍ID',
  title VARCHAR(120) NOT NULL COMMENT '队伍名称',
  contest VARCHAR(80) NOT NULL COMMENT '竞赛项目',
  need_text VARCHAR(1000) NOT NULL COMMENT '招募需求',
  leader_id BIGINT UNSIGNED NOT NULL COMMENT '发起人/队长ID',
  leader_nickname VARCHAR(60) NOT NULL COMMENT '队长昵称快照',
  skills JSON NOT NULL COMMENT '需要的技能标签JSON数组',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态：PENDING/APPROVED/REJECTED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (id),
  KEY idx_team_status_created (status, created_at),
  KEY idx_team_leader (leader_id),
  KEY idx_team_contest (contest)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='技术红娘组队卡片';

CREATE TABLE IF NOT EXISTS competition_team_member (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '成员记录ID',
  team_id BIGINT UNSIGNED NOT NULL COMMENT '队伍ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  member_role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' COMMENT '成员角色：LEADER/MEMBER',
  contact_unlocked TINYINT NOT NULL DEFAULT 0 COMMENT '是否已向该用户解锁联系方式',
  joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_team_member_user (team_id, user_id),
  KEY idx_team_member_user (user_id),
  CONSTRAINT fk_team_member_team
    FOREIGN KEY (team_id) REFERENCES competition_team (id)
    ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='队伍成员与联系方式解锁状态';

CREATE TABLE IF NOT EXISTS competition_team_application (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  team_id BIGINT UNSIGNED NOT NULL COMMENT '队伍ID',
  applicant_id BIGINT UNSIGNED NOT NULL COMMENT '申请人ID',
  applicant_nickname VARCHAR(60) NOT NULL COMMENT '申请人昵称快照',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING/APPROVED/REJECTED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_team_applicant (team_id, applicant_id),
  KEY idx_team_application_team_status (team_id, status),
  KEY idx_team_application_applicant (applicant_id),
  CONSTRAINT fk_team_application_team
    FOREIGN KEY (team_id) REFERENCES competition_team (id)
    ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='队伍加入申请';
