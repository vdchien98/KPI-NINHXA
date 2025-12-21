package vn.gov.bacninh.ninhxareport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class NinhXaReportApplication {
    public static void main(String[] args) {
        // Thiết lập timezone Việt Nam ngay khi khởi động ứng dụng
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        System.setProperty("user.timezone", "Asia/Ho_Chi_Minh");
        
        SpringApplication.run(NinhXaReportApplication.class, args);
    }
}

