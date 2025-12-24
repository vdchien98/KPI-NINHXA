package vn.gov.bacninh.ninhxareport.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.gov.bacninh.ninhxareport.dto.*;
import vn.gov.bacninh.ninhxareport.entity.*;
import vn.gov.bacninh.ninhxareport.repository.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ReportRequestService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportRequestService.class);
    
    @Autowired
    private ReportRequestRepository reportRequestRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private ReportRequestHistoryRepository reportRequestHistoryRepository;
    
    @Autowired
    private ReportResponseRepository reportResponseRepository;
    
    @Autowired
    private ReportResponseHistoryRepository reportResponseHistoryRepository;
    
    @Autowired
    private ReportResponseCommentRepository reportResponseCommentRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ReportRequestAttachmentRepository reportRequestAttachmentRepository;
    
    public List<ReportRequestDTO> getAllReportRequests() {
        return reportRequestRepository.findAllOrderByCreatedAtDesc().stream()
                .map(request -> enrichWithStatistics(ReportRequestDTO.fromEntity(request), request.getId()))
                .collect(Collectors.toList());
    }
    
    public List<ReportRequestDTO> getMyReportRequests(Long userId) {
        return reportRequestRepository.findByCreatedByIdOrderByCreatedAtDesc(userId).stream()
                .map(request -> enrichWithStatistics(ReportRequestDTO.fromEntity(request), request.getId()))
                .collect(Collectors.toList());
    }
    
    public List<ReportRequestDTO> getReceivedRequests(Long userId) {
        return reportRequestRepository.findReceivedByUserId(userId).stream()
                .map(request -> {
                    ReportRequestDTO dto = enrichWithStatistics(ReportRequestDTO.fromEntity(request), request.getId());
                    // Thêm status của response của user hiện tại
                    enrichWithMyResponseStatus(dto, request.getId(), userId);
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    public ReportRequestDTO getById(Long id) {
        ReportRequest request = reportRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu báo cáo với id: " + id));
        // Load attachments
        request.setAttachments(reportRequestAttachmentRepository.findByReportRequestId(id));
        return enrichWithStatistics(ReportRequestDTO.fromEntity(request), id);
    }
    
    @Transactional
    public ReportRequestDTO createReportRequest(CreateReportRequestDTO dto, Long createdByUserId, MultipartFile[] files) throws IOException {
        User createdBy = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        ReportRequest request = ReportRequest.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .createdBy(createdBy)
                .deadline(dto.getDeadline())
                .status(ReportRequest.ReportRequestStatus.PENDING)
                .build();
        
        // Add target organizations
        if (dto.getOrganizationIds() != null && !dto.getOrganizationIds().isEmpty()) {
            Set<Organization> orgs = new HashSet<>();
            for (Long orgId : dto.getOrganizationIds()) {
                Organization org = organizationRepository.findById(orgId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ quan với id: " + orgId));
                orgs.add(org);
            }
            request.setTargetOrganizations(orgs);
        }
        
        // Add target departments
        if (dto.getDepartmentIds() != null && !dto.getDepartmentIds().isEmpty()) {
            Set<Department> depts = new HashSet<>();
            for (Long deptId : dto.getDepartmentIds()) {
                Department dept = departmentRepository.findById(deptId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng ban với id: " + deptId));
                depts.add(dept);
            }
            request.setTargetDepartments(depts);
        }
        
        // Add target users
        if (dto.getUserIds() != null && !dto.getUserIds().isEmpty()) {
            Set<User> users = new HashSet<>();
            for (Long userId : dto.getUserIds()) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với id: " + userId));
                users.add(user);
            }
            request.setTargetUsers(users);
        }
        
        request = reportRequestRepository.save(request);
        
        // Handle file uploads
        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    String filePath = fileStorageService.storeFile(file, "report-requests/" + request.getId());
                    
                    ReportRequestAttachment attachment = ReportRequestAttachment.builder()
                            .reportRequest(request)
                            .fileName(file.getOriginalFilename())
                            .filePath(filePath)
                            .fileType(file.getContentType())
                            .fileSize(file.getSize())
                            .build();
                    
                    reportRequestAttachmentRepository.save(attachment);
                }
            }
        }
        
        // Save history snapshot for initial creation
        saveHistorySnapshot(request, createdBy);
        
        // Reload request with attachments
        request = reportRequestRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu sau khi tạo"));
        
        return ReportRequestDTO.fromEntity(request);
    }
    
    @Transactional
    public ReportRequestDTO forwardReportRequest(Long originalRequestId, ForwardReportRequestDTO dto, Long forwardedByUserId) {
        // Lấy yêu cầu gốc
        ReportRequest originalRequest = reportRequestRepository.findById(originalRequestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu báo cáo với id: " + originalRequestId));
        
        User forwardedBy = userRepository.findById(forwardedByUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        // Tạo yêu cầu mới dựa trên yêu cầu gốc
        String forwardTitle = dto.getTitle();
        String forwardDescription = originalRequest.getDescription() != null ? originalRequest.getDescription() : "";
        
        // Thêm ghi chú chuyển tiếp vào description nếu có
        if (dto.getForwardNote() != null && !dto.getForwardNote().trim().isEmpty()) {
            forwardDescription = (forwardDescription.isEmpty() ? "" : forwardDescription + "\n\n") + 
                    "--- Ghi chú chuyển tiếp ---\n" + dto.getForwardNote();
        }
        
        ReportRequest forwardedRequest = ReportRequest.builder()
                .title(forwardTitle)
                .description(forwardDescription)
                .createdBy(forwardedBy)
                .deadline(dto.getDeadline())
                .status(ReportRequest.ReportRequestStatus.PENDING)
                .build();
        
        // Add target organizations
        if (dto.getOrganizationIds() != null && !dto.getOrganizationIds().isEmpty()) {
            Set<Organization> orgs = new HashSet<>();
            for (Long orgId : dto.getOrganizationIds()) {
                Organization org = organizationRepository.findById(orgId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ quan với id: " + orgId));
                orgs.add(org);
            }
            forwardedRequest.setTargetOrganizations(orgs);
        }
        
        // Add target departments
        if (dto.getDepartmentIds() != null && !dto.getDepartmentIds().isEmpty()) {
            Set<Department> depts = new HashSet<>();
            for (Long deptId : dto.getDepartmentIds()) {
                Department dept = departmentRepository.findById(deptId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng ban với id: " + deptId));
                depts.add(dept);
            }
            forwardedRequest.setTargetDepartments(depts);
        }
        
        // Add target users
        if (dto.getUserIds() != null && !dto.getUserIds().isEmpty()) {
            Set<User> users = new HashSet<>();
            for (Long userId : dto.getUserIds()) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với id: " + userId));
                users.add(user);
            }
            forwardedRequest.setTargetUsers(users);
        }
        
        forwardedRequest = reportRequestRepository.save(forwardedRequest);
        
        // Save history snapshot for forwarded request
        saveHistorySnapshot(forwardedRequest, forwardedBy);
        
        return enrichWithStatistics(ReportRequestDTO.fromEntity(forwardedRequest), forwardedRequest.getId());
    }
    
    @Transactional
    public ReportRequestDTO updateReportRequest(Long id, CreateReportRequestDTO dto, Long userId, 
                                                 MultipartFile[] files, List<Long> deletedAttachmentIds) {
        ReportRequest request = reportRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu báo cáo với id: " + id));
        
        User editor = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        request.setTitle(dto.getTitle());
        request.setDescription(dto.getDescription());
        request.setDeadline(dto.getDeadline());
        
        // Update target organizations
        if (dto.getOrganizationIds() != null) {
            Set<Organization> orgs = new HashSet<>();
            for (Long orgId : dto.getOrganizationIds()) {
                Organization org = organizationRepository.findById(orgId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ quan với id: " + orgId));
                orgs.add(org);
            }
            request.setTargetOrganizations(orgs);
        }
        
        // Update target departments
        if (dto.getDepartmentIds() != null) {
            Set<Department> depts = new HashSet<>();
            for (Long deptId : dto.getDepartmentIds()) {
                Department dept = departmentRepository.findById(deptId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng ban với id: " + deptId));
                depts.add(dept);
            }
            request.setTargetDepartments(depts);
        }
        
        // Update target users
        if (dto.getUserIds() != null) {
            Set<User> users = new HashSet<>();
            for (Long userIdItem : dto.getUserIds()) {
                User user = userRepository.findById(userIdItem)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với id: " + userIdItem));
                users.add(user);
            }
            request.setTargetUsers(users);
        }
        
        request = reportRequestRepository.save(request);
        
        // Handle deleted attachments
        if (deletedAttachmentIds != null && !deletedAttachmentIds.isEmpty()) {
            for (Long attachmentId : deletedAttachmentIds) {
                reportRequestAttachmentRepository.findById(attachmentId).ifPresent(attachment -> {
                    // Delete file from storage
                    try {
                        fileStorageService.deleteFile(attachment.getFilePath());
                    } catch (Exception e) {
                        logger.warn("Failed to delete file: {}", attachment.getFilePath(), e);
                    }
                    // Delete attachment record
                    reportRequestAttachmentRepository.delete(attachment);
                });
            }
        }
        
        // Handle new file uploads
        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    try {
                        String filePath = fileStorageService.storeFile(file, "report-requests/" + request.getId());
                        
                        ReportRequestAttachment attachment = ReportRequestAttachment.builder()
                                .reportRequest(request)
                                .fileName(file.getOriginalFilename())
                                .filePath(filePath)
                                .fileType(file.getContentType())
                                .fileSize(file.getSize())
                                .build();
                        reportRequestAttachmentRepository.save(attachment);
                    } catch (IOException e) {
                        logger.error("Failed to save file: {}", file.getOriginalFilename(), e);
                        throw new RuntimeException("Không thể lưu file: " + file.getOriginalFilename());
                    }
                }
            }
        }
        
        // Save history snapshot
        saveHistorySnapshot(request, editor);
        
        // Return with attachments
        return enrichWithStatistics(ReportRequestDTO.fromEntity(request), request.getId());
    }
    
    private void saveHistorySnapshot(ReportRequest request, User editor) {
        try {
            // Get the last history record to compare
            List<ReportRequestHistory> existingHistories = reportRequestHistoryRepository
                    .findByReportRequestIdOrderByEditedAtDesc(request.getId());
            
            // Create current snapshot
            String orgsJson = objectMapper.writeValueAsString(
                    request.getTargetOrganizations().stream()
                            .map(org -> new SimpleIdName(org.getId(), org.getName()))
                            .collect(Collectors.toList())
            );
            String deptsJson = objectMapper.writeValueAsString(
                    request.getTargetDepartments().stream()
                            .map(dept -> new SimpleIdName(dept.getId(), dept.getName()))
                            .collect(Collectors.toList())
            );
            String usersJson = objectMapper.writeValueAsString(
                    request.getTargetUsers().stream()
                            .map(user -> new SimpleIdName(user.getId(), user.getFullName()))
                            .collect(Collectors.toList())
            );
            
            String currentTitle = request.getTitle() != null ? request.getTitle() : "";
            String currentDescription = request.getDescription() != null ? request.getDescription() : "";
            
            // Check if there's a recent history (within last 10 seconds) with same content
            if (!existingHistories.isEmpty()) {
                ReportRequestHistory lastHistory = existingHistories.get(0);
                String lastTitle = lastHistory.getTitleSnapshot() != null ? lastHistory.getTitleSnapshot() : "";
                String lastDescription = lastHistory.getDescriptionSnapshot() != null ? lastHistory.getDescriptionSnapshot() : "";
                String lastOrgsJson = lastHistory.getTargetOrganizationsSnapshotJson() != null ? lastHistory.getTargetOrganizationsSnapshotJson() : "[]";
                String lastDeptsJson = lastHistory.getTargetDepartmentsSnapshotJson() != null ? lastHistory.getTargetDepartmentsSnapshotJson() : "[]";
                String lastUsersJson = lastHistory.getTargetUsersSnapshotJson() != null ? lastHistory.getTargetUsersSnapshotJson() : "[]";
                
                // Compare content - only save if there's actual change
                if (currentTitle.equals(lastTitle) && 
                    currentDescription.equals(lastDescription) &&
                    orgsJson.equals(lastOrgsJson) &&
                    deptsJson.equals(lastDeptsJson) &&
                    usersJson.equals(lastUsersJson) &&
                    request.getDeadline().equals(lastHistory.getDeadlineSnapshot())) {
                    // No change detected, skip saving
                    return;
                }
                
                // Check if last history was created very recently (within 10 seconds)
                java.time.Duration timeDiff = java.time.Duration.between(
                    lastHistory.getEditedAt(), 
                    java.time.LocalDateTime.now()
                );
                if (timeDiff.getSeconds() < 10 && 
                    currentTitle.equals(lastTitle) && 
                    currentDescription.equals(lastDescription) &&
                    orgsJson.equals(lastOrgsJson) &&
                    deptsJson.equals(lastDeptsJson) &&
                    usersJson.equals(lastUsersJson) &&
                    request.getDeadline().equals(lastHistory.getDeadlineSnapshot())) {
                    // Very recent and identical, skip to avoid duplicates
                    return;
                }
            }
            
            // Calculate version number
            long count = reportRequestHistoryRepository.countByReportRequestId(request.getId());
            
            ReportRequestHistory history = ReportRequestHistory.builder()
                    .reportRequest(request)
                    .editedBy(editor)
                    .versionNumber((int) count + 1)
                    .titleSnapshot(currentTitle)
                    .descriptionSnapshot(currentDescription)
                    .deadlineSnapshot(request.getDeadline())
                    .targetOrganizationsSnapshotJson(orgsJson)
                    .targetDepartmentsSnapshotJson(deptsJson)
                    .targetUsersSnapshotJson(usersJson)
                    .build();
            
            reportRequestHistoryRepository.save(history);
        } catch (JsonProcessingException e) {
            // do not break main flow
            e.printStackTrace();
        }
    }
    
    private record SimpleIdName(Long id, String name) {}
    
    public List<ReportRequestHistoryDTO> getHistory(Long requestId) {
        List<ReportRequestHistory> histories = reportRequestHistoryRepository
                .findByReportRequestIdOrderByEditedAtDesc(requestId);
        
        return histories.stream().map(this::toHistoryDTO).collect(Collectors.toList());
    }
    
    private ReportRequestHistoryDTO toHistoryDTO(ReportRequestHistory history) {
        List<OrganizationDTO> orgs = new ArrayList<>();
        List<DepartmentDTO> depts = new ArrayList<>();
        List<UserDTO> users = new ArrayList<>();
        
        try {
            if (history.getTargetOrganizationsSnapshotJson() != null) {
                List<SimpleIdName> orgList = objectMapper.readValue(
                        history.getTargetOrganizationsSnapshotJson(),
                        new TypeReference<List<SimpleIdName>>() {}
                );
                orgs = orgList.stream()
                        .map(item -> {
                            Organization org = organizationRepository.findById(item.id()).orElse(null);
                            return org != null ? OrganizationDTO.fromEntity(org) : null;
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());
            }
            
            if (history.getTargetDepartmentsSnapshotJson() != null) {
                List<SimpleIdName> deptList = objectMapper.readValue(
                        history.getTargetDepartmentsSnapshotJson(),
                        new TypeReference<List<SimpleIdName>>() {}
                );
                depts = deptList.stream()
                        .map(item -> {
                            Department dept = departmentRepository.findById(item.id()).orElse(null);
                            return dept != null ? DepartmentDTO.fromEntity(dept) : null;
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());
            }
            
            if (history.getTargetUsersSnapshotJson() != null) {
                List<SimpleIdName> userList = objectMapper.readValue(
                        history.getTargetUsersSnapshotJson(),
                        new TypeReference<List<SimpleIdName>>() {}
                );
                users = userList.stream()
                        .map(item -> {
                            User user = userRepository.findById(item.id()).orElse(null);
                            return user != null ? UserDTO.fromEntity(user) : null;
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return ReportRequestHistoryDTO.builder()
                .id(history.getId())
                .version(history.getVersionNumber())
                .editedAt(history.getEditedAt())
                .editedBy(history.getEditedBy() != null ? UserDTO.fromEntity(history.getEditedBy()) : null)
                .title(history.getTitleSnapshot())
                .description(history.getDescriptionSnapshot())
                .deadline(history.getDeadlineSnapshot())
                .targetOrganizations(orgs)
                .targetDepartments(depts)
                .targetUsers(users)
                .build();
    }
    
    private ReportRequestDTO enrichWithStatistics(ReportRequestDTO dto, Long requestId) {
        // Lấy tất cả responses cho request này
        List<ReportResponse> responses = reportResponseRepository.findByReportRequestIdOrderByCreatedAtDesc(requestId);
        
        // Tính toán thống kê
        Set<Long> allTargetUserIds = new HashSet<>();
        if (dto.getTargetUsers() != null) {
            allTargetUserIds.addAll(dto.getTargetUsers().stream()
                    .map(UserDTO::getId)
                    .collect(Collectors.toSet()));
        }
        
        // Lấy danh sách userId đã có response
        Set<Long> respondedUserIds = responses.stream()
                .map(r -> r.getSubmittedBy().getId())
                .collect(Collectors.toSet());
        
        // Lấy danh sách userId đã hoàn thành (có score)
        Set<Long> completedUserIds = responses.stream()
                .filter(r -> r.getScore() != null)
                .map(r -> r.getSubmittedBy().getId())
                .collect(Collectors.toSet());
        
        // Lấy danh sách userId đã nộp nhưng chưa được đánh giá
        Set<Long> submittedUserIds = respondedUserIds.stream()
                .filter(userId -> !completedUserIds.contains(userId))
                .collect(Collectors.toSet());
        
        // Tính số lượng
        int completedCount = completedUserIds.size();
        int submittedCount = submittedUserIds.size();
        int pendingCount = (int) allTargetUserIds.stream()
                .filter(userId -> !respondedUserIds.contains(userId))
                .count();
        
        dto.setCompletedCount(completedCount);
        dto.setSubmittedCount(submittedCount);
        dto.setPendingCount(pendingCount);
        dto.setCompletedUserIds(completedUserIds);
        
        return dto;
    }
    
    private void enrichWithMyResponseStatus(ReportRequestDTO dto, Long requestId, Long userId) {
        // Tìm response của user hiện tại cho request này
        Optional<ReportResponse> myResponse = reportResponseRepository.findByReportRequestIdAndSubmittedById(requestId, userId);
        
        if (myResponse.isPresent()) {
            ReportResponse response = myResponse.get();
            // Nếu có score thì đã hoàn thành
            if (response.getScore() != null) {
                dto.setMyResponseStatus("COMPLETED");
            } else {
                // Có response nhưng chưa có score = đã nộp, chờ đánh giá
                dto.setMyResponseStatus("SUBMITTED");
            }
        } else {
            // Chưa có response = đang chờ
            dto.setMyResponseStatus("PENDING");
        }
    }
    
    @Transactional
    public void deleteReportRequest(Long id, Long userId) {
        ReportRequest request = reportRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu báo cáo với id: " + id));
        
        // Kiểm tra quyền: chỉ người tạo mới được xóa
        if (!request.getCreatedBy().getId().equals(userId)) {
            throw new RuntimeException("Chỉ người tạo yêu cầu mới có quyền xóa");
        }
        
        // 1. Xóa tất cả ReportResponse và dữ liệu liên quan
        List<ReportResponse> responses = reportResponseRepository.findByReportRequestIdOrderByCreatedAtDesc(id);
        for (ReportResponse response : responses) {
            // Xóa files từ các items
            if (response.getItems() != null) {
                for (var item : response.getItems()) {
                    if (item.getFilePath() != null && !item.getFilePath().isEmpty()) {
                        try {
                            fileStorageService.deleteFile(item.getFilePath());
                        } catch (Exception e) {
                            // Log error but continue
                            e.printStackTrace();
                        }
                    }
                }
            }
            
            // Xóa ReportResponseHistory
            List<ReportResponseHistory> responseHistories = reportResponseHistoryRepository
                    .findByReportResponseIdOrderByEditedAtDesc(response.getId());
            reportResponseHistoryRepository.deleteAll(responseHistories);
            
            // Xóa ReportResponseComment
            List<ReportResponseComment> responseComments = reportResponseCommentRepository
                    .findByReportResponseIdOrderByCommentedAtDesc(response.getId());
            reportResponseCommentRepository.deleteAll(responseComments);
            
            // Xóa ReportResponse (cascade sẽ xóa ReportResponseItem)
            reportResponseRepository.delete(response);
        }
        
        // 2. Xóa tất cả ReportRequestHistory
        List<ReportRequestHistory> requestHistories = reportRequestHistoryRepository
                .findByReportRequestIdOrderByEditedAtDesc(id);
        reportRequestHistoryRepository.deleteAll(requestHistories);
        
        // 3. Xóa ReportRequest
        reportRequestRepository.delete(request);
    }
    
    @Transactional
    public ReportRequestDTO updateStatus(Long id, String status) {
        ReportRequest request = reportRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu báo cáo với id: " + id));
        
        request.setStatus(ReportRequest.ReportRequestStatus.valueOf(status));
        request = reportRequestRepository.save(request);
        return ReportRequestDTO.fromEntity(request);
    }
}

