USE competition_hub;

ALTER TABLE competition_blog
  ADD COLUMN like_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数量快照' AFTER view_count;
