package vn.gov.bacninh.ninhxareport.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gov.bacninh.ninhxareport.entity.*;
import vn.gov.bacninh.ninhxareport.repository.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service để gửi thông báo Zalo khi báo cáo sắp đến hạn
 * - Kiểm tra các báo cáo đã trôi qua 80% thời gian
 * - Gửi thông báo cho người nộp báo cáo
 * - Thời gian tối thiểu giữa các lần gửi là 30 phút
 */
@Service
@Slf4j
public class ReportDeadlineNotificationService {
    
    @Autowired
    private ReportRequestRepository reportRequestRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ZaloService zaloService;
    
    @Autowired
    private UserService userService;
    
    private static final double DEADLINE_THRESHOLD = 0.8; // 80%
    private static final int MIN_TIME_REMAINING_MINUTES = 30; // 30 phút
    
    /**
     * Scheduled job chạy mỗi 15 phút để kiểm tra và gửi thông báo
     * Job chỉ gửi 1 lần cho mỗi báo cáo (nếu đã gửi rồi thì không gửi lại)
     */
    @Scheduled(fixedRate = 900000) // 15 phút = 900000 milliseconds
    @Transactional
    public void checkAndSendDeadlineNotifications() {
        log.info("Bắt đầu kiểm tra báo cáo sắp đến hạn...");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Tìm các report requests đang pending hoặc in_progress và chưa quá deadline
            List<ReportRequestStatus> activeStatuses = Arrays.asList(
                ReportRequest.ReportRequestStatus.PENDING,
                ReportRequest.ReportRequestStatus.IN_PROGRESS
            );
            
            List<ReportRequest> activeRequests = reportRequestRepository.findActiveRequestsBeforeDeadline(
                activeStatuses, now
            );
            
            log.info("Tìm thấy {} báo cáo đang active", activeRequests.size());
            
            int notificationCount = 0;
            
            for (ReportRequest request : activeRequests) {
                if (shouldSendNotification(request, now)) {
                    sendNotificationToRecipientsAndCount(request);
                    notificationCount++;
                }
            }
            
            log.info("Đã gửi {} thông báo sắp đến hạn", notificationCount);
            
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra và gửi thông báo deadline: ", e);
        }
    }
    
    /**
     * Kiểm tra xem có nên gửi thông báo cho report request này không
     * Job chỉ gửi 1 lần - nếu đã gửi rồi thì không gửi lại
     */
    private boolean shouldSendNotification(ReportRequest request, LocalDateTime now) {
        // Job chỉ gửi 1 lần - nếu đã gửi rồi thì bỏ qua
        if (request.getLastDeadlineNotificationSentAt() != null) {
            log.debug("Báo cáo ID {} đã được gửi thông báo trước đó, bỏ qua", request.getId());
            return false;
        }
        
        // Kiểm tra điều kiện: >= 80% thời gian HOẶC thời gian còn lại < 30 phút
        double progressPercentage = calculateProgressPercentage(request, now);
        long minutesRemaining = ChronoUnit.MINUTES.between(now, request.getDeadline());
        
        boolean shouldSend = progressPercentage >= DEADLINE_THRESHOLD || minutesRemaining < MIN_TIME_REMAINING_MINUTES;
        
        if (!shouldSend) {
            log.debug("Báo cáo ID {} chưa đủ điều kiện gửi thông báo (progress: {:.2f}%, remaining: {} phút)", 
                request.getId(), progressPercentage * 100, minutesRemaining);
        }
        
        return shouldSend;
    }
    
    /**
     * API để admin chủ động gửi lại thông báo cho một report request
     * @param reportRequestId ID của report request
     * @return số lượng thông báo đã gửi thành công
     */
    @Transactional
    public int sendNotificationManually(Long reportRequestId) {
        ReportRequest request = reportRequestRepository.findById(reportRequestId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo với ID: " + reportRequestId));
        
        LocalDateTime now = LocalDateTime.now();
        
        // Kiểm tra báo cáo còn active và chưa quá deadline
        if (request.getStatus() != ReportRequest.ReportRequestStatus.PENDING 
            && request.getStatus() != ReportRequest.ReportRequestStatus.IN_PROGRESS) {
            throw new RuntimeException("Báo cáo không ở trạng thái active (PENDING hoặc IN_PROGRESS)");
        }
        
        if (request.getDeadline().isBefore(now)) {
            throw new RuntimeException("Báo cáo đã quá deadline");
        }
        
        // Gửi thông báo và trả về số lượng đã gửi thành công
        return sendNotificationToRecipientsAndCount(request);
    }
    
    /**
     * Gửi thông báo và trả về số lượng đã gửi thành công
     */
    private int sendNotificationToRecipientsAndCount(ReportRequest request) {
        Set<User> recipients = collectRecipients(request);
        
        if (recipients.isEmpty()) {
            log.warn("Báo cáo ID {} không có người nhận nào", request.getId());
            return 0;
        }
        
        // Tạo nội dung thông báo
        String message = buildNotificationMessage(request);
        
        int successCount = 0;
        int failCount = 0;
        
        for (User user : recipients) {
            String zaloUserId = user.getZaloUserId();
            
            // Nếu chưa có zaloUserId, thử tự động load từ phone number
            if (zaloUserId == null || zaloUserId.trim().isEmpty()) {
                if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
                    try {
                        log.info("User {} chưa có Zalo User ID, đang tự động load từ số điện thoại {}", 
                            user.getEmail(), user.getPhone());
                        zaloUserId = userService.getOrFetchZaloUserId(user);
                        
                        if (zaloUserId != null && !zaloUserId.trim().isEmpty()) {
                            log.info("Đã tự động load Zalo User ID cho user {}: {}", user.getEmail(), zaloUserId);
                        } else {
                            log.warn("Không thể load Zalo User ID cho user {} từ số điện thoại {}", 
                                user.getEmail(), user.getPhone());
                        }
                    } catch (Exception e) {
                        log.warn("Không thể tự động load Zalo User ID cho user {}: {}", 
                            user.getEmail(), e.getMessage());
                    }
                }
            }
            
            // Nếu vẫn không có zaloUserId sau khi thử load, bỏ qua
            if (zaloUserId == null || zaloUserId.trim().isEmpty()) {
                log.debug("User {} không có Zalo User ID, bỏ qua", user.getEmail());
                failCount++;
                continue;
            }
            
            try {
                boolean sent = zaloService.sendNotification(zaloUserId, message);
                if (sent) {
                    successCount++;
                    log.info("Đã gửi thông báo Zalo cho user {} về báo cáo ID {}", 
                        user.getEmail(), request.getId());
                } else {
                    failCount++;
                    log.warn("Không thể gửi thông báo Zalo cho user {} về báo cáo ID {}", 
                        user.getEmail(), request.getId());
                }
            } catch (Exception e) {
                failCount++;
                log.error("Lỗi khi gửi thông báo Zalo cho user {}: ", user.getEmail(), e);
            }
        }
        
        // Cập nhật thời gian gửi thông báo cuối cùng
        if (successCount > 0) {
            request.setLastDeadlineNotificationSentAt(LocalDateTime.now());
            reportRequestRepository.save(request);
            log.info("Đã gửi thông báo cho {}/{} người nhận của báo cáo ID {}", 
                successCount, recipients.size(), request.getId());
        } else {
            log.warn("Không gửi được thông báo nào cho báo cáo ID {}", request.getId());
        }
        
        return successCount;
    }
    
    /**
     * Tính phần trăm thời gian đã trôi qua (từ createdAt đến deadline)
     */
    private double calculateProgressPercentage(ReportRequest request, LocalDateTime now) {
        LocalDateTime createdAt = request.getCreatedAt();
        LocalDateTime deadline = request.getDeadline();
        
        if (createdAt == null || deadline == null) {
            return 0.0;
        }
        
        long totalDuration = ChronoUnit.SECONDS.between(createdAt, deadline);
        long elapsedDuration = ChronoUnit.SECONDS.between(createdAt, now);
        
        if (totalDuration <= 0) {
            return 1.0; // Đã quá deadline
        }
        
        return (double) elapsedDuration / totalDuration;
    }
    
    
    /**
     * Thu thập users cần nhận thông báo từ report request
     * Chỉ gửi cho người phụ trách (users được chỉ định trực tiếp trong targetUsers)
     */
    private Set<User> collectRecipients(ReportRequest request) {
        Set<User> recipients = new HashSet<>();
        
        // Chỉ thêm users được chỉ định trực tiếp (người phụ trách)
        if (request.getTargetUsers() != null && !request.getTargetUsers().isEmpty()) {
            recipients.addAll(request.getTargetUsers());
        }
        
        // Lọc chỉ lấy active users
        return recipients.stream()
            .filter(user -> user.getIsActive() != null && user.getIsActive())
            .collect(Collectors.toSet());
    }
    
    /**
     * Tạo nội dung thông báo
     */
    private String buildNotificationMessage(ReportRequest request) {
        LocalDateTime deadline = request.getDeadline();
        LocalDateTime now = LocalDateTime.now();
        long hoursRemaining = ChronoUnit.HOURS.between(now, deadline);
        long daysRemaining = ChronoUnit.DAYS.between(now, deadline);
        
        String timeRemaining;
        if (daysRemaining > 0) {
            timeRemaining = daysRemaining + " ngày";
        } else if (hoursRemaining > 0) {
            timeRemaining = hoursRemaining + " giờ";
        } else {
            long minutesRemaining = ChronoUnit.MINUTES.between(now, deadline);
            timeRemaining = minutesRemaining + " phút";
        }
        
        return String.format(
            "🔔 Thông báo sắp đến hạn báo cáo\n\n" +
            "📋 Tiêu đề: %s\n" +
            "⏰ Hạn nộp: %s\n" +
            "⏳ Còn lại: %s\n\n" +
            "Vui lòng hoàn thành và nộp báo cáo trước thời hạn.",
            request.getTitle(),
            formatDateTime(deadline),
            timeRemaining
        );
    }
    
    /**
     * Format LocalDateTime thành chuỗi dễ đọc
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        
        return String.format("%02d/%02d/%04d %02d:%02d",
            dateTime.getDayOfMonth(),
            dateTime.getMonthValue(),
            dateTime.getYear(),
            dateTime.getHour(),
            dateTime.getMinute()
        );
    }
}

