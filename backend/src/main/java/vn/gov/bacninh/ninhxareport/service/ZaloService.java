package vn.gov.bacninh.ninhxareport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.gov.bacninh.ninhxareport.dto.zalo.ZaloUserInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ZaloService {
    
    private final ZaloOAuthService oauthService;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${zalo.oauth.secret:}")
    private String appSecret;
    
    public ZaloService(ZaloOAuthService oauthService, ObjectMapper objectMapper) {
        this.oauthService = oauthService;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl("https://openapi.zalo.me")
                .build();
    }
    
    /**
     * Tính appsecret_proof bằng HMAC-SHA256 của access_token với app_secret
     * Kết quả được encode hex (giống Hex.encodeHexString)
     * 
     * @param accessToken Access token từ Zalo
     * @return appsecret_proof (hex encoded HMAC-SHA256)
     */
    private String calculateAppSecretProof(String accessToken) {
        if (appSecret == null || appSecret.isBlank() || accessToken == null || accessToken.isBlank()) {
            log.warn("App secret or access token is missing, cannot calculate appsecret_proof");
            return null;
        }
        
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    appSecret.getBytes(StandardCharsets.UTF_8), 
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(accessToken.getBytes(StandardCharsets.UTF_8));
            // Convert byte array to hex string (giống Hex.encodeHexString)
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error calculating appsecret_proof: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Convert byte array to hex string
     * Tương đương với Hex.encodeHexString từ Apache Commons Codec
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Lấy Zalo User ID từ số điện thoại
     * Sử dụng API: GET /v2.0/oa/getprofile với data={"user_id": "phone_number"}
     * 
     * Format theo cURL:
     * curl -X GET -H 'access_token: <your_access_token>' \
     * 'https://openapi.zalo.me/v2.0/oa/getprofile?data={"user_id":"567826391599986760"}'
     * 
     * @param phoneNumber Số điện thoại (có thể là zalo_id hoặc số điện thoại)
     * @return Zalo User ID nếu tìm thấy, null nếu không tìm thấy hoặc lỗi
     */
    public String getUserIdByPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            log.warn("Phone number is empty, cannot get Zalo user ID");
            return null;
        }
        
        try {
            String accessToken = oauthService.getValidAccessToken();
            
            // Tính appsecret_proof
            String appSecretProof = calculateAppSecretProof(accessToken);
            
            // Tạo data JSON giống code mẫu: JSONObject data = JSONFactoryUtil.createJSONObject(); data.put("user_id", zalo_id);
            Map<String, String> dataMap = new HashMap<>();
            dataMap.put("user_id", phoneNumber);
            String dataJson = objectMapper.writeValueAsString(dataMap);
            String encoded = URLEncoder.encode(dataJson, StandardCharsets.UTF_8);

            URI uri = new URI(
                    "https://openapi.zalo.me/v2.0/oa/getprofile?data=" + encoded
            );
            var requestBuilder = webClient.get()
                    .uri(uri)
                    .header("access_token", accessToken);
            
            // Chỉ thêm appsecret_proof nếu có (không bắt buộc nhưng nên có để bảo mật)
            if (appSecretProof != null && !appSecretProof.isBlank()) {
                requestBuilder = requestBuilder.header("appsecret_proof", appSecretProof);
            }
            
            requestBuilder = requestBuilder.header("Content-Type", "application/x-www-form-urlencoded");

            String response = requestBuilder
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            if (response == null || response.isEmpty()) {
                log.warn("Empty response from Zalo getprofile API for phone: {}", phoneNumber);
                return null;
            }
            
            ZaloUserInfoResponse userInfo = objectMapper.readValue(response, ZaloUserInfoResponse.class);
            
            if (userInfo == null || userInfo.getError() != 0) {
                log.warn("Zalo API returned error for phone {}: error={}, message={}", 
                        phoneNumber, userInfo != null ? userInfo.getError() : "null", 
                        userInfo != null ? userInfo.getMessage() : "null");
                return null;
            }
            
            if (userInfo.getData() != null && userInfo.getData().getUserId() != null) {
                String userId = userInfo.getData().getUserId();
                log.info("Successfully retrieved Zalo user ID {} for phone {}", userId, phoneNumber);
                return userId;
            }
            
            log.warn("No user_id found in Zalo response for phone: {}", phoneNumber);
            return null;
            
        } catch (WebClientResponseException e) {
            log.error("Zalo API error when getting user ID for phone {}: HTTP {} - {}", 
                    phoneNumber, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return null;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Zalo user info response for phone {}: {}", phoneNumber, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error when getting Zalo user ID for phone {}: {}", phoneNumber, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Gửi thông báo đến Zalo User ID qua Zalo Official Account API
     * Tự động lấy access token hợp lệ từ OAuth service (tự refresh nếu hết hạn)
     * 
     * Lưu ý: Zalo OA API chỉ hỗ trợ gửi tin nhắn qua user_id, không hỗ trợ gửi trực tiếp qua số điện thoại.
     * User phải đã kết nối với Official Account để có user_id.
     * 
     * @param zaloUserId Zalo User ID của người nhận (bắt buộc)
     * @param message Nội dung tin nhắn
     * @return true nếu gửi thành công
     */
    public boolean sendNotification(String zaloUserId, String message) {
        if (zaloUserId == null || zaloUserId.trim().isEmpty()) {
            log.warn("Zalo User ID is empty, cannot send notification");
            return false;
        }
        
        if (message == null || message.trim().isEmpty()) {
            log.warn("Message is empty, cannot send notification");
            return false;
        }
        
        try {
            // Lấy access token hợp lệ (tự động refresh nếu hết hạn)
            String accessToken = oauthService.getValidAccessToken();
            String appSecretProof = calculateAppSecretProof(accessToken);
            
            // appsecret_proof không bắt buộc nhưng nên có để bảo mật
            if (appSecretProof == null) {
                log.warn("Cannot calculate appsecret_proof, sending notification without it");
            }

            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> recipient = new HashMap<>();
            recipient.put("user_id", zaloUserId);
            
            Map<String, Object> messageObj = new HashMap<>();
            messageObj.put("text", message);
            
            payload.put("recipient", recipient);
            payload.put("message", messageObj);
            
            var requestBuilder = webClient.post()
                    .uri("v2.0/oa/message")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("access_token", accessToken);
            
            // Chỉ thêm appsecret_proof nếu có (không bắt buộc nhưng nên có để bảo mật)
            if (appSecretProof != null && !appSecretProof.isBlank()) {
                requestBuilder = requestBuilder.header("appsecret_proof", appSecretProof);
            }
            
            String response = requestBuilder
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.isBlank()) {
                log.error("Zalo notification failed: empty response for userId={}", zaloUserId);
                return false;
            }

            try {
                var node = objectMapper.readTree(response);
                int error = node.has("error") ? node.get("error").asInt(-1) : -1;
                String messageText = node.has("message") ? node.get("message").asText() : null;

                if (error != 0) {
                    log.error(
                            "Zalo notification failed for userId={} - error={}, message={}, rawResponse={}",
                            zaloUserId, error, messageText, response
                    );
                    return false;
                }

                log.info("Zalo notification sent successfully for userId={}. Response: {}", zaloUserId, response);
                return true;
            } catch (JsonProcessingException e) {
                log.error(
                        "Zalo notification response parsing error for userId={} - rawResponse={}",
                        zaloUserId, response, e
                );
                return false;
            }
        } catch (Exception e) {
            log.error("Error sending Zalo notification to userId={}: {}", zaloUserId, e.getMessage(), e);
            return false;
        }
    }
}

