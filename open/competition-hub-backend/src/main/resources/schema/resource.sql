CREATE DATABASE IF NOT EXISTS competition_hub
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE competition_hub;

CREATE TABLE IF NOT EXISTS competition_resource (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '资源ID',
  title VARCHAR(120) NOT NULL COMMENT '资料名称',
  contest VARCHAR(60) NOT NULL COMMENT '竞赛名称，如蓝桥杯、天梯赛、数学建模',
  type VARCHAR(60) NOT NULL COMMENT '资料类型，如历年真题、获奖论文、算法路线',
  summary VARCHAR(500) NOT NULL COMMENT '资料简介',
  uploader_id BIGINT UNSIGNED NOT NULL COMMENT '上传者用户ID',
  uploader_name VARCHAR(60) NOT NULL COMMENT '上传者展示名称快照',
  file_key VARCHAR(255) NOT NULL COMMENT '后端本地存储文件名或云存储对象Key',
  original_file_name VARCHAR(255) NOT NULL COMMENT '用户上传时的原始文件名',
  file_size BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文件大小，单位字节',
  content_type VARCHAR(120) DEFAULT NULL COMMENT '文件MIME类型',
  download_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '下载次数',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态：PENDING/APPROVED/REJECTED',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (id),
  KEY idx_resource_status_download (status, download_count),
  KEY idx_resource_contest_type (contest, type),
  KEY idx_resource_uploader (uploader_id),
  KEY idx_resource_created_at (created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='竞赛资源库';
