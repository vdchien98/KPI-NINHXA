package vn.gov.bacninh.ninhxareport.controller;

import vn.gov.bacninh.ninhxareport.dto.ApiResponse;
import vn.gov.bacninh.ninhxareport.dto.zalo.ZaloTokenInfoDTO;
import vn.gov.bacninh.ninhxareport.service.ZaloOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/zalo/oauth")
@RequiredArgsConstructor
public class ZaloOAuthController {

    private final ZaloOAuthService oauthService;

    /**
     * Khởi tạo token lần đầu bằng refresh token
     * Endpoint này dùng để set refresh token ban đầu (có thể lấy từ OAuth flow hoặc manual)
     * 
     * Body: { "refreshToken": "..." }
     */
    @PostMapping("/init")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ZaloTokenInfoDTO>> initializeToken(
            @RequestBody InitTokenRequest request
    ) {
        oauthService.initializeToken(request.refreshToken);
        ZaloTokenInfoDTO tokenInfo = oauthService.getTokenInfo();
        return ResponseEntity.ok(ApiResponse.success("Khởi tạo token Zalo thành công", tokenInfo));
    }

    /**
     * Lấy thông tin token chi tiết (không bao gồm access_token và refresh_token vì bảo mật)
     */
    @GetMapping("/info")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ZaloTokenInfoDTO>> getTokenInfo() {
        ZaloTokenInfoDTO tokenInfo = oauthService.getTokenInfo();
        if (tokenInfo == null) {
            return ResponseEntity.ok(ApiResponse.success("Token chưa được khởi tạo", null));
        }
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin token thành công", tokenInfo));
    }

    /**
     * Kiểm tra trạng thái token (deprecated, use /info instead)
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> getTokenStatus() {
        boolean initialized = oauthService.isTokenInitialized();
        return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái token thành công", initialized));
    }

    /**
     * Refresh token manually (nếu cần)
     */
    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ZaloTokenInfoDTO>> refreshToken() {
        oauthService.getValidAccessToken(); // This will auto-refresh if needed
        ZaloTokenInfoDTO tokenInfo = oauthService.getTokenInfo();
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", tokenInfo));
    }

    // Inner class for request body
    private static class InitTokenRequest {
        public String refreshToken;
    }
}

