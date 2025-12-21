package vn.gov.bacninh.ninhxareport.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminUpdateReportRequestDTO {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;
    
    private String description;
    
    private LocalDateTime deadline;
    
    private String status; // PENDING, COMPLETED, CANCELLED
    
    private List<Long> organizationIds;
    
    private List<Long> departmentIds;
}

