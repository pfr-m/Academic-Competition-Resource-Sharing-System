USE competition_hub;

CREATE TABLE IF NOT EXISTS blog_like_collect (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  blog_id BIGINT UNSIGNED NOT NULL COMMENT '博客ID',
  type VARCHAR(20) NOT NULL COMMENT 'LIKE/COLLECT',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_blog_user_type (blog_id, user_id, type),
  KEY idx_blog_social_blog_type (blog_id, type),
  KEY idx_blog_social_user_type (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='博客点赞收藏';

CREATE TABLE IF NOT EXISTS user_follow (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  follower_id BIGINT UNSIGNED NOT NULL COMMENT '粉丝用户ID',
  author_id BIGINT UNSIGNED NOT NULL COMMENT '被关注作者ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_follower_author (follower_id, author_id),
  KEY idx_follow_author (author_id),
  KEY idx_follow_follower (follower_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注关系';
