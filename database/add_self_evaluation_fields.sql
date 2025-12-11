-- Thêm trường lưu điểm tự đánh giá của người báo cáo
ALTER TABLE report_responses 
ADD COLUMN self_score DOUBLE COMMENT 'Điểm tự đánh giá của người nộp báo cáo (0-10)' AFTER score,
ADD COLUMN self_evaluated_at DATETIME COMMENT 'Thời gian tự đánh giá' AFTER evaluated_at;

-- Thêm index cho self_score
CREATE INDEX idx_self_score ON report_responses(self_score);

