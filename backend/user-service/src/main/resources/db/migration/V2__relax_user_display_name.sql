-- user-service: display_name là profile hữu ích nhưng KHÔNG bắt buộc.
-- auth publish UserRegistered kèm displayName, nhưng flow khác (OAuth login, producer cũ)
-- có thể không mang tên → cho phép NULL để upsert không crash NOT-NULL.
ALTER TABLE users ALTER COLUMN display_name DROP NOT NULL;
