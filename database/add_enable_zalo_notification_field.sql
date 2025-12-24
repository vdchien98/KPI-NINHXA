-- Thêm field để bật/tắt tính năng gửi thông báo Zalo cho user
ALTER TABLE users 
ADD COLUMN enable_zalo_notification BOOLEAN DEFAULT TRUE 
COMMENT 'Bật/tắt tính năng gửi thông báo Zalo (TRUE = bật, FALSE = tắt)';

