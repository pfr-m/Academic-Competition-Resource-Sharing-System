USE competition_hub;

ALTER TABLE competition_user
  ADD COLUMN first_login_prompted TINYINT NOT NULL DEFAULT 0 COMMENT '是否已展示首次登录提示'
  AFTER wechat_visibility;
