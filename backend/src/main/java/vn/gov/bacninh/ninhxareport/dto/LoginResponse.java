package vn.gov.bacninh.ninhxareport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String type;
    private UserDTO user;
    
    public static LoginResponse of(String token, UserDTO user) {
        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .user(user)
                .build();
    }
}

