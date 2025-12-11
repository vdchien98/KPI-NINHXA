package vn.gov.bacninh.ninhxareport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.gov.bacninh.ninhxareport.entity.ReportResponseItem;

import java.util.List;

@Repository
public interface ReportResponseItemRepository extends JpaRepository<ReportResponseItem, Long> {
    
    List<ReportResponseItem> findByReportResponseIdOrderByDisplayOrderAsc(Long responseId);
}

