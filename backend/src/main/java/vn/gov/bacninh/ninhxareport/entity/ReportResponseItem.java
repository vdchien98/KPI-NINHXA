package vn.gov.bacninh.ninhxareport.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_response_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponseItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_response_id", nullable = false)
    private ReportResponse reportResponse;
    
    @Column(length = 500)
    private String title; // Tiêu đề mục báo cáo
    
    @Column(length = 2000)
    private String content; // Nội dung mục báo cáo
    
    @Column(name = "progress")
    @Builder.Default
    private Integer progress = 0; // Tiến độ hoàn thành (0-100%)
    
    @Column(length = 2000)
    private String difficulties; // Khó khăn gặp phải
    
    @Column(name = "file_name", length = 500)
    private String fileName;
    
    @Column(name = "file_path", length = 1000)
    private String filePath;
    
    @Column(name = "file_type", length = 100)
    private String fileType;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

