package vn.gov.bacninh.ninhxareport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class NinhXaReportApplication {
    public static void main(String[] args) {
        // Thiết lập timezone UTC cho toàn bộ ứng dụng
        // Tất cả datetime được lưu và xử lý theo UTC
        // Khi trả về JSON, JacksonConfig sẽ tự động convert sang Asia/Ho_Chi_Minh (+07:00)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        System.setProperty("user.timezone", "UTC");
        
        SpringApplication.run(NinhXaReportApplication.class, args);
    }
}

