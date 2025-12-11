package vn.gov.bacninh.ninhxareport.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOrganizationRequest {
    
    @NotBlank(message = "Tên cơ quan không được để trống")
    private String name;
    
    private String code;
    
    private String address;
    
    private String phone;
    
    private String email;
    
    private Boolean isActive = true;
}

