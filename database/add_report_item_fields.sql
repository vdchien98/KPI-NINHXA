-- Thêm các trường mới vào bảng report_response_items
ALTER TABLE report_response_items 
ADD COLUMN title VARCHAR(500) COMMENT 'Tiêu đề mục báo cáo' AFTER report_response_id,
ADD COLUMN progress INT DEFAULT 0 COMMENT 'Tiến độ hoàn thành (0-100%)' AFTER content,
ADD COLUMN difficulties VARCHAR(2000) COMMENT 'Khó khăn gặp phải' AFTER progress;

-- Cập nhật content để không bắt buộc nữa (vì có thể chỉ có title)
ALTER TABLE report_response_items 
MODIFY COLUMN content VARCHAR(2000) COMMENT 'Nội dung mục báo cáo';

