package vn.gov.bacninh.ninhxareport.dto;

public record BacNinhSsoAuthenticationResult(
        BacNinhTokenResponse tokenResponse,
        BacNinhSsoUserInfo userInfo
) {
}


