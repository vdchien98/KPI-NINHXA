package vn.gov.bacninh.ninhxareport.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReportResponseDTO {
    
    @NotNull(message = "Report request ID is required")
    private Long reportRequestId;
    
    private String note;
    
    private List<CreateReportResponseItemDTO> items;
}

