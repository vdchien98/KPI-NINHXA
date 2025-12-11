package vn.gov.bacninh.ninhxareport.dto;

import lombok.*;
import vn.gov.bacninh.ninhxareport.entity.ReportResponseItem;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponseItemDTO {
    private Long id;
    private String title; // Tiêu đề mục báo cáo
    private String content; // Nội dung mục báo cáo
    private Integer progress; // Tiến độ hoàn thành (0-100%)
    private String difficulties; // Khó khăn gặp phải
    private String fileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    
    public static ReportResponseItemDTO fromEntity(ReportResponseItem entity) {
        if (entity == null) return null;
        
        return ReportResponseItemDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .progress(entity.getProgress())
                .difficulties(entity.getDifficulties())
                .fileName(entity.getFileName())
                .filePath(entity.getFilePath())
                .fileType(entity.getFileType())
                .fileSize(entity.getFileSize())
                .displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

