-- 仅在表为空时插入，schema.sql 的 IF NOT EXISTS 保证表结构安全
-- admin/admin 的 BCrypt 值
INSERT OR IGNORE INTO merchant_user (id, username, password, must_change_password)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 1);
