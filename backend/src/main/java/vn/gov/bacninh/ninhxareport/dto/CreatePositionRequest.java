package vn.gov.bacninh.ninhxareport.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePositionRequest {
    
    @NotBlank(message = "Tên chức vụ không được để trống")
    private String name;
    
    private String code;
    
    private String description;
    
    private Integer displayOrder = 0;
    
    private Boolean isActive = true;
}

