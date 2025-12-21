-- Thêm field để track lần cuối gửi thông báo deadline
ALTER TABLE report_requests 
ADD COLUMN last_deadline_notification_sent_at TIMESTAMP NULL 
COMMENT 'Thời điểm lần cuối gửi thông báo sắp đến hạn';

