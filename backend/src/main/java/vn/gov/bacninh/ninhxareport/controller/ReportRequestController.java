package vn.gov.bacninh.ninhxareport.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.gov.bacninh.ninhxareport.dto.ApiResponse;
import vn.gov.bacninh.ninhxareport.dto.CreateReportRequestDTO;
import vn.gov.bacninh.ninhxareport.dto.ForwardReportRequestDTO;
import vn.gov.bacninh.ninhxareport.dto.ReportRequestDTO;
import vn.gov.bacninh.ninhxareport.dto.ReportRequestHistoryDTO;
import vn.gov.bacninh.ninhxareport.entity.User;
import vn.gov.bacninh.ninhxareport.repository.UserRepository;
import vn.gov.bacninh.ninhxareport.service.ReportDeadlineNotificationService;
import vn.gov.bacninh.ninhxareport.service.ReportRequestService;
import vn.gov.bacninh.ninhxareport.service.ReportWordExportService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/report-requests")
public class ReportRequestController {
    
    @Autowired
    private ReportRequestService reportRequestService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ReportWordExportService wordExportService;
    
    @Autowired
    private ReportDeadlineNotificationService deadlineNotificationService;
    
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReportRequestDTO>>> getAllReportRequests() {
        List<ReportRequestDTO> requests = reportRequestService.getAllReportRequests();
        return ResponseEntity.ok(ApiResponse.success(requests));
    }
    
    @GetMapping("/my-requests")
    public ResponseEntity<ApiResponse<List<ReportRequestDTO>>> getMyReportRequests() {
        User currentUser = getCurrentUser();
        List<ReportRequestDTO> requests = reportRequestService.getMyReportRequests(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(requests));
    }
    
    @GetMapping("/received")
    public ResponseEntity<ApiResponse<List<ReportRequestDTO>>> getReceivedRequests() {
        User currentUser = getCurrentUser();
        List<ReportRequestDTO> requests = reportRequestService.getReceivedRequests(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(requests));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReportRequestDTO>> getById(@PathVariable Long id) {
        try {
            ReportRequestDTO request = reportRequestService.getById(id);
            return ResponseEntity.ok(ApiResponse.success(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<ReportRequestDTO>> createReportRequest(
            @Valid @RequestBody CreateReportRequestDTO dto) {
        try {
            User currentUser = getCurrentUser();
            ReportRequestDTO request = reportRequestService.createReportRequest(dto, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("Tạo yêu cầu báo cáo thành công", request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReportRequestDTO>> updateReportRequest(
            @PathVariable Long id,
            @Valid @RequestBody CreateReportRequestDTO dto) {
        try {
            User currentUser = getCurrentUser();
            ReportRequestDTO request = reportRequestService.updateReportRequest(id, dto, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("Cập nhật yêu cầu báo cáo thành công", request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<ReportRequestHistoryDTO>>> getHistory(@PathVariable Long id) {
        try {
            List<ReportRequestHistoryDTO> history = reportRequestService.getHistory(id);
            return ResponseEntity.ok(ApiResponse.success(history));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReportRequest(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            reportRequestService.deleteReportRequest(id, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("Xóa yêu cầu báo cáo thành công", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ReportRequestDTO>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        try {
            ReportRequestDTO request = reportRequestService.updateStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/forward")
    public ResponseEntity<ApiResponse<ReportRequestDTO>> forwardReportRequest(
            @PathVariable Long id,
            @Valid @RequestBody ForwardReportRequestDTO dto) {
        try {
            User currentUser = getCurrentUser();
            ReportRequestDTO request = reportRequestService.forwardReportRequest(id, dto, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("Chuyển tiếp yêu cầu báo cáo thành công", request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/{id}/export-word")
    public ResponseEntity<org.springframework.core.io.Resource> exportToWord(@PathVariable Long id) {
        try {
            byte[] wordBytes = wordExportService.generateWordDocument(id);
            org.springframework.core.io.ByteArrayResource resource = 
                new org.springframework.core.io.ByteArrayResource(wordBytes);
            
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"BaoCao_" + id + ".docx\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * API để admin chủ động gửi lại thông báo Zalo cho báo cáo sắp đến hạn
     */
    @PostMapping("/{id}/send-deadline-notification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Object>> sendDeadlineNotification(@PathVariable Long id) {
        try {
            int successCount = deadlineNotificationService.sendNotificationManually(id);
            return ResponseEntity.ok(ApiResponse.success(
                String.format("Đã gửi thông báo cho %d người nhận", successCount),
                Map.of("sentCount", successCount)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

