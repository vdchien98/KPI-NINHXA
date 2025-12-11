package vn.gov.bacninh.ninhxareport.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequestHistoryDTO {
    private Long id;
    private Integer version;
    private LocalDateTime editedAt;
    private UserDTO editedBy;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private List<OrganizationDTO> targetOrganizations;
    private List<DepartmentDTO> targetDepartments;
    private List<UserDTO> targetUsers;
}

