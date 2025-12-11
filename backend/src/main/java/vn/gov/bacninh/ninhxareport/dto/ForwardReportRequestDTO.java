package vn.gov.bacninh.ninhxareport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ForwardReportRequestDTO {
    
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;
    
    private String description;
    
    private String forwardNote; // Ghi chú khi chuyển tiếp
    
    private List<Long> organizationIds;
    
    private List<Long> departmentIds;
    
    private List<Long> userIds;
    
    @NotNull(message = "Thời hạn báo cáo không được để trống")
    private LocalDateTime deadline;
}

