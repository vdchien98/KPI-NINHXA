package vn.gov.bacninh.ninhxareport.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.gov.bacninh.ninhxareport.dto.ApiResponse;
import vn.gov.bacninh.ninhxareport.dto.CreatePositionRequest;
import vn.gov.bacninh.ninhxareport.dto.PositionDTO;
import vn.gov.bacninh.ninhxareport.service.PositionService;

import java.util.List;

@RestController
@RequestMapping("/admin/positions")
@PreAuthorize("hasRole('ADMIN')")
public class PositionController {
    
    @Autowired
    private PositionService positionService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<PositionDTO>>> getAllPositions() {
        List<PositionDTO> positions = positionService.getAllPositions();
        return ResponseEntity.ok(ApiResponse.success(positions));
    }
    
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<PositionDTO>>> getActivePositions() {
        List<PositionDTO> positions = positionService.getActivePositions();
        return ResponseEntity.ok(ApiResponse.success(positions));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PositionDTO>> getPositionById(@PathVariable Long id) {
        try {
            PositionDTO position = positionService.getPositionById(id);
            return ResponseEntity.ok(ApiResponse.success(position));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<PositionDTO>> createPosition(
            @Valid @RequestBody CreatePositionRequest request) {
        try {
            PositionDTO position = positionService.createPosition(request);
            return ResponseEntity.ok(ApiResponse.success("Tạo chức vụ thành công", position));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PositionDTO>> updatePosition(
            @PathVariable Long id, 
            @Valid @RequestBody CreatePositionRequest request) {
        try {
            PositionDTO position = positionService.updatePosition(id, request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật chức vụ thành công", position));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePosition(@PathVariable Long id) {
        try {
            positionService.deletePosition(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa chức vụ thành công", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

