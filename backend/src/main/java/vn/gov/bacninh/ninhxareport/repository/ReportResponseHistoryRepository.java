package vn.gov.bacninh.ninhxareport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gov.bacninh.ninhxareport.entity.ReportResponseHistory;

import java.util.List;

public interface ReportResponseHistoryRepository extends JpaRepository<ReportResponseHistory, Long> {
    List<ReportResponseHistory> findByReportResponseIdOrderByEditedAtDesc(Long responseId);
    long countByReportResponseId(Long responseId);
}


