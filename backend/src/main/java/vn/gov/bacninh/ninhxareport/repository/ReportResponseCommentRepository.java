package vn.gov.bacninh.ninhxareport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gov.bacninh.ninhxareport.entity.ReportResponseComment;

import java.util.List;

public interface ReportResponseCommentRepository extends JpaRepository<ReportResponseComment, Long> {
    List<ReportResponseComment> findByReportResponseIdOrderByCommentedAtDesc(Long responseId);
}

