package vn.gov.bacninh.ninhxareport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.gov.bacninh.ninhxareport.dto.zalo.ZaloOAuthTokenResponse;
import vn.gov.bacninh.ninhxareport.entity.ZaloAccessToken;
import vn.gov.bacninh.ninhxareport.repository.ZaloAccessTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZaloOAuthService {

    private final ZaloAccessTokenRepository tokenRepository;
    private final ObjectMapper objectMapper;

    @Value("${zalo.oauth.app-id:}")
    private String appId;

    @Value("${zalo.oauth.secret:}")
    private String appSecret;

    private final WebClient oauthClient = WebClient.builder()
            .baseUrl("https://oauth.zaloapp.com")
            .build();

    /**
     * Lấy access token hợp lệ (tự động refresh nếu hết hạn)
     * Logic tương tự code mẫu: kiểm tra cache/DB, nếu null hoặc hết hạn thì refresh
     */
    public String getValidAccessToken() {
        validateConfig();
        
        Optional<ZaloAccessToken> tokenOpt = tokenRepository.findFirstByOrderByIdAsc();
        
        if (tokenOpt.isEmpty()) {
            throw new IllegalStateException(
                    "Zalo access token chưa được khởi tạo. Vui lòng thiết lập refresh_token trước bằng endpoint /api/zalo/oauth/init."
            );
        }

        ZaloAccessToken token = tokenOpt.get();
        
        // Kiểm tra token hết hạn (trừ 60 giây buffer)
        if (token.getExpiresAt() == null || token.getExpiresAt().isBefore(Instant.now().plusSeconds(60))) {
            log.info("Access token expired or about to expire, refreshing...");
            token = refreshAccessToken(token);
        }
        
        return token.getAccessToken();
    }

    /**
     * Refresh access token bằng refresh token
     * Logic từ code mẫu: POST /v4/oa/access_token với refresh_token
     */
    @Transactional
    public ZaloAccessToken refreshAccessToken(ZaloAccessToken token) {
        if (token.getRefreshToken() == null || token.getRefreshToken().isBlank()) {
            throw new IllegalStateException("Refresh token bị thiếu. Vui lòng khởi tạo lại OAuth.");
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("refresh_token", token.getRefreshToken());
            form.add("app_id", appId);
            form.add("grant_type", "refresh_token");

            String json = oauthClient.post()
                    .uri("/v4/oa/access_token")
                    .header("secret_key", appSecret)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (json == null || json.isEmpty()) {
                throw new IllegalStateException("Phản hồi trống từ Zalo OAuth");
            }

            log.info("Zalo OAuth response: {}", json);

            ZaloOAuthTokenResponse response = objectMapper.readValue(json, ZaloOAuthTokenResponse.class);

            if (response == null || response.getAccessToken() == null) {
                throw new IllegalStateException("Không thể lấy Zalo access token");
            }

            // Cập nhật token
            token.setAccessToken(response.getAccessToken());
            token.setRefreshToken(response.getRefreshToken() != null ? response.getRefreshToken() : token.getRefreshToken());
            token.setTokenType(response.getTokenType());
            token.setScope(response.getScope());
            token.setExpiresAt(Instant.now().plusSeconds(response.getExpiresIn() != null ? response.getExpiresIn() : 3600));
            
            return tokenRepository.save(token);
            
        } catch (WebClientResponseException e) {
            log.error("Zalo OAuth API error: HTTP {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new IllegalStateException("Không thể làm mới Zalo access token: " + e.getStatusCode(), e);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Zalo OAuth response: {}", e.getMessage(), e);
            throw new IllegalStateException("Không thể phân tích phản hồi token từ Zalo", e);
        } catch (Exception e) {
            log.error("Error refreshing Zalo access token: {}", e.getMessage(), e);
            throw new RuntimeException("Làm mới Zalo OAuth thất bại", e);
        }
    }

    /**
     * Khởi tạo token lần đầu bằng refresh token
     * Dùng khi đã có refresh token từ OAuth flow hoặc manual setup
     */
    @Transactional
    public ZaloAccessToken initializeToken(String refreshToken) {
        validateConfig();
        
        // Xóa token cũ nếu có
        tokenRepository.deleteAll();
        
        // Tạo token mới trong memory (chưa save vào DB)
        ZaloAccessToken token = new ZaloAccessToken();
        token.setRefreshToken(refreshToken);
        token.setExpiresAt(Instant.MIN); // Force refresh
        
        // Refresh để lấy access token - method này sẽ save vào DB
        return refreshAccessToken(token);
    }

    /**
     * Kiểm tra xem token đã được khởi tạo chưa
     */
    public boolean isTokenInitialized() {
        return tokenRepository.findFirstByOrderByIdAsc().isPresent();
    }
    
    /**
     * Lấy thông tin token chi tiết (không bao gồm access_token và refresh_token vì bảo mật)
     */
    public vn.gov.bacninh.ninhxareport.dto.zalo.ZaloTokenInfoDTO getTokenInfo() {
        Optional<ZaloAccessToken> tokenOpt = tokenRepository.findFirstByOrderByIdAsc();
        if (tokenOpt.isEmpty()) {
            return null;
        }
        
        ZaloAccessToken token = tokenOpt.get();
        Instant now = Instant.now();
        Instant expiresAt = token.getExpiresAt();
        
        boolean isExpired = expiresAt != null && expiresAt.isBefore(now);
        boolean isExpiringSoon = expiresAt != null && expiresAt.isBefore(now.plusSeconds(3600)); // 1 hour
        
        String expiresIn = "N/A";
        if (expiresAt != null) {
            long secondsUntilExpiry = expiresAt.getEpochSecond() - now.getEpochSecond();
            if (secondsUntilExpiry > 0) {
                long hours = secondsUntilExpiry / 3600;
                long minutes = (secondsUntilExpiry % 3600) / 60;
                if (hours > 0) {
                    expiresIn = hours + " giờ " + (minutes > 0 ? minutes + " phút" : "");
                } else {
                    expiresIn = minutes + " phút";
                }
            } else {
                expiresIn = "Đã hết hạn";
            }
        }
        
        return vn.gov.bacninh.ninhxareport.dto.zalo.ZaloTokenInfoDTO.builder()
                .id(token.getId())
                .tokenType(token.getTokenType())
                .scope(token.getScope())
                .expiresAt(expiresAt)
                .createdAt(token.getCreatedAt())
                .updatedAt(token.getUpdatedAt())
                .isExpired(isExpired)
                .isExpiringSoon(isExpiringSoon)
                .expiresIn(expiresIn)
                .build();
    }

    private void validateConfig() {
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new IllegalStateException("Cấu hình Zalo OAuth bị thiếu. Vui lòng kiểm tra application properties.");
        }
    }
}

