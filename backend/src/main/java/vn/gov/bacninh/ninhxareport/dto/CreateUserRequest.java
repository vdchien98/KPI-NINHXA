package vn.gov.bacninh.ninhxareport.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateUserRequest {
    
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
    
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
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
    
    private String loginMethod; // 'SSO' or 'PASSWORD'
    
    private Boolean isActive = true;
}

