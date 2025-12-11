package vn.gov.bacninh.ninhxareport.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_request_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequestHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_request_id", nullable = false)
    private ReportRequest reportRequest;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_by", nullable = false)
    private User editedBy;
    
    @Column(name = "edited_at", nullable = false)
    private LocalDateTime editedAt;
    
    @Column(name = "version_number")
    private Integer versionNumber;
    
    @Column(name = "title_snapshot", length = 500)
    private String titleSnapshot;
    
    @Column(name = "description_snapshot", length = 2000)
    private String descriptionSnapshot;
    
    @Column(name = "deadline_snapshot", nullable = false)
    private LocalDateTime deadlineSnapshot;
    
    @Lob
    @Column(name = "target_organizations_snapshot", columnDefinition = "TEXT")
    private String targetOrganizationsSnapshotJson;
    
    @Lob
    @Column(name = "target_departments_snapshot", columnDefinition = "TEXT")
    private String targetDepartmentsSnapshotJson;
    
    @Lob
    @Column(name = "target_users_snapshot", columnDefinition = "TEXT")
    private String targetUsersSnapshotJson;
    
    @PrePersist
    protected void onCreate() {
        editedAt = LocalDateTime.now();
    }
}

