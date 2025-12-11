package vn.gov.bacninh.ninhxareport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.gov.bacninh.ninhxareport.entity.Role;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDTO {
    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private String parentName;
    private Integer level;
    private List<RoleDTO> children;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static RoleDTO fromEntity(Role role) {
        if (role == null) return null;
        
        return RoleDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .parentId(role.getParent() != null ? role.getParent().getId() : null)
                .parentName(role.getParent() != null ? role.getParent().getName() : null)
                .level(role.getLevel())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }
    
    public static RoleDTO fromEntityWithChildren(Role role) {
        if (role == null) return null;
        
        RoleDTO dto = fromEntity(role);
        if (role.getChildren() != null && !role.getChildren().isEmpty()) {
            dto.setChildren(role.getChildren().stream()
                    .map(RoleDTO::fromEntityWithChildren)
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}

