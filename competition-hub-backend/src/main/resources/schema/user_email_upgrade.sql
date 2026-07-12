USE competition_hub;

ALTER TABLE competition_user
  ADD COLUMN email VARCHAR(120) DEFAULT NULL COMMENT '邮箱' AFTER phone;

CREATE UNIQUE INDEX uk_user_email ON competition_user (email);
