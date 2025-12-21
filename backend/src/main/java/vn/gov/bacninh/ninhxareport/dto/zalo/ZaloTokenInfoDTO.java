package vn.gov.bacninh.ninhxareport.dto.zalo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZaloTokenInfoDTO {
    private Long id;
    private String tokenType;
    private String scope;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean isExpired;
    private boolean isExpiringSoon; // Expires within 1 hour
    private String expiresIn; // Human readable (e.g., "2 hours", "30 minutes")
}

