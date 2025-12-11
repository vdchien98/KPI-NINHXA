package vn.gov.bacninh.ninhxareport.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_response_comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponseComment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_response_id", nullable = false)
    private ReportResponse reportResponse;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commented_by", nullable = false)
    private User commentedBy;
    
    @Column(name = "comment", length = 2000, nullable = false)
    private String comment;
    
    @Column(name = "score")
    private Double score; // Điểm đánh giá (nếu có)
    
    @Column(name = "is_final_evaluation")
    @Builder.Default
    private Boolean isFinalEvaluation = false; // true nếu là đánh giá cuối cùng, false nếu là gửi lại
    
    @Column(name = "commented_at", nullable = false)
    private LocalDateTime commentedAt;
    
    @PrePersist
    protected void onCreate() {
        commentedAt = LocalDateTime.now();
    }
}

