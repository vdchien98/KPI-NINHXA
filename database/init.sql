-- ============================================
-- Script khởi tạo Database cho hệ thống báo cáo Ninh Xá
-- ============================================

-- Tạo database nếu chưa tồn tại
CREATE DATABASE IF NOT EXISTS ninhxa_report CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ninhxa_report;

-- ============================================
-- 1. Bảng Role (hỗ trợ cấu trúc cây - parent_id)
-- ============================================
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    parent_id BIGINT NULL,
    level INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES roles(id) ON DELETE SET NULL,
    INDEX idx_parent_id (parent_id),
    INDEX idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Bảng vai trò người dùng';

-- ============================================
-- 2. Bảng Cơ quan (Organization)
-- ============================================
CREATE TABLE IF NOT EXISTS organizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) UNIQUE,
    address VARCHAR(500),
    phone VARCHAR(20),
    email VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (code),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Bảng cơ quan tổ chức';

-- ============================================
-- 3. Bảng Phòng ban (Department)
-- ============================================
CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50),
    organization_id BIGINT NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    UNIQUE KEY unique_dept_org (code, organization_id),
    INDEX idx_organization_id (organization_id),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Bảng phòng ban';

-- ============================================
-- 4. Bảng Chức vụ (Position)
-- ============================================
CREATE TABLE IF NOT EXISTS positions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) UNIQUE,
    description VARCHAR(500),
    display_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (code),
    INDEX idx_display_order (display_order),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Bảng chức vụ';

-- ============================================
-- 5. Bảng Users
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    avatar VARCHAR(500),
    role_id BIGINT,
    department_id BIGINT,
    position_id BIGINT,
    representative_type VARCHAR(20) COMMENT 'Loại đại diện: organization, department, hoặc NULL (không phải đại diện)',
    is_active BOOLEAN DEFAULT TRUE,
    last_login TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE SET NULL,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL,
    FOREIGN KEY (position_id) REFERENCES positions(id) ON DELETE SET NULL,
    INDEX idx_email (email),
    INDEX idx_role_id (role_id),
    INDEX idx_department_id (department_id),
    INDEX idx_representative_type (representative_type),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Bảng người dùng';

-- ============================================
-- 6. Bảng trung gian: User - Organization
-- ============================================
CREATE TABLE IF NOT EXISTS user_organizations (
    user_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, organization_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    INDEX idx_organization_id (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Bảng liên kết người dùng với cơ quan';

-- ============================================
-- 7. Bảng Yêu cầu báo cáo (report_requests)
-- ============================================
CREATE TABLE IF NOT EXISTS report_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL COMMENT 'Tiêu đề yêu cầu báo cáo',
    description VARCHAR(2000) COMMENT 'Mô tả chi tiết yêu cầu',
    created_by BIGINT NOT NULL COMMENT 'Người tạo yêu cầu',
    deadline DATETIME NOT NULL COMMENT 'Hạn nộp báo cáo',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'Trạng thái: PENDING, IN_PROGRESS, SUBMITTED, COMPLETED, CANCELLED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời gian tạo',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời gian cập nhật',
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_status (status),
    INDEX idx_created_by (created_by),
    INDEX idx_deadline (deadline),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Bảng lưu thông tin yêu cầu báo cáo';

-- ============================================
-- 8. Bảng Báo cáo của người nộp (report_responses)
-- ============================================
CREATE TABLE IF NOT EXISTS report_responses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_request_id BIGINT NOT NULL COMMENT 'ID yêu cầu báo cáo',
    submitted_by BIGINT NOT NULL COMMENT 'Người nộp báo cáo',
    note VARCHAR(2000) COMMENT 'Ghi chú của người nộp',
    submitted_at DATETIME COMMENT 'Thời gian nộp báo cáo',
    score DOUBLE COMMENT 'Điểm đánh giá của người gửi yêu cầu (0-10)',
    evaluated_by BIGINT COMMENT 'Người đánh giá (người gửi yêu cầu)',
    evaluated_at DATETIME COMMENT 'Thời gian đánh giá của người gửi yêu cầu',
    self_score DOUBLE COMMENT 'Điểm tự đánh giá của người nộp báo cáo (0-10)',
    self_evaluated_at DATETIME COMMENT 'Thời gian tự đánh giá của người nộp báo cáo',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời gian tạo',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Thời gian cập nhật',
    FOREIGN KEY (report_request_id) REFERENCES report_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (submitted_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (evaluated_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_report_request_id (report_request_id),
    INDEX idx_submitted_by (submitted_by),
    INDEX idx_evaluated_by (evaluated_by),
    INDEX idx_submitted_at (submitted_at),
    INDEX idx_score (score),
    INDEX idx_self_score (self_score),
    UNIQUE KEY unique_user_request (report_request_id, submitted_by) COMMENT 'Mỗi người chỉ nộp 1 báo cáo cho 1 yêu cầu'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Bảng lưu báo cáo của người nộp';

-- ============================================
-- 9. Bảng Chi tiết các mục trong báo cáo (report_response_items)
-- ============================================
CREATE TABLE IF NOT EXISTS report_response_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_response_id BIGINT NOT NULL COMMENT 'ID báo cáo',
    title VARCHAR(500) COMMENT 'Tiêu đề mục báo cáo',
    content VARCHAR(2000) COMMENT 'Nội dung mục báo cáo',
    progress INT DEFAULT 0 COMMENT 'Tiến độ hoàn thành (0-100%)',
    difficulties VARCHAR(2000) COMMENT 'Khó khăn gặp phải',
    file_name VARCHAR(500) COMMENT 'Tên file đính kèm',
    file_path VARCHAR(1000) COMMENT 'Đường dẫn file đính kèm',
    file_type VARCHAR(100) COMMENT 'Loại file (MIME type)',
    file_size BIGINT COMMENT 'Kích thước file (bytes)',
    display_order INT DEFAULT 0 COMMENT 'Thứ tự hiển thị',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời gian tạo',
    FOREIGN KEY (report_response_id) REFERENCES report_responses(id) ON DELETE CASCADE,
    INDEX idx_report_response_id (report_response_id),
    INDEX idx_display_order (display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Bảng lưu chi tiết các mục trong báo cáo';

-- ============================================
-- 10. Bảng trung gian: Yêu cầu báo cáo - Tổ chức (report_request_organizations)
-- ============================================
CREATE TABLE IF NOT EXISTS report_request_organizations (
    report_request_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    PRIMARY KEY (report_request_id, organization_id),
    FOREIGN KEY (report_request_id) REFERENCES report_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    INDEX idx_organization_id (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Bảng liên kết yêu cầu báo cáo với tổ chức';

-- ============================================
-- 11. Bảng trung gian: Yêu cầu báo cáo - Phòng ban (report_request_departments)
-- ============================================
CREATE TABLE IF NOT EXISTS report_request_departments (
    report_request_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    PRIMARY KEY (report_request_id, department_id),
    FOREIGN KEY (report_request_id) REFERENCES report_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE,
    INDEX idx_department_id (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Bảng liên kết yêu cầu báo cáo với phòng ban';

-- ============================================
-- 12. Bảng trung gian: Yêu cầu báo cáo - Người dùng (report_request_users)
-- ============================================
CREATE TABLE IF NOT EXISTS report_request_users (
    report_request_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (report_request_id, user_id),
    FOREIGN KEY (report_request_id) REFERENCES report_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Bảng liên kết yêu cầu báo cáo với người dùng';

-- ============================================
-- Chèn dữ liệu mặc định
-- ============================================

-- Thêm Role Admin
INSERT IGNORE INTO roles (id, name, description, parent_id, level) VALUES 
(1, 'Admin', 'Quản trị Viên hệ thống - Quyền cao nhất', NULL, 0);

-- Thêm Role mặc định khác
INSERT IGNORE INTO roles (name, description, parent_id, level) VALUES 


-- Thêm Cơ quan mặc định
INSERT IGNORE INTO organizations (id, name, code, address, phone, email) VALUES 
s

-- Thêm Phòng ban mặc định
INSERT IGNORE INTO departments (id, name, code, organization_id, description) VALUES 

-- Thêm tài khoản Admin mặc định
-- Mật khẩu: admin123 (đã được mã hóa BCrypt)
INSERT IGNORE INTO users (id, email, password, full_name, phone, role_id, department_id, is_active) VALUES 
(1, 'admin@bacninh.gov.vn', '$2a$10$slYQmyNdGzTn7ZLBXBChFOC9f6kFjAqPhccnP6DxlWXx2lPk1C3G6', 'Quản trị Viên', '0222.123.456', 1, 1, TRUE);

-- ============================================
-- Kiểm tra kết quả
-- ============================================
SELECT 
    TABLE_NAME,
    TABLE_COMMENT,
    TABLE_ROWS
FROM 
    INFORMATION_SCHEMA.TABLES
WHERE 
    TABLE_SCHEMA = 'ninhxa_report'
ORDER BY 
    TABLE_NAME;

