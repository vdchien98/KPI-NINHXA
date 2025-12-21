package vn.gov.bacninh.ninhxareport.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Cấu hình timezone mặc định cho ứng dụng
 * Đảm bảo tất cả các thao tác với ngày giờ đều sử dụng timezone Việt Nam (Asia/Ho_Chi_Minh)
 */
@Configuration
public class TimezoneConfig {

    @PostConstruct
    public void init() {
        // Thiết lập timezone mặc định cho JVM
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        System.setProperty("user.timezone", "Asia/Ho_Chi_Minh");
    }
}

