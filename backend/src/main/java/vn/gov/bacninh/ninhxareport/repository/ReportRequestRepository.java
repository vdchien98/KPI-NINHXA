package vn.gov.bacninh.ninhxareport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.gov.bacninh.ninhxareport.entity.ReportRequest;

import java.util.List;

@Repository
public interface ReportRequestRepository extends JpaRepository<ReportRequest, Long> {
    
    List<ReportRequest> findByCreatedByIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT DISTINCT r FROM ReportRequest r " +
           "LEFT JOIN r.targetOrganizations o " +
           "LEFT JOIN r.targetDepartments d " +
           "LEFT JOIN r.targetUsers u " +
           "WHERE o.id IN :orgIds OR d.id IN :deptIds OR u.id = :userId " +
           "ORDER BY r.createdAt DESC")
    List<ReportRequest> findRequestsForUser(
        @Param("orgIds") List<Long> organizationIds,
        @Param("deptIds") List<Long> departmentIds,
        @Param("userId") Long userId
    );
    
    @Query("SELECT r FROM ReportRequest r ORDER BY r.createdAt DESC")
    List<ReportRequest> findAllOrderByCreatedAtDesc();
    
    @Query("SELECT DISTINCT r FROM ReportRequest r " +
           "JOIN r.targetUsers u " +
           "WHERE u.id = :userId " +
           "ORDER BY r.createdAt DESC")
    List<ReportRequest> findReceivedByUserId(@Param("userId") Long userId);
}

