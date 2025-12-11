package vn.gov.bacninh.ninhxareport.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRoleRequest {
    
    @NotBlank(message = "Tên role không được để trống")
    private String name;
    
    private String description;
    
    private Long parentId;
}

