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
        return reportResponseRepository.findByReportRequestIdOrderByCreatedAtDesc(requestId).stream()
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
}

