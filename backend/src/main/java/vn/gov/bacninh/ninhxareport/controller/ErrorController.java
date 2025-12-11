package vn.gov.bacninh.ninhxareport.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.gov.bacninh.ninhxareport.dto.ApiResponse;

@RestController
@RequestMapping("/error")
public class ErrorController {
    
    @GetMapping
    public ResponseEntity<ApiResponse<String>> handleError() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("Vui lòng sử dụng frontend tại http://localhost:3001 để truy cập hệ thống"));
    }
    
    @GetMapping("/auth/login")
    public ResponseEntity<ApiResponse<String>> handleLoginError() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("Endpoint này chỉ chấp nhận POST request. Vui lòng sử dụng frontend để đăng nhập."));
    }
}

