package vn.gov.bacninh.ninhxareport.dto;

import lombok.*;
import vn.gov.bacninh.ninhxareport.entity.ReportResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponseDTO {
    private Long id;
    private Long reportRequestId;
    private String reportRequestTitle;
    private UserDTO submittedBy;
    private String note;
    private List<ReportResponseItemDTO> items;
    private LocalDateTime submittedAt;
    private Double score; // Điểm đánh giá của người gửi yêu cầu
    private UserDTO evaluatedBy; // Người đánh giá (người gửi yêu cầu)
    private LocalDateTime evaluatedAt; // Thời gian đánh giá của người gửi yêu cầu
    private Double selfScore; // Điểm tự đánh giá của người nộp báo cáo
    private LocalDateTime selfEvaluatedAt; // Thời gian tự đánh giá của người nộp báo cáo
    private String comment; // Nhận xét của người đánh giá
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static ReportResponseDTO fromEntity(ReportResponse entity) {
        if (entity == null) return null;
        
        return ReportResponseDTO.builder()
                .id(entity.getId())
                .reportRequestId(entity.getReportRequest() != null ? entity.getReportRequest().getId() : null)
                .reportRequestTitle(entity.getReportRequest() != null ? entity.getReportRequest().getTitle() : null)
                .submittedBy(entity.getSubmittedBy() != null ? UserDTO.fromEntity(entity.getSubmittedBy()) : null)
                .note(entity.getNote())
                .items(entity.getItems() != null ? 
                        entity.getItems().stream()
                                .map(ReportResponseItemDTO::fromEntity)
                                .collect(Collectors.toList()) : null)
                .submittedAt(entity.getSubmittedAt())
                .score(entity.getScore())
                .evaluatedBy(entity.getEvaluatedBy() != null ? UserDTO.fromEntity(entity.getEvaluatedBy()) : null)
                .evaluatedAt(entity.getEvaluatedAt())
                .selfScore(entity.getSelfScore())
                .selfEvaluatedAt(entity.getSelfEvaluatedAt())
                .comment(entity.getComment())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

