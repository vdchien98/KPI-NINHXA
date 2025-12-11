package vn.gov.bacninh.ninhxareport.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "report_responses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_request_id", nullable = false)
    private ReportRequest reportRequest;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;
    
    @Column(length = 2000)
    private String note;
    
    @OneToMany(mappedBy = "reportResponse", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReportResponseItem> items = new ArrayList<>();
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "score")
    private Double score; // Điểm đánh giá của người gửi yêu cầu
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluated_by")
    private User evaluatedBy; // Người đánh giá (người gửi yêu cầu)
    
    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt; // Thời gian đánh giá của người gửi yêu cầu
    
    @Column(name = "self_score")
    private Double selfScore; // Điểm tự đánh giá của người nộp báo cáo
    
    @Column(name = "self_evaluated_at")
    private LocalDateTime selfEvaluatedAt; // Thời gian tự đánh giá của người nộp báo cáo
    
    @Column(name = "comment", length = 2000)
    private String comment; // Nhận xét của người đánh giá
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        submittedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public void addItem(ReportResponseItem item) {
        items.add(item);
        item.setReportResponse(this);
    }
    
    public void removeItem(ReportResponseItem item) {
        items.remove(item);
        item.setReportResponse(null);
    }
}

