package vn.gov.bacninh.ninhxareport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponseHistoryDTO {
    private Long id;
    private Integer version;
    private LocalDateTime editedAt;
    private UserDTO editedBy;
    private String note;
    private List<ReportResponseItemDTO> items;
}


