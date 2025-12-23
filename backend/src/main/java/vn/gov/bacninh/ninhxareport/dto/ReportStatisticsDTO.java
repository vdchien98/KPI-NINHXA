package vn.gov.bacninh.ninhxareport.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportStatisticsDTO {
    private List<ReportStatisticItemDTO> reports;
    private SummaryDTO summary;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReportStatisticItemDTO {
        private Long id;
        private Integer stt; // Số thứ tự
        private String reportName; // Tên báo cáo
        private UserDTO reportAuthor; // Người thực hiện báo cáo
        private DepartmentDTO department; // Phòng ban
        private List<OrganizationDTO> organizations; // Cơ quan
        private Double score; // Điểm
        private UserDTO reviewer; // Người chấm
        private LocalDateTime submissionDate; // Ngày giao báo cáo
        private String status; // Trạng thái (Đúng hạn / Quá hạn)
        private String documentLink; // Link tài liệu kiểm chứng (HTML string với links)
        private List<DocumentFileDTO> documentFiles; // Danh sách file với tên và link
        private Long reportRequestId; // ID của yêu cầu báo cáo
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentFileDTO {
        private String fileName; // Tên file gốc
        private String filePath; // Đường dẫn file để tạo link
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SummaryDTO {
        private Integer totalReports; // Tổng số báo cáo
        private Integer onTimeReports; // Số báo cáo đúng hạn
        private Integer overdueReports; // Số báo cáo quá hạn
        private Double averageScore; // Điểm trung bình
        private String rating; // Xếp loại (A, B, C, D)
    }
}

