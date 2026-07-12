USE competition_hub;

UPDATE competition_user
SET password_hash = '$2a$10$DTcFddZkkWQvaJ/UY1pHYeffr2sS.UpE3RCwfakGfYRqgtx60HNRi',
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'student'
  AND student_no = '20230118'
  AND deleted = 0;
