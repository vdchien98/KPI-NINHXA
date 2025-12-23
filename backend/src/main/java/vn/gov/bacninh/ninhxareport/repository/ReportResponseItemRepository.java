package vn.gov.bacninh.ninhxareport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.gov.bacninh.ninhxareport.entity.ReportResponseItem;

import java.util.List;

@Repository
public interface ReportResponseItemRepository extends JpaRepository<ReportResponseItem, Long> {
    
    List<ReportResponseItem> findByReportResponseIdOrderByDisplayOrderAsc(Long responseId);
    
    @org.springframework.data.jpa.repository.Query("SELECT i FROM ReportResponseItem i WHERE i.reportResponse.id IN :responseIds ORDER BY i.reportResponse.id, i.displayOrder ASC")
    List<ReportResponseItem> findByReportResponseIdInOrderByDisplayOrderAsc(@org.springframework.data.repository.query.Param("responseIds") List<Long> responseIds);
}

