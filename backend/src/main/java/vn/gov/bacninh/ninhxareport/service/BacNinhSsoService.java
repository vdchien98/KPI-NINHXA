package vn.gov.bacninh.ninhxareport.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import vn.gov.bacninh.ninhxareport.config.BacNinhSsoProperties;
import vn.gov.bacninh.ninhxareport.dto.BacNinhSsoAuthenticationResult;
import vn.gov.bacninh.ninhxareport.dto.BacNinhSsoUserInfo;
import vn.gov.bacninh.ninhxareport.dto.BacNinhTokenResponse;

import java.util.Base64;

@Service
public class BacNinhSsoService {
    
    private static final Logger logger = LoggerFactory.getLogger(BacNinhSsoService.class);
    
    @Autowired
    private BacNinhSsoProperties ssoProperties;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public BacNinhSsoService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    public BacNinhSsoAuthenticationResult authenticate(String email, String password) {
        validateConfig();
        BacNinhTokenResponse tokenResponse = requestToken(email, password);
        BacNinhSsoUserInfo userInfo = decodeAccessToken(tokenResponse.getAccessToken());
        return new BacNinhSsoAuthenticationResult(tokenResponse, userInfo);
    }
    
    private void validateConfig() {
        if (!StringUtils.hasText(ssoProperties.getTokenUrl())) {
            throw new IllegalStateException("SSO token URL is not configured. Please set bacninh.sso.token-url in application.yml");
        }
        if (!StringUtils.hasText(ssoProperties.getClientId())) {
            throw new IllegalStateException("SSO client ID is not configured. Please set bacninh.sso.client-id in application.yml");
        }
        if (!StringUtils.hasText(ssoProperties.getClientSecret())) {
            throw new IllegalStateException("SSO client secret is not configured. Please set bacninh.sso.client-secret in application.yml");
        }
    }
    
    private BacNinhTokenResponse requestToken(String email, String password) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            // Add Basic Authorization if configured
            if (StringUtils.hasText(ssoProperties.getBasicAuthorization())) {
                headers.set("Authorization", ssoProperties.getBasicAuthorization());
            } else {
                // Build Basic Auth from client_id:client_secret
                String credentials = ssoProperties.getClientId() + ":" + ssoProperties.getClientSecret();
                String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
                headers.set("Authorization", "Basic " + encodedCredentials);
            }
            
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", ssoProperties.getClientId());
            body.add("client_secret", ssoProperties.getClientSecret());
            body.add("grant_type", "password");
            body.add("username", email);
            body.add("password", password);
            body.add("scope", "openid email profile");
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                ssoProperties.getTokenUrl(),
                request,
                String.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.error("SSO token request failed with status: {}", response.getStatusCode());
                throw new RuntimeException("SSO authentication failed");
            }
            
            BacNinhTokenResponse tokenResponse = objectMapper.readValue(
                response.getBody(),
                BacNinhTokenResponse.class
            );
            
            if (tokenResponse.getAccessToken() == null) {
                throw new RuntimeException("SSO token response missing access_token");
            }
            
            logger.info("SSO token obtained successfully for user: {}", email);
            return tokenResponse;
            
        } catch (Exception e) {
            logger.error("Error requesting SSO token for user: {}", email, e);
            throw new RuntimeException("SSO authentication failed: " + e.getMessage(), e);
        }
    }
    
    private BacNinhSsoUserInfo decodeAccessToken(String accessToken) {
        try {
            // JWT token có 3 phần: header.payload.signature
            String[] parts = accessToken.split("\\.");
            if (parts.length != 3) {
                throw new RuntimeException("Invalid JWT token format");
            }
            
            // Decode payload (phần thứ 2)
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode jsonNode = objectMapper.readTree(payload);
            
            BacNinhSsoUserInfo userInfo = BacNinhSsoUserInfo.builder()
                    .email(jsonNode.has("email") ? jsonNode.get("email").asText() : null)
                    .emailVerified(jsonNode.has("email_verified") && jsonNode.get("email_verified").asBoolean())
                    .preferredUsername(jsonNode.has("preferred_username") ? jsonNode.get("preferred_username").asText() : null)
                    .fullName(jsonNode.has("name") ? jsonNode.get("name").asText() : null)
                    .givenName(jsonNode.has("given_name") ? jsonNode.get("given_name").asText() : null)
                    .familyName(jsonNode.has("family_name") ? jsonNode.get("family_name").asText() : null)
                    .subject(jsonNode.has("sub") ? jsonNode.get("sub").asText() : null)
                    .build();
            
            return userInfo;
            
        } catch (Exception e) {
            logger.error("Error decoding SSO access token", e);
            throw new RuntimeException("Failed to decode SSO user info: " + e.getMessage(), e);
        }
    }
}
