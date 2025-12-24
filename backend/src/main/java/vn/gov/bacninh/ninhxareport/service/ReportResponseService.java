package vn.gov.bacninh.ninhxareport.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.gov.bacninh.ninhxareport.dto.*;
import vn.gov.bacninh.ninhxareport.entity.*;
import vn.gov.bacninh.ninhxareport.repository.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ReportResponseService {
    
    @Autowired
    private ReportResponseRepository reportResponseRepository;
    
    @Autowired
    private ReportResponseItemRepository reportResponseItemRepository;
    
    @Autowired
    private ReportRequestRepository reportRequestRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private ReportResponseHistoryRepository reportResponseHistoryRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ReportResponseCommentRepository reportResponseCommentRepository;
    
    public List<ReportResponseDTO> getByReportRequestId(Long requestId) {
        return reportResponseRepository.findByReportRequestIdWithItems(requestId).stream()
                .map(ReportResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<ReportResponseDTO> getMyResponses(Long userId) {
        return reportResponseRepository.findBySubmittedByIdOrderByCreatedAtDesc(userId).stream()
                .map(ReportResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    public ReportResponseDTO getById(Long id) {
        ReportResponse response = reportResponseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo với id: " + id));
        return ReportResponseDTO.fromEntity(response);
    }
    
    public ReportResponseDTO getByRequestAndUser(Long requestId, Long userId) {
        return reportResponseRepository.findByReportRequestIdAndSubmittedById(requestId, userId)
                .map(ReportResponseDTO::fromEntity)
                .orElse(null);
    }
    
    @Transactional
    public ReportResponseDTO createResponse(CreateReportResponseDTO dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        ReportRequest request = reportRequestRepository.findById(dto.getReportRequestId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu báo cáo"));
        
        // Check if deadline has passed
        if (request.getDeadline().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Yêu cầu báo cáo đã quá hạn, không thể nộp báo cáo");
        }
        
        // Check if user already submitted
        if (reportResponseRepository.existsByReportRequestIdAndSubmittedById(dto.getReportRequestId(), userId)) {
            throw new RuntimeException("Bạn đã nộp báo cáo cho yêu cầu này rồi");
        }
        
        ReportResponse response = ReportResponse.builder()
                .reportRequest(request)
                .submittedBy(user)
                .note(dto.getNote())
                .build();
        
        // Add items
        if (dto.getItems() != null) {
            for (int i = 0; i < dto.getItems().size(); i++) {
                CreateReportResponseItemDTO itemDto = dto.getItems().get(i);
                ReportResponseItem item = ReportResponseItem.builder()
                        .title(itemDto.getTitle())
                        .content(itemDto.getContent())
                        .progress(itemDto.getProgress() != null ? itemDto.getProgress() : 0)
                        .difficulties(itemDto.getDifficulties())
                        .displayOrder(itemDto.getDisplayOrder() != null ? itemDto.getDisplayOrder() : i)
                        .build();
                response.addItem(item);
            }
        }
        
        response = reportResponseRepository.save(response);
        
        // Update request status to SUBMITTED when report is submitted
        request.setStatus(ReportRequest.ReportRequestStatus.SUBMITTED);
        reportRequestRepository.save(request);
        
        return ReportResponseDTO.fromEntity(response);
    }
    
    @Transactional
    public ReportResponseDTO updateResponse(Long id, CreateReportResponseDTO dto, Long userId) {
        ReportResponse response = reportResponseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo với id: " + id));
        
        // Check ownership
        if (!response.getSubmittedBy().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền sửa báo cáo này");
        }
        
        // Check if deadline has passed
        ReportRequest request = response.getReportRequest();
        if (request.getDeadline().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Yêu cầu báo cáo đã quá hạn, không thể chỉnh sửa báo cáo");
        }
        
        // Check if report has been evaluated
        if (response.getScore() != null) {
            throw new RuntimeException("Báo cáo đã được đánh giá, không thể chỉnh sửa");
        }
        
        response.setNote(dto.getNote());
        
        // Update items: keep existing files, only update content and order
        // Map old items by their order/index to preserve files
        List<ReportResponseItem> oldItems = new ArrayList<>(response.getItems());
        oldItems.sort((a, b) -> {
            int orderA = a.getDisplayOrder() != null ? a.getDisplayOrder() : 0;
            int orderB = b.getDisplayOrder() != null ? b.getDisplayOrder() : 0;
            return Integer.compare(orderA, orderB);
        });
        
        // Clear old items
        response.getItems().clear();
        
        // Add new items, preserving files from old items if they exist
        if (dto.getItems() != null) {
            for (int i = 0; i < dto.getItems().size(); i++) {
                CreateReportResponseItemDTO itemDto = dto.getItems().get(i);
                
                // Try to find corresponding old item to preserve file
                ReportResponseItem oldItem = null;
                if (i < oldItems.size()) {
                    oldItem = oldItems.get(i);
                }
                
                ReportResponseItem item = ReportResponseItem.builder()
                        .title(itemDto.getTitle())
                        .content(itemDto.getContent())
                        .progress(itemDto.getProgress() != null ? itemDto.getProgress() : 0)
                        .difficulties(itemDto.getDifficulties())
                        .displayOrder(itemDto.getDisplayOrder() != null ? itemDto.getDisplayOrder() : i)
                        .build();
                
                // Preserve file from old item if exists
                if (oldItem != null && oldItem.getFilePath() != null) {
                    item.setFileName(oldItem.getFileName());
                    item.setFilePath(oldItem.getFilePath());
                    item.setFileType(oldItem.getFileType());
                    item.setFileSize(oldItem.getFileSize());
                }
                
                response.addItem(item);
            }
        }
        
        response = reportResponseRepository.save(response);
        
        saveHistorySnapshot(response, response.getSubmittedBy());
        
        return ReportResponseDTO.fromEntity(response);
    }
    
    @Transactional
    public ReportResponseItemDTO addItemWithFile(Long responseId, String content, MultipartFile file, Long userId) throws IOException {
        ReportResponse response = reportResponseRepository.findById(responseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo"));
        
        // Check ownership
        if (!response.getSubmittedBy().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền thêm vào báo cáo này");
        }
        
        // Check if report has been evaluated
        if (response.getScore() != null) {
            throw new RuntimeException("Báo cáo đã được đánh giá, không thể chỉnh sửa");
        }
        
        ReportResponseItem item = ReportResponseItem.builder()
                .content(content)
                .displayOrder(response.getItems().size())
                .build();
        
        // Handle file upload
        if (file != null && !file.isEmpty()) {
            if (!fileStorageService.isValidFileType(file)) {
                throw new RuntimeException("Chỉ chấp nhận file ảnh hoặc PDF");
            }
            
            String filePath = fileStorageService.storeFile(file, "reports/" + responseId);
            item.setFileName(file.getOriginalFilename());
            item.setFilePath(filePath);
            item.setFileType(file.getContentType());
            item.setFileSize(file.getSize());
        }
        
        response.addItem(item);
        reportResponseRepository.save(response);
        
        saveHistorySnapshot(response, response.getSubmittedBy());
        
        return ReportResponseItemDTO.fromEntity(item);
    }
    
    @Transactional
    public ReportResponseItemDTO updateItemFile(Long itemId, MultipartFile file, Long userId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File không hợp lệ");
        }
        
        ReportResponseItem item = reportResponseItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mục báo cáo"));
        
        ReportResponse response = item.getReportResponse();
        
        // Check ownership
        if (!response.getSubmittedBy().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền sửa mục này");
        }
        
        // Check if deadline has passed
        ReportRequest request = response.getReportRequest();
        if (request.getDeadline().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Yêu cầu báo cáo đã quá hạn, không thể upload file");
        }
        
        // Check if report has been evaluated
        if (response.getScore() != null) {
            throw new RuntimeException("Báo cáo đã được đánh giá, không thể chỉnh sửa");
        }
        
        if (!fileStorageService.isValidFileType(file)) {
            throw new RuntimeException("Chỉ chấp nhận file ảnh hoặc PDF");
        }
        
        // Store old file info to check if it's actually a change
        String oldFilePath = item.getFilePath();
        String oldFileName = item.getFileName();
        
        // Delete old file if exists
        if (oldFilePath != null) {
            fileStorageService.deleteFile(oldFilePath);
        }
        
        String newFilePath = fileStorageService.storeFile(file, "reports/" + response.getId());
        item.setFileName(file.getOriginalFilename());
        item.setFilePath(newFilePath);
        item.setFileType(file.getContentType());
        item.setFileSize(file.getSize());
        
        reportResponseItemRepository.save(item);
        
        // Reload response to get updated state
        response = reportResponseRepository.findById(response.getId()).orElse(response);
        
        // Only save history if file actually changed (different path or name)
        // This prevents duplicate history when file is uploaded right after form update
        boolean fileChanged = !java.util.Objects.equals(oldFilePath, newFilePath) || 
                             !java.util.Objects.equals(oldFileName, file.getOriginalFilename());
        
        if (fileChanged) {
            // Pass flag to indicate this is a file-only update
            // The saveHistorySnapshot will check if we should update last history or create new
            saveHistorySnapshot(response, response.getSubmittedBy(), true);
        }
        
        return ReportResponseItemDTO.fromEntity(item);
    }
    
    @Transactional
    public void deleteItem(Long itemId, Long userId) {
        ReportResponseItem item = reportResponseItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mục báo cáo"));
        
        // Check ownership
        if (!item.getReportResponse().getSubmittedBy().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa mục này");
        }
        
        // Check if report has been evaluated
        if (item.getReportResponse().getScore() != null) {
            throw new RuntimeException("Báo cáo đã được đánh giá, không thể chỉnh sửa");
        }
        
        // Delete file if exists
        if (item.getFilePath() != null) {
            fileStorageService.deleteFile(item.getFilePath());
        }
        
        reportResponseItemRepository.delete(item);
    }
    
    @Transactional
    public void deleteResponse(Long id, Long userId) {
        ReportResponse response = reportResponseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo"));
        
        // Check ownership
        if (!response.getSubmittedBy().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa báo cáo này");
        }
        
        // Delete all files
        for (ReportResponseItem item : response.getItems()) {
            if (item.getFilePath() != null) {
                fileStorageService.deleteFile(item.getFilePath());
            }
        }
        
        reportResponseRepository.delete(response);
    }
    
    public List<ReportResponseHistoryDTO> getHistory(Long responseId, Long requesterId) {
        ReportResponse response = reportResponseRepository.findById(responseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo với id: " + responseId));
        
        boolean isOwner = response.getSubmittedBy() != null && response.getSubmittedBy().getId().equals(requesterId);
        boolean isCreator = response.getReportRequest() != null
                && response.getReportRequest().getCreatedBy() != null
                && response.getReportRequest().getCreatedBy().getId().equals(requesterId);
        
        if (!isOwner && !isCreator) {
            throw new RuntimeException("Bạn không có quyền xem lịch sử báo cáo này");
        }
        
        return reportResponseHistoryRepository.findByReportResponseIdOrderByEditedAtDesc(responseId)
                .stream()
                .map(this::toHistoryDTO)
                .collect(Collectors.toList());
    }
    
    private void saveHistorySnapshot(ReportResponse response, User editor) {
        saveHistorySnapshot(response, editor, false);
    }
    
    private void saveHistorySnapshot(ReportResponse response, User editor, boolean isFileOnlyUpdate) {
        try {
            // Get the last history record to compare
            List<ReportResponseHistory> existingHistories = reportResponseHistoryRepository
                    .findByReportResponseIdOrderByEditedAtDesc(response.getId());
            
            // Create current snapshot
            List<SnapshotItem> snapshotItems = response.getItems().stream()
                    .sorted((a, b) -> Integer.compare(a.getDisplayOrder() != null ? a.getDisplayOrder() : 0,
                            b.getDisplayOrder() != null ? b.getDisplayOrder() : 0))
                    .map(item -> new SnapshotItem(
                            item.getTitle(),
                            item.getContent(),
                            item.getProgress(),
                            item.getDifficulties(),
                            item.getFileName(),
                            item.getFilePath(),
                            item.getFileType(),
                            item.getFileSize(),
                            item.getDisplayOrder()
                    ))
                    .collect(Collectors.toList());
            
            String currentJson = objectMapper.writeValueAsString(snapshotItems);
            String currentNote = response.getNote() != null ? response.getNote() : "";
            
            // Check if there's a recent history (within last 10 seconds) with same content
            // This prevents duplicate saves from rapid consecutive updates
            if (!existingHistories.isEmpty()) {
                ReportResponseHistory lastHistory = existingHistories.get(0);
                String lastJson = lastHistory.getItemsSnapshotJson() != null ? lastHistory.getItemsSnapshotJson() : "";
                String lastNote = lastHistory.getNoteSnapshot() != null ? lastHistory.getNoteSnapshot() : "";
                
                // Compare content - only save if there's actual change
                if (currentJson.equals(lastJson) && currentNote.equals(lastNote)) {
                    // No change detected, skip saving
                    return;
                }
                
                // Check if last history was created very recently (within 10 seconds)
                java.time.Duration timeDiff = java.time.Duration.between(
                    lastHistory.getEditedAt(), 
                    java.time.LocalDateTime.now()
                );
                
                if (timeDiff.getSeconds() < 10) {
                    // Very recent update - check if only file changed
                    if (isFileOnlyUpdate) {
                        // This is a file-only update right after a form update
                        // Compare content without file paths to see if only files differ
                        try {
                            List<SnapshotItem> lastItems = objectMapper.readValue(lastJson,
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, SnapshotItem.class));
                            List<SnapshotItem> currentItems = snapshotItems;
                            
                            // Compare all fields except filePath and fileName
                            boolean onlyFilesChanged = true;
                            if (lastItems.size() == currentItems.size()) {
                                for (int i = 0; i < lastItems.size(); i++) {
                                    SnapshotItem lastItem = lastItems.get(i);
                                    SnapshotItem currentItem = currentItems.get(i);
                                    
                                    if (!java.util.Objects.equals(lastItem.title(), currentItem.title()) ||
                                        !java.util.Objects.equals(lastItem.content(), currentItem.content()) ||
                                        !java.util.Objects.equals(lastItem.progress(), currentItem.progress()) ||
                                        !java.util.Objects.equals(lastItem.difficulties(), currentItem.difficulties()) ||
                                        !java.util.Objects.equals(lastItem.displayOrder(), currentItem.displayOrder())) {
                                        onlyFilesChanged = false;
                                        break;
                                    }
                                }
                            } else {
                                onlyFilesChanged = false;
                            }
                            
                            // If only files changed and note is same, update the last history instead of creating new
                            if (onlyFilesChanged && currentNote.equals(lastNote)) {
                                lastHistory.setItemsSnapshotJson(currentJson);
                                lastHistory.setEditedBy(editor);
                                lastHistory.setEditedAt(java.time.LocalDateTime.now());
                                reportResponseHistoryRepository.save(lastHistory);
                                return;
                            }
                        } catch (IOException e) {
                            // If parsing fails, continue with normal save
                        }
                    } else if (currentJson.equals(lastJson) && currentNote.equals(lastNote)) {
                        // Identical content, skip to avoid duplicates
                        return;
                    }
                }
            }
            
            // Calculate version number
            long count = reportResponseHistoryRepository.countByReportResponseId(response.getId());
            
            ReportResponseHistory history = ReportResponseHistory.builder()
                    .reportResponse(response)
                    .editedBy(editor)
                    .versionNumber((int) count + 1)
                    .noteSnapshot(currentNote)
                    .itemsSnapshotJson(currentJson)
                    .build();
            
            reportResponseHistoryRepository.save(history);
        } catch (JsonProcessingException e) {
            // do not break main flow
            e.printStackTrace();
        }
    }
    
    private ReportResponseHistoryDTO toHistoryDTO(ReportResponseHistory history) {
        List<SnapshotItem> snapshotItems = new ArrayList<>();
        try {
            if (history.getItemsSnapshotJson() != null) {
                snapshotItems = objectMapper.readValue(history.getItemsSnapshotJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, SnapshotItem.class));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        List<ReportResponseItemDTO> itemDTOs = snapshotItems.stream()
                .map(si -> ReportResponseItemDTO.builder()
                        .title(si.title())
                        .content(si.content())
                        .progress(si.progress())
                        .difficulties(si.difficulties())
                        .fileName(si.fileName())
                        .filePath(si.filePath())
                        .fileType(si.fileType())
                        .fileSize(si.fileSize())
                        .displayOrder(si.displayOrder())
                        .build())
                .collect(Collectors.toList());
        
        return ReportResponseHistoryDTO.builder()
                .id(history.getId())
                .version(history.getVersionNumber())
                .editedAt(history.getEditedAt())
                .editedBy(history.getEditedBy() != null ? UserDTO.fromEntity(history.getEditedBy()) : null)
                .note(history.getNoteSnapshot())
                .items(itemDTOs)
                .build();
    }
    
    private record SnapshotItem(
            String title,
            String content,
            Integer progress,
            String difficulties,
            String fileName,
            String filePath,
            String fileType,
            Long fileSize,
            Integer displayOrder
    ) {}
    
    @Transactional
    public ReportResponseDTO evaluateResponse(Long responseId, Double score, String comment, Long evaluatorId) {
        ReportResponse response = reportResponseRepository.findById(responseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo với id: " + responseId));
        
        User evaluator = userRepository.findById(evaluatorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người đánh giá"));
        
        // Check if evaluator is the creator of the request
        ReportRequest request = response.getReportRequest();
        if (!request.getCreatedBy().getId().equals(evaluatorId)) {
            throw new RuntimeException("Chỉ người tạo yêu cầu mới có quyền đánh giá báo cáo");
        }
        
        // Check if already completed
        if (response.getScore() != null && request.getStatus() == ReportRequest.ReportRequestStatus.COMPLETED) {
            throw new RuntimeException("Báo cáo đã được đánh giá và hoàn thành, không thể chỉnh sửa");
        }
        
        // Validate score (0-10)
        if (score == null || score < 0 || score > 10) {
            throw new RuntimeException("Điểm số phải từ 0 đến 10");
        }
        
        response.setScore(score);
        response.setComment(comment);
        response.setEvaluatedBy(evaluator);
        response.setEvaluatedAt(java.time.LocalDateTime.now());
        
        // Set request status to COMPLETED
        request.setStatus(ReportRequest.ReportRequestStatus.COMPLETED);
        reportRequestRepository.save(request);
        
        response = reportResponseRepository.save(response);
        
        // Save comment history
        if (comment != null && !comment.trim().isEmpty()) {
            vn.gov.bacninh.ninhxareport.entity.ReportResponseComment commentHistory = vn.gov.bacninh.ninhxareport.entity.ReportResponseComment.builder()
                    .reportResponse(response)
                    .commentedBy(evaluator)
                    .comment(comment)
                    .score(score)
                    .isFinalEvaluation(true)
                    .build();
            reportResponseCommentRepository.save(commentHistory);
        }
        
        return ReportResponseDTO.fromEntity(response);
    }
    
    @Transactional
    public ReportResponseDTO sendBackForRevision(Long responseId, String comment, Long evaluatorId) {
        ReportResponse response = reportResponseRepository.findById(responseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo với id: " + responseId));
        
        User evaluator = userRepository.findById(evaluatorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người đánh giá"));
        
        // Check if evaluator is the creator of the request
        ReportRequest request = response.getReportRequest();
        if (!request.getCreatedBy().getId().equals(evaluatorId)) {
            throw new RuntimeException("Chỉ người tạo yêu cầu mới có quyền gửi lại báo cáo");
        }
        
        // Check if already completed
        if (response.getScore() != null && request.getStatus() == ReportRequest.ReportRequestStatus.COMPLETED) {
            throw new RuntimeException("Báo cáo đã được đánh giá và hoàn thành, không thể chỉnh sửa");
        }
        
        // Save comment but don't set score or status
        response.setComment(comment);
        response.setEvaluatedBy(evaluator);
        response.setEvaluatedAt(java.time.LocalDateTime.now());
        
        // Reset score if it was set before
        response.setScore(null);
        
        // Set request status back to SUBMITTED or IN_PROGRESS
        if (request.getStatus() != ReportRequest.ReportRequestStatus.COMPLETED) {
            request.setStatus(ReportRequest.ReportRequestStatus.SUBMITTED);
            reportRequestRepository.save(request);
        }
        
        response = reportResponseRepository.save(response);
        
        // Save comment history
        if (comment != null && !comment.trim().isEmpty()) {
            vn.gov.bacninh.ninhxareport.entity.ReportResponseComment commentHistory = vn.gov.bacninh.ninhxareport.entity.ReportResponseComment.builder()
                    .reportResponse(response)
                    .commentedBy(evaluator)
                    .comment(comment)
                    .score(null)
                    .isFinalEvaluation(false)
                    .build();
            reportResponseCommentRepository.save(commentHistory);
        }
        
        return ReportResponseDTO.fromEntity(response);
    }
    
    @Transactional
    public ReportResponseDTO selfEvaluateResponse(Long responseId, Double score, Long submitterId) {
        ReportResponse response = reportResponseRepository.findById(responseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo với id: " + responseId));
        
        // Check if the submitter is the owner of the response
        if (!response.getSubmittedBy().getId().equals(submitterId)) {
            throw new RuntimeException("Chỉ người nộp báo cáo mới có quyền tự đánh giá");
        }
        
        // Validate score (0-10)
        if (score == null || score < 0 || score > 10) {
            throw new RuntimeException("Điểm số phải từ 0 đến 10");
        }
        
        response.setSelfScore(score);
        response.setSelfEvaluatedAt(java.time.LocalDateTime.now());
        
        response = reportResponseRepository.save(response);
        
        return ReportResponseDTO.fromEntity(response);
    }
    
    public List<ReportResponseCommentDTO> getCommentHistory(Long responseId) {
        List<vn.gov.bacninh.ninhxareport.entity.ReportResponseComment> comments = reportResponseCommentRepository
                .findByReportResponseIdOrderByCommentedAtDesc(responseId);
        return comments.stream()
                .map(ReportResponseCommentDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy thống kê báo cáo cho một user cụ thể
     * Logic cốt lõi: Duyệt từng REQUEST được giao cho user, kiểm tra trạng thái và response tương ứng
     */
    public ReportStatisticsDTO getReportStatisticsByUser(Long userId) {
        User user = userRepository.findByIdWithRelations(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với id: " + userId));
        
        // CỐT LÕI: Lấy tất cả REQUESTS được giao cho user này (qua targetUsers, targetDepartments, targetOrganizations)
        List<Long> orgIds = user.getOrganizations() != null ? 
                user.getOrganizations().stream().map(org -> org.getId()).collect(Collectors.toList()) : 
                new ArrayList<>();
        List<Long> deptIds = new ArrayList<>();
        if (user.getDepartment() != null) {
            deptIds.add(user.getDepartment().getId());
        }
        
        List<ReportRequest> assignedRequests = reportRequestRepository.findRequestsForUser(orgIds, deptIds, userId);
        
        // Lấy responses để tra cứu (chỉ để biết request nào đã nộp và đã đánh giá)
        List<ReportResponse> responses = reportResponseRepository.findBySubmittedByIdWithRelations(userId);
        
        return buildStatistics(assignedRequests, responses, user);
    }
    
    /**
     * Lấy thống kê báo cáo cho tất cả users (admin)
     */
    public ReportStatisticsDTO getAllReportStatistics() {
        // Lấy tất cả requests
        List<ReportRequest> allRequests = reportRequestRepository.findAllOrderByCreatedAtDesc();
        
        // Lấy tất cả responses đã nộp và đã đánh giá
        List<ReportResponse> responses = reportResponseRepository.findAllWithRelations();
        
        return buildStatisticsForAllUsers(allRequests, responses);
    }
    
    /**
     * Xây dựng DTO thống kê cho một user cụ thể
     * Logic cốt lõi: Duyệt từng REQUEST được giao, kiểm tra trạng thái và response tương ứng
     * Bao gồm cả các request quá hạn nhưng chưa nộp (không có response)
     */
    private ReportStatisticsDTO buildStatistics(List<ReportRequest> assignedRequests, 
                                                 List<ReportResponse> responses, 
                                                 User user) {
        List<ReportStatisticsDTO.ReportStatisticItemDTO> reportItems = new ArrayList<>();
        int stt = 1;
        int onTimeCount = 0;
        int overdueCount = 0;
        double totalScore = 0;
        int scoredCount = 0;
        
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        // Load items cho tất cả responses để tránh lazy loading và đảm bảo mỗi response chỉ có 1 lần
        if (!responses.isEmpty()) {
            List<Long> responseIds = responses.stream()
                    .map(ReportResponse::getId)
                    .collect(Collectors.toList());
            
            // Load tất cả items theo response IDs trong một query
            List<ReportResponseItem> allItems = reportResponseItemRepository.findByReportResponseIdInOrderByDisplayOrderAsc(responseIds);
            
            // Group items theo response ID
            java.util.Map<Long, List<ReportResponseItem>> itemsMap = allItems.stream()
                    .collect(Collectors.groupingBy(item -> item.getReportResponse().getId()));
            
            // Set items vào responses
            for (ReportResponse response : responses) {
                List<ReportResponseItem> items = itemsMap.getOrDefault(response.getId(), new ArrayList<>());
                response.getItems().clear();
                response.getItems().addAll(items);
            }
        }
        
        // Tạo map để tra cứu response theo requestId (chỉ để biết request nào đã nộp và đã đánh giá)
        java.util.Map<Long, ReportResponse> responseMap = responses.stream()
                .filter(r -> r.getScore() != null) // Chỉ lấy các response đã được đánh giá
                .collect(Collectors.toMap(
                    r -> r.getReportRequest().getId(),
                    r -> r,
                    (r1, r2) -> r1 // Nếu có nhiều response cho cùng request, lấy cái đầu tiên
                ));
        
        // Lấy danh sách organizations của user
        List<OrganizationDTO> organizations = user.getOrganizations() != null ?
                user.getOrganizations().stream()
                        .map(OrganizationDTO::fromEntity)
                        .collect(Collectors.toList()) : new ArrayList<>();
        
        // CỐT LÕI: Duyệt từng REQUEST được giao cho user
        for (ReportRequest request : assignedRequests) {
            ReportResponse response = responseMap.get(request.getId());
            
            if (response != null) {
                // Đã nộp và đã được đánh giá - hiển thị trong bảng
                User submittedBy = response.getSubmittedBy();
                
                // Xác định trạng thái (Đúng hạn / Quá hạn)
                String status;
                if (response.getSubmittedAt() != null && request.getDeadline() != null) {
                    if (response.getSubmittedAt().isBefore(request.getDeadline()) || 
                        response.getSubmittedAt().isEqual(request.getDeadline())) {
                        status = "Đúng hạn";
                        onTimeCount++;
                    } else {
                        status = "Quá hạn";
                        overdueCount++;
                    }
                } else {
                    status = "Chưa xác định";
                }
                
                // Tạo link tài liệu kiểm chứng từ response items
                String documentLink = buildDocumentLink(response);
                List<ReportStatisticsDTO.DocumentFileDTO> documentFiles = buildDocumentFiles(response);
                
                ReportStatisticsDTO.ReportStatisticItemDTO item = ReportStatisticsDTO.ReportStatisticItemDTO.builder()
                        .id(response.getId())
                        .stt(stt++)
                        .reportName(request.getTitle())
                        .reportAuthor(UserDTO.fromEntity(submittedBy))
                        .department(user.getDepartment() != null ? 
                                DepartmentDTO.fromEntity(user.getDepartment()) : null)
                        .organizations(organizations)
                        .score(response.getScore())
                        .reviewer(response.getEvaluatedBy() != null ? 
                                UserDTO.fromEntity(response.getEvaluatedBy()) : null)
                        .submissionDate(response.getSubmittedAt())
                        .status(status)
                        .documentLink(documentLink)
                        .documentFiles(documentFiles)
                        .reportRequestId(request.getId())
                        .build();
                
                reportItems.add(item);
                
                // Tính tổng điểm
                totalScore += response.getScore();
                scoredCount++;
            } else {
                // Chưa nộp - chỉ hiển thị nếu quá hạn
                if (request.getDeadline() != null && request.getDeadline().isBefore(now)) {
                    // Quá hạn nhưng chưa nộp - hiển thị trong bảng với trạng thái "Quá hạn"
                    overdueCount++;
                    
                    ReportStatisticsDTO.ReportStatisticItemDTO item = ReportStatisticsDTO.ReportStatisticItemDTO.builder()
                            .id(null) // Không có response ID
                            .stt(stt++)
                            .reportName(request.getTitle())
                            .reportAuthor(UserDTO.fromEntity(user))
                            .department(user.getDepartment() != null ? 
                                    DepartmentDTO.fromEntity(user.getDepartment()) : null)
                            .organizations(organizations)
                            .score(null) // Chưa có điểm vì chưa nộp
                            .reviewer(null) // Chưa có người chấm
                            .submissionDate(request.getDeadline()) // Dùng deadline làm ngày giao báo cáo
                            .status("Quá hạn")
                            .documentLink("-") // Chưa có tài liệu
                            .documentFiles(new ArrayList<>()) // Chưa có file
                            .reportRequestId(request.getId())
                            .build();
                    
                    reportItems.add(item);
                    
                    // Tính điểm trung bình: báo cáo quá hạn chưa nộp được tính là 0 điểm
                    // Không cần cộng vào totalScore vì đã là 0, nhưng cần tăng scoredCount để tính vào mẫu số
                    scoredCount++;
                }
                // Chưa đến hạn chưa nộp - không hiển thị
            }
        }
        
        // Tính điểm trung bình: bao gồm cả các báo cáo quá hạn chưa nộp (tính là 0 điểm)
        double averageScore = scoredCount > 0 ? totalScore / scoredCount : 0;
        
        // Xác định xếp loại
        String rating = calculateRating(averageScore);
        
        ReportStatisticsDTO.SummaryDTO summary = ReportStatisticsDTO.SummaryDTO.builder()
                .totalReports(reportItems.size())
                .onTimeReports(onTimeCount)
                .overdueReports(overdueCount)
                .averageScore(averageScore)
                .rating(rating)
                .build();
        
        return ReportStatisticsDTO.builder()
                .reports(reportItems)
                .summary(summary)
                .build();
    }
    
    /**
     * Xây dựng DTO thống kê cho tất cả users (admin)
     */
    private ReportStatisticsDTO buildStatisticsForAllUsers(List<ReportRequest> allRequests, 
                                                           List<ReportResponse> responses) {
        List<ReportStatisticsDTO.ReportStatisticItemDTO> reportItems = new ArrayList<>();
        int stt = 1;
        int onTimeCount = 0;
        int overdueCount = 0;
        double totalScore = 0;
        int scoredCount = 0;
        
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        // Load items cho tất cả responses để tránh lazy loading và đảm bảo mỗi response chỉ có 1 lần
        if (!responses.isEmpty()) {
            List<Long> responseIds = responses.stream()
                    .map(ReportResponse::getId)
                    .collect(Collectors.toList());
            
            // Load tất cả items theo response IDs trong một query
            List<ReportResponseItem> allItems = reportResponseItemRepository.findByReportResponseIdInOrderByDisplayOrderAsc(responseIds);
            
            // Group items theo response ID
            java.util.Map<Long, List<ReportResponseItem>> itemsMap = allItems.stream()
                    .collect(Collectors.groupingBy(item -> item.getReportResponse().getId()));
            
            // Set items vào responses
            for (ReportResponse response : responses) {
                List<ReportResponseItem> items = itemsMap.getOrDefault(response.getId(), new ArrayList<>());
                response.getItems().clear();
                response.getItems().addAll(items);
            }
        }
        
        // Tạo map để tra cứu response theo requestId và userId
        java.util.Map<String, ReportResponse> responseMap = new java.util.HashMap<>();
        for (ReportResponse response : responses) {
            if (response.getSubmittedBy() != null && response.getScore() != null) {
                String key = response.getReportRequest().getId() + "_" + response.getSubmittedBy().getId();
                responseMap.put(key, response);
            }
        }
        
        // Xử lý các response đã nộp và đã đánh giá
        for (ReportResponse response : responses) {
            if (response.getSubmittedBy() == null || response.getScore() == null) {
                continue;
            }
            
            ReportRequest request = response.getReportRequest();
            User submittedBy = response.getSubmittedBy();
            
            // Xác định trạng thái (Đúng hạn / Quá hạn)
            String status;
            if (response.getSubmittedAt() != null && request.getDeadline() != null) {
                if (response.getSubmittedAt().isBefore(request.getDeadline()) || 
                    response.getSubmittedAt().isEqual(request.getDeadline())) {
                    status = "Đúng hạn";
                    onTimeCount++;
                } else {
                    status = "Quá hạn";
                    overdueCount++;
                }
            } else {
                status = "Chưa xác định";
            }
            
            // Lấy danh sách organizations của user
            User userWithOrgs = userRepository.findByIdWithRelations(submittedBy.getId())
                    .orElse(submittedBy);
            List<OrganizationDTO> organizations = userWithOrgs.getOrganizations() != null ?
                    userWithOrgs.getOrganizations().stream()
                            .map(OrganizationDTO::fromEntity)
                            .collect(Collectors.toList()) : new ArrayList<>();
            
            // Tạo link tài liệu kiểm chứng từ response items
            String documentLink = buildDocumentLink(response);
            List<ReportStatisticsDTO.DocumentFileDTO> documentFiles = buildDocumentFiles(response);
            
            ReportStatisticsDTO.ReportStatisticItemDTO item = ReportStatisticsDTO.ReportStatisticItemDTO.builder()
                    .id(response.getId())
                    .stt(stt++)
                    .reportName(request.getTitle())
                    .reportAuthor(UserDTO.fromEntity(submittedBy))
                    .department(submittedBy.getDepartment() != null ? 
                            DepartmentDTO.fromEntity(submittedBy.getDepartment()) : null)
                    .organizations(organizations)
                    .score(response.getScore())
                    .reviewer(response.getEvaluatedBy() != null ? 
                            UserDTO.fromEntity(response.getEvaluatedBy()) : null)
                    .submissionDate(response.getSubmittedAt())
                    .status(status)
                    .documentLink(documentLink)
                    .documentFiles(documentFiles)
                    .reportRequestId(request.getId())
                    .build();
            
            reportItems.add(item);
            
            // Tính tổng điểm
            totalScore += response.getScore();
            scoredCount++;
        }
        
        // Xử lý các request chưa nộp - hiển thị trong bảng
        for (ReportRequest request : allRequests) {
            // Lấy tất cả user được giao request này
            java.util.Set<Long> assignedUserIds = new java.util.HashSet<>();
            java.util.Map<Long, User> userMap = new java.util.HashMap<>();
            
            // Lấy user IDs từ targetUsers
            if (request.getTargetUsers() != null) {
                for (User targetUser : request.getTargetUsers()) {
                    assignedUserIds.add(targetUser.getId());
                    userMap.put(targetUser.getId(), targetUser);
                }
            }
            
            // Lấy user IDs từ targetDepartments
            if (request.getTargetDepartments() != null) {
                for (Department dept : request.getTargetDepartments()) {
                    List<User> deptUsers = userRepository.findByDepartmentId(dept.getId());
                    for (User deptUser : deptUsers) {
                        assignedUserIds.add(deptUser.getId());
                        if (!userMap.containsKey(deptUser.getId())) {
                            userMap.put(deptUser.getId(), deptUser);
                        }
                    }
                }
            }
            
            // Lấy user IDs từ targetOrganizations
            if (request.getTargetOrganizations() != null) {
                for (Organization org : request.getTargetOrganizations()) {
                    List<User> orgUsers = userRepository.findByOrganizationId(org.getId());
                    for (User orgUser : orgUsers) {
                        assignedUserIds.add(orgUser.getId());
                        if (!userMap.containsKey(orgUser.getId())) {
                            userMap.put(orgUser.getId(), orgUser);
                        }
                    }
                }
            }
            
            // Kiểm tra từng user được giao
            for (Long userId : assignedUserIds) {
                String key = request.getId() + "_" + userId;
                ReportResponse response = responseMap.get(key);
                
                if (response == null) {
                    // Chưa nộp - chỉ hiển thị nếu quá hạn
                    if (request.getDeadline() != null && request.getDeadline().isBefore(now)) {
                        // Quá hạn nhưng chưa nộp - hiển thị trong bảng với trạng thái "Quá hạn"
                        overdueCount++;
                        
                        User user = userRepository.findByIdWithRelations(userId)
                                .orElse(userMap.get(userId));
                        if (user == null) continue;
                        
                        List<OrganizationDTO> organizations = user.getOrganizations() != null ?
                                user.getOrganizations().stream()
                                        .map(OrganizationDTO::fromEntity)
                                        .collect(Collectors.toList()) : new ArrayList<>();
                        
                        ReportStatisticsDTO.ReportStatisticItemDTO item = ReportStatisticsDTO.ReportStatisticItemDTO.builder()
                                .id(null) // Không có response ID
                                .stt(stt++)
                                .reportName(request.getTitle())
                                .reportAuthor(UserDTO.fromEntity(user))
                                .department(user.getDepartment() != null ? 
                                        DepartmentDTO.fromEntity(user.getDepartment()) : null)
                                .organizations(organizations)
                                .score(null) // Chưa có điểm vì chưa nộp
                                .reviewer(null) // Chưa có người chấm
                                .submissionDate(request.getDeadline()) // Dùng deadline làm ngày giao báo cáo
                                .status("Quá hạn")
                                .documentLink("-") // Chưa có tài liệu
                                .documentFiles(new ArrayList<>()) // Chưa có file
                                .reportRequestId(request.getId())
                                .build();
                        
                        reportItems.add(item);
                        
                        // Tính điểm trung bình: báo cáo quá hạn chưa nộp được tính là 0 điểm
                        // Không cần cộng vào totalScore vì đã là 0, nhưng cần tăng scoredCount để tính vào mẫu số
                        scoredCount++;
                    }
                    // Chưa đến hạn chưa nộp - không hiển thị
                }
            }
        }
        
        // Tính điểm trung bình: bao gồm cả các báo cáo quá hạn chưa nộp (tính là 0 điểm)
        double averageScore = scoredCount > 0 ? totalScore / scoredCount : 0;
        
        // Xác định xếp loại
        String rating = calculateRating(averageScore);
        
        ReportStatisticsDTO.SummaryDTO summary = ReportStatisticsDTO.SummaryDTO.builder()
                .totalReports(reportItems.size())
                .onTimeReports(onTimeCount)
                .overdueReports(overdueCount)
                .averageScore(averageScore)
                .rating(rating)
                .build();
        
        return ReportStatisticsDTO.builder()
                .reports(reportItems)
                .summary(summary)
                .build();
    }
    
    /**
     * Tính xếp loại dựa trên điểm trung bình
     * >= 8.5: A
     * >= 8 và < 8.5: B
     * >= 6 và < 8: C
     * < 6: D
     */
    private String calculateRating(double averageScore) {
        if (averageScore >= 8.5) {
            return "A";
        } else if (averageScore >= 8.0) {
            return "B";
        } else if (averageScore >= 6.0) {
            return "C";
        } else {
            return "D";
        }
    }
    
    /**
     * Tạo danh sách file tài liệu kiểm chứng từ response items
     */
    private List<ReportStatisticsDTO.DocumentFileDTO> buildDocumentFiles(ReportResponse response) {
        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            return new ArrayList<>();
        }
        
        // Lấy tất cả file từ items có file
        return response.getItems().stream()
                .filter(item -> item.getFilePath() != null && !item.getFilePath().trim().isEmpty())
                .map(item -> {
                    String fileName = item.getFileName() != null && !item.getFileName().trim().isEmpty() 
                            ? item.getFileName() 
                            : "file";
                    return ReportStatisticsDTO.DocumentFileDTO.builder()
                            .fileName(fileName)
                            .filePath(item.getFilePath())
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Tạo link tài liệu kiểm chứng từ response items (backward compatibility)
     */
    private String buildDocumentLink(ReportResponse response) {
        List<ReportStatisticsDTO.DocumentFileDTO> files = buildDocumentFiles(response);
        if (files.isEmpty()) {
            return "-";
        }
        
        // Tạo HTML string với links
        return files.stream()
                .map(file -> {
                    String link = String.format("/api/report-responses/files/%s", file.getFilePath());
                    return String.format("<a href=\"%s\" target=\"_blank\" class=\"text-blue-600 hover:underline\">%s</a>", 
                            link, file.getFileName());
                })
                .collect(Collectors.joining(", "));
    }
}

