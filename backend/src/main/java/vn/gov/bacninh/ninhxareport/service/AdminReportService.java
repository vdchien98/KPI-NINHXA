package vn.gov.bacninh.ninhxareport.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gov.bacninh.ninhxareport.dto.*;
import vn.gov.bacninh.ninhxareport.entity.*;
import vn.gov.bacninh.ninhxareport.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminReportService {
    
    @Autowired
    private ReportRequestRepository reportRequestRepository;
    
    @Autowired
    private ReportResponseRepository reportResponseRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private ReportRequestService reportRequestService;
    
    /**
     * Lấy tất cả báo cáo với filter và search
     */
    public List<ReportRequestDTO> getAllReportsWithFilters(
            String search, String status, Long createdById, Long submittedById,
            Long organizationId, Long departmentId) {
        
        List<ReportRequest> allRequests = reportRequestRepository.findAllOrderByCreatedAtDesc();
        
        return allRequests.stream()
                .filter(request -> {
                    // Filter theo search (title hoặc description)
                    if (search != null && !search.isEmpty()) {
                        String searchLower = search.toLowerCase();
                        boolean matchTitle = request.getTitle().toLowerCase().contains(searchLower);
                        boolean matchDesc = request.getDescription() != null && 
                                request.getDescription().toLowerCase().contains(searchLower);
                        if (!matchTitle && !matchDesc) {
                            return false;
                        }
                    }
                    
                    // Filter theo status
                    if (status != null && !status.isEmpty()) {
                        if (!request.getStatus().name().equals(status)) {
                            return false;
                        }
                    }
                    
                    // Filter theo người tạo
                    if (createdById != null) {
                        if (!request.getCreatedBy().getId().equals(createdById)) {
                            return false;
                        }
                    }
                    
                    // Filter theo organization
                    if (organizationId != null) {
                        boolean hasOrg = request.getTargetOrganizations().stream()
                                .anyMatch(org -> org.getId().equals(organizationId));
                        if (!hasOrg) {
                            return false;
                        }
                    }
                    
                    // Filter theo department
                    if (departmentId != null) {
                        boolean hasDept = request.getTargetDepartments().stream()
                                .anyMatch(dept -> dept.getId().equals(departmentId));
                        if (!hasDept) {
                            return false;
                        }
                    }
                    
                    // Filter theo người báo cáo (submittedById)
                    if (submittedById != null) {
                        List<ReportResponse> responses = reportResponseRepository
                                .findByReportRequestIdOrderByCreatedAtDesc(request.getId());
                        boolean hasSubmitter = responses.stream()
                                .anyMatch(resp -> resp.getSubmittedBy().getId().equals(submittedById));
                        if (!hasSubmitter) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .map(request -> enrichWithStatistics(ReportRequestDTO.fromEntity(request), request.getId()))
                .collect(Collectors.toList());
    }
    
    /**
     * Thống kê báo cáo
     */
    public Map<String, Object> getReportStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<ReportRequest> allRequests = reportRequestRepository.findAll();
        List<ReportResponse> allResponses = reportResponseRepository.findAll();
        
        // Tổng số báo cáo
        stats.put("totalReports", allRequests.size());
        
        // Báo cáo theo trạng thái
        Map<String, Long> byStatus = allRequests.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getStatus().name(),
                        Collectors.counting()
                ));
        stats.put("byStatus", byStatus);
        
        // Tổng số response
        stats.put("totalResponses", allResponses.size());
        
        // Response đã được đánh giá
        long evaluatedResponses = allResponses.stream()
                .filter(r -> r.getScore() != null)
                .count();
        stats.put("evaluatedResponses", evaluatedResponses);
        
        // Điểm trung bình
        Double avgScore = allResponses.stream()
                .filter(r -> r.getScore() != null)
                .mapToDouble(ReportResponse::getScore)
                .average()
                .orElse(0.0);
        stats.put("averageScore", Math.round(avgScore * 100.0) / 100.0);
        
        // Top 5 người tạo báo cáo nhiều nhất
        Map<String, Long> topCreators = allRequests.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedBy().getFullName(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        stats.put("topCreators", topCreators);
        
        // Top 5 người báo cáo nhiều nhất
        Map<String, Long> topSubmitters = allResponses.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getSubmittedBy().getFullName(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        stats.put("topSubmitters", topSubmitters);
        
        // Báo cáo quá hạn
        long overdueReports = allRequests.stream()
                .filter(r -> r.getDeadline() != null && 
                             r.getDeadline().isBefore(LocalDateTime.now()) &&
                             r.getStatus() != ReportRequest.ReportRequestStatus.COMPLETED)
                .count();
        stats.put("overdueReports", overdueReports);
        
        return stats;
    }
    
    /**
     * Admin cập nhật yêu cầu báo cáo
     */
    @Transactional
    public ReportRequestDTO adminUpdateReportRequest(Long id, AdminUpdateReportRequestDTO dto) {
        ReportRequest request = reportRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu báo cáo"));
        
        // Cập nhật thông tin cơ bản
        request.setTitle(dto.getTitle());
        request.setDescription(dto.getDescription());
        request.setDeadline(dto.getDeadline());
        
        // Cập nhật status
        if (dto.getStatus() != null) {
            try {
                ReportRequest.ReportRequestStatus newStatus = 
                        ReportRequest.ReportRequestStatus.valueOf(dto.getStatus());
                request.setStatus(newStatus);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Trạng thái không hợp lệ: " + dto.getStatus());
            }
        }
        
        // Cập nhật target organizations
        if (dto.getOrganizationIds() != null) {
            Set<Organization> orgs = new HashSet<>();
            for (Long orgId : dto.getOrganizationIds()) {
                Organization org = organizationRepository.findById(orgId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy tổ chức với id: " + orgId));
                orgs.add(org);
            }
            request.setTargetOrganizations(orgs);
        }
        
        // Cập nhật target departments
        if (dto.getDepartmentIds() != null) {
            Set<Department> depts = new HashSet<>();
            for (Long deptId : dto.getDepartmentIds()) {
                Department dept = departmentRepository.findById(deptId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng ban với id: " + deptId));
                depts.add(dept);
            }
            request.setTargetDepartments(depts);
        }
        
        ReportRequest saved = reportRequestRepository.save(request);
        return enrichWithStatistics(ReportRequestDTO.fromEntity(saved), saved.getId());
    }
    
    /**
     * Admin cập nhật nội dung báo cáo
     */
    @Transactional
    public ReportResponseDTO adminUpdateReportResponse(Long id, AdminUpdateReportResponseDTO dto) {
        ReportResponse response = reportResponseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo"));
        
        // Cập nhật thông tin
        if (dto.getNote() != null) {
            response.setNote(dto.getNote());
        }
        
        if (dto.getScore() != null) {
            response.setScore(dto.getScore());
        }
        
        if (dto.getComment() != null) {
            response.setComment(dto.getComment());
        }
        
        if (dto.getSelfScore() != null) {
            response.setSelfScore(dto.getSelfScore());
        }
        
        // Cập nhật items nếu có
        if (dto.getItems() != null) {
            response.getItems().clear();
            for (AdminUpdateReportResponseDTO.ItemUpdateDTO itemDto : dto.getItems()) {
                ReportResponseItem item = ReportResponseItem.builder()
                        .title(itemDto.getTitle())
                        .content(itemDto.getContent())
                        .progress(itemDto.getProgress())
                        .difficulties(itemDto.getDifficulties())
                        .reportResponse(response)
                        .build();
                response.getItems().add(item);
            }
        }
        
        ReportResponse saved = reportResponseRepository.save(response);
        return ReportResponseDTO.fromEntity(saved);
    }
    
    /**
     * Lấy response theo ID
     */
    public ReportResponseDTO getResponseById(Long id) {
        ReportResponse response = reportResponseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo"));
        return ReportResponseDTO.fromEntity(response);
    }
    
    /**
     * Enrich DTO với thống kê
     */
    private ReportRequestDTO enrichWithStatistics(ReportRequestDTO dto, Long requestId) {
        List<ReportResponse> responses = reportResponseRepository.findByReportRequestIdOrderByCreatedAtDesc(requestId);
        
        dto.setTotalResponses(responses.size());
        
        long evaluatedCount = responses.stream()
                .filter(r -> r.getScore() != null)
                .count();
        dto.setEvaluatedResponses((int) evaluatedCount);
        
        double avgScore = responses.stream()
                .filter(r -> r.getScore() != null)
                .mapToDouble(ReportResponse::getScore)
                .average()
                .orElse(0.0);
        dto.setAverageScore(Math.round(avgScore * 100.0) / 100.0);
        
        return dto;
    }
}

