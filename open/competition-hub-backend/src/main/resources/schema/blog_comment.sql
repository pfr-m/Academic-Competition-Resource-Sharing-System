CREATE DATABASE IF NOT EXISTS competition_hub
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE competition_hub;

CREATE TABLE IF NOT EXISTS competition_blog_comment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  blog_id BIGINT UNSIGNED NOT NULL COMMENT '所属博客ID',
  parent_id BIGINT UNSIGNED DEFAULT NULL COMMENT '父评论ID，NULL表示一级评论，非NULL表示二级回复',
  author_id BIGINT UNSIGNED NOT NULL COMMENT '评论人用户ID',
  author_nickname VARCHAR(60) NOT NULL COMMENT '评论人昵称快照',
  content VARCHAR(1000) NOT NULL COMMENT '评论内容',
  anonymous TINYINT NOT NULL DEFAULT 0 COMMENT '是否匿名评论：0否，1是',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (id),
  KEY idx_comment_blog_parent_created (blog_id, parent_id, created_at),
  KEY idx_comment_parent (parent_id),
  KEY idx_comment_author (author_id),
  CONSTRAINT fk_blog_comment_blog
    FOREIGN KEY (blog_id) REFERENCES competition_blog (id)
    ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='博客评论与二级回复';
