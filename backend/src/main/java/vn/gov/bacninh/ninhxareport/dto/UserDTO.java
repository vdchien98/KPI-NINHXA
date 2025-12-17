package vn.gov.bacninh.ninhxareport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.gov.bacninh.ninhxareport.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String avatar;
    private RoleDTO role;
    private List<OrganizationDTO> organizations;
    private DepartmentDTO department;
    private PositionDTO position;
    private String representativeType; // 'organization', 'department', or null
    private String loginMethod; // 'SSO' or 'PASSWORD'
    private Boolean isActive;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    
    public static UserDTO fromEntity(User user) {
        if (user == null) return null;
        
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .role(user.getRole() != null ? RoleDTO.fromEntity(user.getRole()) : null)
                .organizations(user.getOrganizations() != null ? 
                    user.getOrganizations().stream()
                        .map(OrganizationDTO::fromEntity)
                        .collect(Collectors.toList()) : null)
                .department(user.getDepartment() != null ? DepartmentDTO.fromEntity(user.getDepartment()) : null)
                .position(user.getPosition() != null ? PositionDTO.fromEntity(user.getPosition()) : null)
                .representativeType(user.getRepresentativeType())
                .loginMethod(user.getLoginMethod() != null ? user.getLoginMethod().name() : "SSO")
                .isActive(user.getIsActive())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

