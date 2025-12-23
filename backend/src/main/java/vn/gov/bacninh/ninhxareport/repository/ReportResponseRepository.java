package vn.gov.bacninh.ninhxareport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.gov.bacninh.ninhxareport.entity.ReportResponse;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportResponseRepository extends JpaRepository<ReportResponse, Long> {
    
    List<ReportResponse> findByReportRequestIdOrderByCreatedAtDesc(Long reportRequestId);
    
    List<ReportResponse> findBySubmittedByIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT r FROM ReportResponse r WHERE r.reportRequest.id = :requestId AND r.submittedBy.id = :userId")
    Optional<ReportResponse> findByReportRequestIdAndSubmittedById(
            @Param("requestId") Long requestId, 
            @Param("userId") Long userId);
    
    boolean existsByReportRequestIdAndSubmittedById(Long requestId, Long userId);
    
    @Query("SELECT DISTINCT r FROM ReportResponse r " +
           "JOIN FETCH r.reportRequest req " +
           "JOIN FETCH req.createdBy " +
           "JOIN FETCH r.submittedBy u " +
           "LEFT JOIN FETCH u.department " +
           "LEFT JOIN FETCH r.evaluatedBy " +
           "WHERE r.submittedBy.id = :userId " +
           "ORDER BY r.submittedAt DESC")
    List<ReportResponse> findBySubmittedByIdWithRelations(@Param("userId") Long userId);
    
    @Query("SELECT DISTINCT r FROM ReportResponse r " +
           "JOIN FETCH r.reportRequest req " +
           "JOIN FETCH req.createdBy " +
           "JOIN FETCH r.submittedBy u " +
           "LEFT JOIN FETCH u.department " +
           "LEFT JOIN FETCH r.evaluatedBy " +
           "ORDER BY r.submittedAt DESC")
    List<ReportResponse> findAllWithRelations();
}

