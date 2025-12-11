package vn.gov.bacninh.ninhxareport.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "report_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @Column(length = 2000)
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "report_request_organizations",
        joinColumns = @JoinColumn(name = "report_request_id"),
        inverseJoinColumns = @JoinColumn(name = "organization_id")
    )
    @Builder.Default
    private Set<Organization> targetOrganizations = new HashSet<>();
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "report_request_departments",
        joinColumns = @JoinColumn(name = "report_request_id"),
        inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    @Builder.Default
    private Set<Department> targetDepartments = new HashSet<>();
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "report_request_users",
        joinColumns = @JoinColumn(name = "report_request_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> targetUsers = new HashSet<>();
    
    @Column(name = "deadline", nullable = false)
    private LocalDateTime deadline;
    
    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportRequestStatus status = ReportRequestStatus.PENDING;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum ReportRequestStatus {
        PENDING,    // Đang chờ
        IN_PROGRESS, // Đang thực hiện
        SUBMITTED,  // Đã nộp, chờ đánh giá
        COMPLETED,  // Hoàn thành
        CANCELLED   // Đã hủy
    }
}

