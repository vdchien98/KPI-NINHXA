-- Tạo bảng lưu file đính kèm của report request
CREATE TABLE IF NOT EXISTS report_request_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_request_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL COMMENT 'Tên file gốc',
    file_path VARCHAR(500) NOT NULL COMMENT 'Đường dẫn file trên server',
    file_type VARCHAR(100) COMMENT 'Loại file (MIME type)',
    file_size BIGINT COMMENT 'Kích thước file (bytes)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (report_request_id) REFERENCES report_requests(id) ON DELETE CASCADE,
    INDEX idx_report_request_id (report_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

