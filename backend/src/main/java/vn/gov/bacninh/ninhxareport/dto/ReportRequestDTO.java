package vn.gov.bacninh.ninhxareport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.gov.bacninh.ninhxareport.entity.ReportRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestDTO {
    private Long id;
    private String title;
    private String description;
    private UserDTO createdBy;
    private List<OrganizationDTO> targetOrganizations;
    private List<DepartmentDTO> targetDepartments;
    private List<UserDTO> targetUsers;
    private LocalDateTime deadline;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Thống kê trạng thái responses
    private Integer completedCount; // Số người đã hoàn thành (có score)
    private Integer submittedCount; // Số người đã nộp, chờ đánh giá (có response nhưng chưa có score)
    private Integer pendingCount; // Số người chưa nộp
    private Set<Long> completedUserIds; // Danh sách userId đã hoàn thành
    
    // Status của response của user hiện tại (dùng cho inbox)
    private String myResponseStatus; // PENDING, SUBMITTED, COMPLETED
    
    public static ReportRequestDTO fromEntity(ReportRequest request) {
        if (request == null) return null;
        
        return ReportRequestDTO.builder()
                .id(request.getId())
                .title(request.getTitle())
                .description(request.getDescription())
                .createdBy(request.getCreatedBy() != null ? 
                    UserDTO.builder()
                        .id(request.getCreatedBy().getId())
                        .fullName(request.getCreatedBy().getFullName())
                        .email(request.getCreatedBy().getEmail())
                        .build() : null)
                .targetOrganizations(request.getTargetOrganizations() != null ?
                    request.getTargetOrganizations().stream()
                        .map(OrganizationDTO::fromEntity)
                        .collect(Collectors.toList()) : null)
                .targetDepartments(request.getTargetDepartments() != null ?
                    request.getTargetDepartments().stream()
                        .map(DepartmentDTO::fromEntity)
                        .collect(Collectors.toList()) : null)
                .targetUsers(request.getTargetUsers() != null ?
                    request.getTargetUsers().stream()
                        .map(u -> UserDTO.builder()
                            .id(u.getId())
                            .fullName(u.getFullName())
                            .email(u.getEmail())
                            .build())
                        .collect(Collectors.toList()) : null)
                .deadline(request.getDeadline())
                .status(request.getStatus() != null ? request.getStatus().name() : null)
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}

