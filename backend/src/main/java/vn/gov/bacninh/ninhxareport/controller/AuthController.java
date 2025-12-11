package vn.gov.bacninh.ninhxareport.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.gov.bacninh.ninhxareport.dto.ApiResponse;
import vn.gov.bacninh.ninhxareport.dto.LoginRequest;
import vn.gov.bacninh.ninhxareport.dto.LoginResponse;
import vn.gov.bacninh.ninhxareport.dto.UserDTO;
import vn.gov.bacninh.ninhxareport.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @GetMapping("/login")
    public ResponseEntity<ApiResponse<String>> loginGet() {
        return ResponseEntity.status(org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("Vui lòng sử dụng POST method để đăng nhập. Truy cập http://localhost:3001/login để sử dụng giao diện."));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email hoặc mật khẩu không đúng"));
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser() {
        try {
            UserDTO user = authService.getCurrentUser();
            return ResponseEntity.ok(ApiResponse.success(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

