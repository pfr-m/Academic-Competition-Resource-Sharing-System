USE competition_hub;

ALTER TABLE competition_blog
  ADD COLUMN contest_tag VARCHAR(80) NOT NULL DEFAULT '经验分享' COMMENT '竞赛标签' AFTER content;

CREATE INDEX idx_blog_contest_visibility
  ON competition_blog (contest_tag, visibility, status);
