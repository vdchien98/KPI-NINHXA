-- Thêm trường representative_type vào bảng users
ALTER TABLE users 
ADD COLUMN representative_type VARCHAR(20) COMMENT 'Loại đại diện: organization, department, hoặc NULL (không phải đại diện)';

-- Thêm index
CREATE INDEX idx_representative_type ON users(representative_type);

