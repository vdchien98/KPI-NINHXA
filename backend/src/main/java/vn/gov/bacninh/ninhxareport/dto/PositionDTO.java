package vn.gov.bacninh.ninhxareport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.gov.bacninh.ninhxareport.entity.Position;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionDTO {
    private Long id;
    private String name;
    private String code;
    private String description;
    private Integer displayOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static PositionDTO fromEntity(Position position) {
        if (position == null) return null;
        
        return PositionDTO.builder()
                .id(position.getId())
                .name(position.getName())
                .code(position.getCode())
                .description(position.getDescription())
                .displayOrder(position.getDisplayOrder())
                .isActive(position.getIsActive())
                .createdAt(position.getCreatedAt())
                .updatedAt(position.getUpdatedAt())
                .build();
    }
}

