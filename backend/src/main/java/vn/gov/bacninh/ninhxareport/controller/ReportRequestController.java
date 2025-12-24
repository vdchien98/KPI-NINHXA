package vn.gov.bacninh.ninhxareport.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.gov.bacninh.ninhxareport.dto.ApiResponse;
import vn.gov.bacninh.ninhxareport.dto.CreateReportRequestDTO;
import vn.gov.bacninh.ninhxareport.dto.ForwardReportRequestDTO;
import vn.gov.bacninh.ninhxareport.dto.ReportRequestDTO;
import vn.gov.bacninh.ninhxareport.dto.ReportRequestHistoryDTO;
import vn.gov.bacninh.ninhxareport.entity.User;
import vn.gov.bacninh.ninhxareport.repository.UserRepository;
import vn.gov.bacninh.ninhxareport.service.FileStorageService;
import vn.gov.bacninh.ninhxareport.service.ReportDeadlineNotificationService;
import vn.gov.bacninh.ninhxareport.service.ReportRequestService;
import vn.gov.bacninh.ninhxareport.service.ReportWordExportService;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/report-requests")
public class ReportRequestController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportRequestController.class);
    
    @Autowired
    private ReportRequestService reportRequestService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ReportWordExportService wordExportService;
    
    @Autowired
    private ReportDeadlineNotificationService deadlineNotificationService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
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
    
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<ReportRequestDTO>> createReportRequest(
            @RequestPart("dto") String dtoJson,
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        try {
            User currentUser = getCurrentUser();
            CreateReportRequestDTO dto = objectMapper.readValue(dtoJson, CreateReportRequestDTO.class);
            ReportRequestDTO request = reportRequestService.createReportRequest(dto, currentUser.getId(), files);
            return ResponseEntity.ok(ApiResponse.success("Tạo yêu cầu báo cáo thành công", request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping(value = "/{id}", consumes = {"application/json"})
    public ResponseEntity<ApiResponse<ReportRequestDTO>> updateReportRequest(
            @PathVariable Long id,
            @Valid @RequestBody CreateReportRequestDTO dto) {
        try {
            User currentUser = getCurrentUser();
            ReportRequestDTO request = reportRequestService.updateReportRequest(id, dto, currentUser.getId(), null, null);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật yêu cầu báo cáo thành công", request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<ReportRequestDTO>> updateReportRequestWithFiles(
            @PathVariable Long id,
            @RequestPart("dto") String dtoJson,
            @RequestPart(value = "files", required = false) MultipartFile[] files) {
        try {
            User currentUser = getCurrentUser();
            CreateReportRequestDTO dto = objectMapper.readValue(dtoJson, CreateReportRequestDTO.class);
            List<Long> deletedAttachmentIds = dto.getDeletedAttachmentIds();
            ReportRequestDTO request = reportRequestService.updateReportRequest(id, dto, currentUser.getId(), files, deletedAttachmentIds);
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
    
    @GetMapping("/files/**")
    public ResponseEntity<Resource> downloadFile(HttpServletRequest request) {
        try {
            // Extract file path after "/files/"
            String requestUri = request.getRequestURI();
            String contextPath = request.getContextPath();
            String prefix = contextPath + "/report-requests/files/";
            
            // Find the position of the prefix
            int idx = requestUri.indexOf(prefix);
            if (idx == -1) {
                prefix = "/report-requests/files/";
                idx = requestUri.indexOf(prefix);
                if (idx == -1) {
                    logger.error("Cannot find prefix in URI: {}", requestUri);
                    return ResponseEntity.badRequest().build();
                }
            }
            
            String filePath = requestUri.substring(idx + prefix.length());
            
            // Decode URL if needed
            try {
                String decoded = java.net.URLDecoder.decode(filePath, "UTF-8");
                if (!decoded.equals(filePath)) {
                    filePath = decoded;
                }
            } catch (Exception e) {
                logger.warn("Failed to decode file path: {}", filePath, e);
            }
            
            // Normalize the path to prevent directory traversal
            Path normalizedPath = Paths.get(filePath).normalize();
            if (normalizedPath.toString().contains("..")) {
                logger.error("Directory traversal detected in path: {}", filePath);
                return ResponseEntity.badRequest().build();
            }
            
            // Get the full file path
            Path path = fileStorageService.getFilePath(filePath);
            
            // Check if file exists
            if (!Files.exists(path)) {
                logger.error("File not found: {}", path);
                return ResponseEntity.notFound().build();
            }
            
            long fileSize = Files.size(path);
            FileTime lastModified = Files.getLastModifiedTime(path);
            String contentType = determineContentType(filePath);
            String filename = path.getFileName().toString();
            
            // Parse Range header for partial content support
            String rangeHeader = request.getHeader(HttpHeaders.RANGE);
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                return handleRangeRequest(path, rangeHeader, fileSize, lastModified, contentType, filename);
            }
            
            // Full file request - use streaming resource
            Resource resource = new UrlResource(path.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                // Generate ETag for caching
                String etag = generateETag(path, lastModified, fileSize);
                String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
                if (etag.equals(ifNoneMatch)) {
                    return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                            .header(HttpHeaders.ETAG, etag)
                            .build();
                }
                
                // Check If-Modified-Since
                String ifModifiedSince = request.getHeader(HttpHeaders.IF_MODIFIED_SINCE);
                if (ifModifiedSince != null) {
                    try {
                        ZonedDateTime modifiedSince = ZonedDateTime.parse(ifModifiedSince, DateTimeFormatter.RFC_1123_DATE_TIME);
                        ZonedDateTime fileModified = ZonedDateTime.ofInstant(lastModified.toInstant(), ZoneId.systemDefault());
                        if (!fileModified.isAfter(modifiedSince)) {
                            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                                    .header(HttpHeaders.ETAG, etag)
                                    .build();
                        }
                    } catch (Exception e) {
                        // Ignore parse errors
                    }
                }
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .contentLength(fileSize)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .header(HttpHeaders.ETAG, etag)
                        .lastModified(lastModified.toMillis())
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600, must-revalidate")
                        .body(resource);
            } else {
                logger.error("File exists but is not readable: {}", path);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error serving file", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    private ResponseEntity<Resource> handleRangeRequest(Path path, String rangeHeader, long fileSize, 
                                                       FileTime lastModified, String contentType, String filename) {
        try {
            // Parse range: bytes=start-end
            String range = rangeHeader.substring(6); // Remove "bytes="
            String[] ranges = range.split(",");
            if (ranges.length > 1) {
                // Multiple ranges not supported, return full file
                Resource resource = new UrlResource(path.toUri());
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                        .build();
            }
            
            String[] rangeParts = ranges[0].split("-");
            long start = 0;
            long end = fileSize - 1;
            
            if (rangeParts.length == 2) {
                if (!rangeParts[0].isEmpty()) {
                    start = Long.parseLong(rangeParts[0]);
                }
                if (!rangeParts[1].isEmpty()) {
                    end = Long.parseLong(rangeParts[1]);
                }
            } else if (rangeParts.length == 1) {
                if (rangeParts[0].startsWith("-")) {
                    // Suffix range: -500 means last 500 bytes
                    long suffix = Long.parseLong(rangeParts[0].substring(1));
                    start = Math.max(0, fileSize - suffix);
                    end = fileSize - 1;
                } else {
                    // Prefix range: 500- means from byte 500 to end
                    start = Long.parseLong(rangeParts[0]);
                    end = fileSize - 1;
                }
            }
            
            // Validate range
            if (start < 0 || end >= fileSize || start > end) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                        .build();
            }
            
            long contentLength = end - start + 1;
            String contentRange = String.format("bytes %d-%d/%d", start, end, fileSize);
            
            // Create partial resource
            Resource resource = new PartialFileResource(path, start, end);
            String etag = generateETag(path, lastModified, fileSize);
            
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(contentLength)
                    .header(HttpHeaders.CONTENT_RANGE, contentRange)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.ETAG, etag)
                    .lastModified(lastModified.toMillis())
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600, must-revalidate")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error handling range request", e);
            // Fallback to full file
            try {
                Resource resource = new UrlResource(path.toUri());
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .contentLength(fileSize)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            } catch (MalformedURLException ex) {
                logger.error("Failed to create resource from path", ex);
                return ResponseEntity.badRequest().build();
            }
        }
    }
    
    private String generateETag(Path path, FileTime lastModified, long fileSize) {
        // Generate ETag from file path, size, and last modified time
        String etagValue = path.toString() + "_" + fileSize + "_" + lastModified.toMillis();
        return "\"" + Integer.toHexString(etagValue.hashCode()) + "\"";
    }
    
    // Inner class for partial file resource
    private static class PartialFileResource extends org.springframework.core.io.AbstractResource {
        private final Path path;
        private final long start;
        private final long end;
        
        public PartialFileResource(Path path, long start, long end) {
            this.path = path;
            this.start = start;
            this.end = end;
        }
        
        @Override
        public String getDescription() {
            return "Partial file resource [" + path + "]";
        }
        
        @Override
        public java.io.InputStream getInputStream() throws IOException {
            RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r");
            raf.seek(start);
            return new java.io.InputStream() {
                private long position = start;
                
                @Override
                public int read() throws IOException {
                    if (position > end) {
                        raf.close();
                        return -1;
                    }
                    int b = raf.read();
                    if (b != -1) {
                        position++;
                    } else {
                        raf.close();
                    }
                    return b;
                }
                
                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    if (position > end) {
                        raf.close();
                        return -1;
                    }
                    long maxRead = Math.min(len, end - position + 1);
                    int bytesRead = raf.read(b, off, (int) maxRead);
                    if (bytesRead > 0) {
                        position += bytesRead;
                    }
                    if (position > end) {
                        raf.close();
                    }
                    return bytesRead;
                }
                
                @Override
                public void close() throws IOException {
                    raf.close();
                }
            };
        }
        
        @Override
        public long contentLength() throws IOException {
            return end - start + 1;
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
        } else if (lowerPath.endsWith(".doc") || lowerPath.endsWith(".docx")) {
            return "application/msword";
        } else if (lowerPath.endsWith(".xls") || lowerPath.endsWith(".xlsx")) {
            return "application/vnd.ms-excel";
        }
        return "application/octet-stream";
    }
}

