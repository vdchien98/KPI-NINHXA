package vn.gov.bacninh.ninhxareport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateReportRequestDTO {
    
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;
    
    private String description;
    
    private List<Long> organizationIds;
    
    private List<Long> departmentIds;
    
    private List<Long> userIds;
    
    @NotNull(message = "Thời hạn báo cáo không được để trống")
    private LocalDateTime deadline;
    
    // For update operations - list of attachment IDs to delete
    private List<Long> deletedAttachmentIds;
}

