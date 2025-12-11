package vn.gov.bacninh.ninhxareport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDepartmentRequest {
    
    @NotBlank(message = "Tên phòng ban không được để trống")
    private String name;
    
    private String code;
    
    @NotNull(message = "Cơ quan không được để trống")
    private Long organizationId;
    
    private String description;
    
    private Boolean isActive = true;
}

