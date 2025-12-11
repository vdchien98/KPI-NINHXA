package vn.gov.bacninh.ninhxareport.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRequest {
    
    @Email(message = "Email không hợp lệ")
    private String email;
    
    private String password;
    
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
    
    private String phone;
    
    private String avatar;
    
    private Long roleId;
    
    private List<Long> organizationIds;
    
    private Long departmentId;
    
    private Long positionId;
    
    private String representativeType; // 'organization', 'department', or null
    
    private Boolean isActive;
}

