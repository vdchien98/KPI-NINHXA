package vn.gov.bacninh.ninhxareport.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.gov.bacninh.ninhxareport.dto.*;
import vn.gov.bacninh.ninhxareport.service.AdminReportService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {
    
    @Autowired
    private AdminReportService adminReportService;
    
    /**
     * Lấy tất cả báo cáo với tìm kiếm và filter
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReportRequestDTO>>> getAllReports(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long createdById,
            @RequestParam(required = false) Long submittedById,
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) Long departmentId) {
        try {
            List<ReportRequestDTO> reports = adminReportService.getAllReportsWithFilters(
                    search, status, createdById, submittedById, organizationId, departmentId);
            return ResponseEntity.ok(ApiResponse.success(reports));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Thống kê báo cáo
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        try {
            Map<String, Object> statistics = adminReportService.getReportStatistics();
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Cập nhật yêu cầu báo cáo (admin)
     */
    @PutMapping("/requests/{id}")
    public ResponseEntity<ApiResponse<ReportRequestDTO>> updateReportRequest(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateReportRequestDTO dto) {
        try {
            ReportRequestDTO updated = adminReportService.adminUpdateReportRequest(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật yêu cầu báo cáo thành công", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Cập nhật nội dung báo cáo (admin)
     */
    @PutMapping("/responses/{id}")
    public ResponseEntity<ApiResponse<ReportResponseDTO>> updateReportResponse(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateReportResponseDTO dto) {
        try {
            ReportResponseDTO updated = adminReportService.adminUpdateReportResponse(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật nội dung báo cáo thành công", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Lấy chi tiết response theo ID
     */
    @GetMapping("/responses/{id}")
    public ResponseEntity<ApiResponse<ReportResponseDTO>> getResponseById(@PathVariable Long id) {
        try {
            ReportResponseDTO response = adminReportService.getResponseById(id);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

