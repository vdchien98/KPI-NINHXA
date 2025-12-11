package vn.gov.bacninh.ninhxareport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gov.bacninh.ninhxareport.entity.ReportRequestHistory;

import java.util.List;

public interface ReportRequestHistoryRepository extends JpaRepository<ReportRequestHistory, Long> {
    List<ReportRequestHistory> findByReportRequestIdOrderByEditedAtDesc(Long requestId);
    long countByReportRequestId(Long requestId);
}

