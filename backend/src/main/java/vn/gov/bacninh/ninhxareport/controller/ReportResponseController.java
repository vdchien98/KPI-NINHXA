package vn.gov.bacninh.ninhxareport.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.gov.bacninh.ninhxareport.dto.*;
import vn.gov.bacninh.ninhxareport.entity.User;
import vn.gov.bacninh.ninhxareport.repository.UserRepository;
import vn.gov.bacninh.ninhxareport.service.FileStorageService;
import vn.gov.bacninh.ninhxareport.service.ReportResponseService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/report-responses")
@PreAuthorize("isAuthenticated()")
public class ReportResponseController {
    
    @Autowired
    private ReportResponseService reportResponseService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private UserRepository userRepository;
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }
    
    @GetMapping("/by-request/{requestId}")
    public ResponseEntity<ApiResponse<List<ReportResponseDTO>>> getByRequestId(@PathVariable Long requestId) {
        List<ReportResponseDTO> responses = reportResponseService.getByReportRequestId(requestId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    @GetMapping("/my-responses")
    public ResponseEntity<ApiResponse<List<ReportResponseDTO>>> getMyResponses() {
        User currentUser = getCurrentUser();
        List<ReportResponseDTO> responses = reportResponseService.getMyResponses(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReportResponseDTO>> getById(@PathVariable Long id) {
        ReportResponseDTO response = reportResponseService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/by-request/{requestId}/my")
    public ResponseEntity<ApiResponse<ReportResponseDTO>> getMyResponseForRequest(@PathVariable Long requestId) {
        User currentUser = getCurrentUser();
        ReportResponseDTO response = reportResponseService.getByRequestAndUser(requestId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponseDTO>> createResponse(
            @Valid @RequestBody CreateReportResponseDTO dto) {
        User currentUser = getCurrentUser();
        ReportResponseDTO response = reportResponseService.createResponse(dto, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Nộp báo cáo thành công", response));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReportResponseDTO>> updateResponse(
            @PathVariable Long id,
            @Valid @RequestBody CreateReportResponseDTO dto) {
        User currentUser = getCurrentUser();
        ReportResponseDTO response = reportResponseService.updateResponse(id, dto, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật báo cáo thành công", response));
    }
    
    @PostMapping("/{responseId}/items")
    public ResponseEntity<ApiResponse<ReportResponseItemDTO>> addItem(
            @PathVariable Long responseId,
            @RequestParam("content") String content,
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        User currentUser = getCurrentUser();
        ReportResponseItemDTO item = reportResponseService.addItemWithFile(responseId, content, file, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Thêm nội dung thành công", item));
    }
    
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable Long itemId) {
        User currentUser = getCurrentUser();
        reportResponseService.deleteItem(itemId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Xóa nội dung thành công", null));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteResponse(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        reportResponseService.deleteResponse(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Xóa báo cáo thành công", null));
    }
    
    @PostMapping("/items/{itemId}/file")
    public ResponseEntity<ApiResponse<ReportResponseItemDTO>> uploadItemFile(
            @PathVariable Long itemId,
            @RequestParam("file") MultipartFile file) throws IOException {
        User currentUser = getCurrentUser();
        ReportResponseItemDTO item = reportResponseService.updateItemFile(itemId, file, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Upload file thành công", item));
    }
    
    @GetMapping("/{responseId}/history")
    public ResponseEntity<ApiResponse<List<ReportResponseHistoryDTO>>> getHistory(
            @PathVariable Long responseId) {
        User currentUser = getCurrentUser();
        List<ReportResponseHistoryDTO> histories = reportResponseService.getHistory(responseId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(histories));
    }
    
    @GetMapping("/{responseId}/comments")
    public ResponseEntity<ApiResponse<List<ReportResponseCommentDTO>>> getCommentHistory(
            @PathVariable Long responseId) {
        try {
            List<ReportResponseCommentDTO> comments = reportResponseService.getCommentHistory(responseId);
            return ResponseEntity.ok(ApiResponse.success(comments));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{responseId}/evaluate")
    public ResponseEntity<ApiResponse<ReportResponseDTO>> evaluateResponse(
            @PathVariable Long responseId,
            @RequestParam Double score,
            @RequestParam(required = false) String comment) {
        try {
            User currentUser = getCurrentUser();
            ReportResponseDTO response = reportResponseService.evaluateResponse(responseId, score, comment, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("Đánh giá báo cáo thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{responseId}/send-back")
    public ResponseEntity<ApiResponse<ReportResponseDTO>> sendBackForRevision(
            @PathVariable Long responseId,
            @RequestParam String comment) {
        try {
            User currentUser = getCurrentUser();
            ReportResponseDTO response = reportResponseService.sendBackForRevision(responseId, comment, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("Đã gửi lại người nộp báo cáo", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{responseId}/self-evaluate")
    public ResponseEntity<ApiResponse<ReportResponseDTO>> selfEvaluateResponse(
            @PathVariable Long responseId,
            @RequestParam Double score) {
        try {
            User currentUser = getCurrentUser();
            ReportResponseDTO response = reportResponseService.selfEvaluateResponse(responseId, score, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("Tự đánh giá báo cáo thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/files/**")
    public ResponseEntity<Resource> downloadFile(HttpServletRequest request) {
        try {
            // Extract file path after "/files/"
            String requestUri = request.getRequestURI(); // e.g., /api/report-responses/files/reports/4/abc.pdf
            String prefix = request.getContextPath() + "/report-responses/files/";
            int idx = requestUri.indexOf(prefix);
            if (idx == -1) {
                throw new RuntimeException("File path không hợp lệ");
            }
            String filePath = requestUri.substring(idx + prefix.length());
            
            Path path = fileStorageService.getFilePath(filePath);
            Resource resource = new UrlResource(path.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                // Determine content type based on file extension
                String contentType = determineContentType(filePath);
                
                // Get original filename from path (last part)
                String filename = path.getFileName().toString();
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                        .header(HttpHeaders.PRAGMA, "no-cache")
                        .header(HttpHeaders.EXPIRES, "0")
                        .body(resource);
            } else {
                throw new RuntimeException("File not found");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("File not found", e);
        }
    }
    
    private String determineContentType(String filePath) {
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerPath.endsWith(".png")) {
            return "image/png";
        } else if (lowerPath.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerPath.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}

