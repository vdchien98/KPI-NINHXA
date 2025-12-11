package vn.gov.bacninh.ninhxareport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.gov.bacninh.ninhxareport.entity.Department;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDTO {
    private Long id;
    private String name;
    private String code;
    private Long organizationId;
    private String organizationName;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static DepartmentDTO fromEntity(Department dept) {
        if (dept == null) return null;
        
        return DepartmentDTO.builder()
                .id(dept.getId())
                .name(dept.getName())
                .code(dept.getCode())
                .organizationId(dept.getOrganization() != null ? dept.getOrganization().getId() : null)
                .organizationName(dept.getOrganization() != null ? dept.getOrganization().getName() : null)
                .description(dept.getDescription())
                .isActive(dept.getIsActive())
                .createdAt(dept.getCreatedAt())
                .updatedAt(dept.getUpdatedAt())
                .build();
    }
}

