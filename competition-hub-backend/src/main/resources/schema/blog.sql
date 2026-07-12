CREATE DATABASE IF NOT EXISTS competition_hub
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE competition_hub;

CREATE TABLE IF NOT EXISTS competition_blog (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '博客ID',
  title VARCHAR(160) NOT NULL COMMENT '博客标题',
  content MEDIUMTEXT NOT NULL COMMENT 'Markdown正文内容',
  contest_tag VARCHAR(80) NOT NULL DEFAULT '经验分享' COMMENT '竞赛标签',
  author_id BIGINT UNSIGNED NOT NULL COMMENT '作者用户ID',
  author_nickname VARCHAR(60) NOT NULL COMMENT '作者昵称快照',
  anonymous TINYINT NOT NULL DEFAULT 0 COMMENT '是否匿名发布：0否，1是',
  visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC' COMMENT '可见性：PUBLIC/PRIVATE',
  comments_open TINYINT NOT NULL DEFAULT 1 COMMENT '是否开启评论：0关闭，1开启',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态：PENDING/APPROVED/REJECTED',
  view_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览次数',
  like_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数量快照',
  comment_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '评论数量',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (id),
  KEY idx_blog_status_created (status, created_at),
  KEY idx_blog_contest_visibility (contest_tag, visibility, status),
  KEY idx_blog_author (author_id),
  KEY idx_blog_visibility (visibility),
  FULLTEXT KEY ft_blog_title_content (title, content)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='经验博客';
