package vn.gov.bacninh.ninhxareport.dto;

import lombok.*;
import vn.gov.bacninh.ninhxareport.entity.ReportRequestAttachment;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequestAttachmentDTO {
    private Long id;
    private String fileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    
    public static ReportRequestAttachmentDTO fromEntity(ReportRequestAttachment entity) {
        if (entity == null) return null;
        
        return ReportRequestAttachmentDTO.builder()
                .id(entity.getId())
                .fileName(entity.getFileName())
                .filePath(entity.getFilePath())
                .fileType(entity.getFileType())
                .fileSize(entity.getFileSize())
                .build();
    }
}

