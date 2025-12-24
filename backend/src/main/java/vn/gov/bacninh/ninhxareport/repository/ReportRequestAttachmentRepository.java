package vn.gov.bacninh.ninhxareport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.gov.bacninh.ninhxareport.entity.ReportRequestAttachment;

import java.util.List;

@Repository
public interface ReportRequestAttachmentRepository extends JpaRepository<ReportRequestAttachment, Long> {
    List<ReportRequestAttachment> findByReportRequestId(Long reportRequestId);
    void deleteByReportRequestId(Long reportRequestId);
}

