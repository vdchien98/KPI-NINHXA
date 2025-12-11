package vn.gov.bacninh.ninhxareport.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReportResponseItemDTO {
    
    private String title; // Tiêu đề mục báo cáo
    
    private String content; // Nội dung mục báo cáo
    
    private Integer progress; // Tiến độ hoàn thành (0-100%)
    
    private String difficulties; // Khó khăn gặp phải
    
    private Integer displayOrder;
}

