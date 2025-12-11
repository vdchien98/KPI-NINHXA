package vn.gov.bacninh.ninhxareport.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_response_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponseHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_response_id", nullable = false)
    private ReportResponse reportResponse;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_by", nullable = false)
    private User editedBy;
    
    @Column(name = "edited_at", nullable = false)
    private LocalDateTime editedAt;
    
    @Column(name = "version_number")
    private Integer versionNumber;
    
    @Column(name = "note", length = 2000)
    private String noteSnapshot;
    
    @Lob
    @Column(name = "items_snapshot", columnDefinition = "TEXT")
    private String itemsSnapshotJson;
    
    @PrePersist
    protected void onCreate() {
        editedAt = LocalDateTime.now();
    }
}


