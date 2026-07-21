USE competition_hub;

UPDATE competition_user
SET role = 'ADMIN',
    first_login_prompted = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE role = 'SUPER_ADMIN'
  AND username <> 'PFR'
  AND deleted = 0;

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_user'
    AND column_name = 'super_admin_guard'
);

SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE competition_user ADD COLUMN super_admin_guard TINYINT GENERATED ALWAYS AS (CASE WHEN role = ''SUPER_ADMIN'' AND deleted = 0 THEN 1 ELSE NULL END) STORED',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @key_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'competition_user'
    AND index_name = 'uk_single_super_admin'
);

SET @sql := IF(
  @key_exists = 0,
  'ALTER TABLE competition_user ADD UNIQUE KEY uk_single_super_admin (super_admin_guard)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO competition_user
  (student_no, username, password_hash, real_name, nickname, phone, wechat, skills, badges, role, real_name_visibility, phone_visibility, wechat_visibility, first_login_prompted, deleted)
VALUES
  ('PFR', 'PFR', '$2a$10$kCkwbLTyxdfxsSN0AbGKxec5IzpCzAnqAervMTSFj0r0PN0cHk0Lq', 'PFR', 'PFR', '13600002026', 'pfr_super_admin', JSON_ARRAY('系统管理', '权限管理'), JSON_ARRAY('平台超级管理员'), 'SUPER_ADMIN', 'PUBLIC', 'TEAM_ONLY', 'PUBLIC', 0, 0)
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  real_name = VALUES(real_name),
  nickname = VALUES(nickname),
  role = 'SUPER_ADMIN',
  first_login_prompted = 0,
  deleted = 0,
  updated_at = CURRENT_TIMESTAMP;
