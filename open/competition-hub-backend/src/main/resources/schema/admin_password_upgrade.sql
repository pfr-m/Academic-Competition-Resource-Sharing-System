USE competition_hub;

UPDATE competition_user
SET password_hash = '$2a$10$2yJDuDBZmzMY2v8YMRPeM.Bjqv0SdrTTukyoaYT6f0h6Fd80rXfg6',
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'admin'
  AND deleted = 0;
