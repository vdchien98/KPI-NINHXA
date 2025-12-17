package vn.gov.bacninh.ninhxareport.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BacNinhSsoUserInfo {
    String email;
    Boolean emailVerified;
    String preferredUsername;
    String fullName;
    String givenName;
    String familyName;
    String subject;
}


