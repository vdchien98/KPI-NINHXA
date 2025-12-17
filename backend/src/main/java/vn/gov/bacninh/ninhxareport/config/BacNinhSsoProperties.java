package vn.gov.bacninh.ninhxareport.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bacninh.sso")
public class BacNinhSsoProperties {
    /**
     * Full token endpoint URL, e.g. https://.../protocol/openid-connect/token
     */
    private String tokenUrl;
    /**
     * OAuth2 client id (form parameter)
     */
    private String clientId;
    /**
     * OAuth2 client secret (form parameter)
     */
    private String clientSecret;
    /**
     * Authorization header value (e.g. Basic base64(...)) required by Keycloak realm.
     */
    private String basicAuthorization;
}


