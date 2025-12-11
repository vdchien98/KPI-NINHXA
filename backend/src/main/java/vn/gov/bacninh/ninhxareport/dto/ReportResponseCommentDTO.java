package vn.gov.bacninh.ninhxareport.dto;

import lombok.*;
import vn.gov.bacninh.ninhxareport.entity.ReportResponseComment;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponseCommentDTO {
    private Long id;
    private String comment;
    private Double score;
    private Boolean isFinalEvaluation;
    private LocalDateTime commentedAt;
    private UserDTO commentedBy;
    
    public static ReportResponseCommentDTO fromEntity(ReportResponseComment entity) {
        if (entity == null) return null;
        
        return ReportResponseCommentDTO.builder()
                .id(entity.getId())
                .comment(entity.getComment())
                .score(entity.getScore())
                .isFinalEvaluation(entity.getIsFinalEvaluation())
                .commentedAt(entity.getCommentedAt())
                .commentedBy(entity.getCommentedBy() != null ? UserDTO.fromEntity(entity.getCommentedBy()) : null)
                .build();
    }
}

