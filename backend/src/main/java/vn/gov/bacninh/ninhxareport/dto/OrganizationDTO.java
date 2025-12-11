package vn.gov.bacninh.ninhxareport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.gov.bacninh.ninhxareport.entity.Organization;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDTO {
    private Long id;
    private String name;
    private String code;
    private String address;
    private String phone;
    private String email;
    private Boolean isActive;
    private List<DepartmentDTO> departments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static OrganizationDTO fromEntity(Organization org) {
        if (org == null) return null;
        
        return OrganizationDTO.builder()
                .id(org.getId())
                .name(org.getName())
                .code(org.getCode())
                .address(org.getAddress())
                .phone(org.getPhone())
                .email(org.getEmail())
                .isActive(org.getIsActive())
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }
    
    public static OrganizationDTO fromEntityWithDepartments(Organization org) {
        if (org == null) return null;
        
        OrganizationDTO dto = fromEntity(org);
        if (org.getDepartments() != null && !org.getDepartments().isEmpty()) {
            dto.setDepartments(org.getDepartments().stream()
                    .map(DepartmentDTO::fromEntity)
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}

