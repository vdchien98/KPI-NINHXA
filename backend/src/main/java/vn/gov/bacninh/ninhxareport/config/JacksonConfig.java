package vn.gov.bacninh.ninhxareport.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Cấu hình Jackson để serialize datetime
 * 
 * Logic:
 * - Tất cả datetime trong database và server được lưu/xử lý theo UTC
 * - Khi serialize JSON, tự động convert UTC → Asia/Ho_Chi_Minh (+07:00)
 * - Trả về ISO 8601 format với timezone (ví dụ: 2024-12-21T10:30:00+07:00)
 * 
 * Lưu ý: JVM timezone được set là UTC trong NinhXaReportApplication.main()
 */
@Configuration
public class JacksonConfig {
    
    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();
        
        // Đăng ký JavaTimeModule để xử lý ZonedDateTime, Instant, etc.
        objectMapper.registerModule(new JavaTimeModule());
        
        // Custom serializer cho LocalDateTime để convert UTC → Asia/Ho_Chi_Minh
        SimpleModule localDateTimeModule = new SimpleModule();
        localDateTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
        objectMapper.registerModule(localDateTimeModule);
        
        // Disable WRITE_DATES_AS_TIMESTAMPS để trả về ISO 8601 format thay vì timestamp
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // ObjectMapper timezone vẫn là UTC (vì LocalDateTime được xử lý bởi custom serializer)
        objectMapper.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        
        return objectMapper;
    }
    
    /**
     * Custom serializer cho LocalDateTime
     * 
     * Logic:
     * - LocalDateTime được hiểu là UTC (vì JVM timezone là UTC)
     * - Convert UTC → Asia/Ho_Chi_Minh (+07:00) khi serialize
     * - Ví dụ: UTC 11:30:00 → VN 18:30:00+07:00
     */
    private static class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) 
                throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                // LocalDateTime được hiểu là UTC, convert sang ZonedDateTime UTC
                ZonedDateTime utcDateTime = value.atZone(UTC_ZONE);
                // Convert sang timezone Asia/Ho_Chi_Minh (+07:00)
                ZonedDateTime vietnamDateTime = utcDateTime.withZoneSameInstant(VIETNAM_ZONE);
                // Format theo ISO 8601 với timezone offset
                gen.writeString(vietnamDateTime.format(ISO_FORMATTER));
            }
        }
    }
}

