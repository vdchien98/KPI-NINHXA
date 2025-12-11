package vn.gov.bacninh.ninhxareport.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.gov.bacninh.ninhxareport.dto.ApiResponse;
import vn.gov.bacninh.ninhxareport.dto.CreateDepartmentRequest;
import vn.gov.bacninh.ninhxareport.dto.DepartmentDTO;
import vn.gov.bacninh.ninhxareport.service.DepartmentService;

import java.util.List;

@RestController
@RequestMapping("/admin/departments")
@PreAuthorize("hasRole('ADMIN')")
public class DepartmentController {
    
    @Autowired
    private DepartmentService departmentService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentDTO>>> getAllDepartments() {
        List<DepartmentDTO> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(ApiResponse.success(departments));
    }
    
    @GetMapping("/by-organization/{organizationId}")
    public ResponseEntity<ApiResponse<List<DepartmentDTO>>> getDepartmentsByOrganization(
            @PathVariable Long organizationId) {
        List<DepartmentDTO> departments = departmentService.getDepartmentsByOrganization(organizationId);
        return ResponseEntity.ok(ApiResponse.success(departments));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentDTO>> getDepartmentById(@PathVariable Long id) {
        try {
            DepartmentDTO department = departmentService.getDepartmentById(id);
            return ResponseEntity.ok(ApiResponse.success(department));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentDTO>> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request) {
        try {
            DepartmentDTO department = departmentService.createDepartment(request);
            return ResponseEntity.ok(ApiResponse.success("Tạo phòng ban thành công", department));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentDTO>> updateDepartment(
            @PathVariable Long id, 
            @Valid @RequestBody CreateDepartmentRequest request) {
        try {
            DepartmentDTO department = departmentService.updateDepartment(id, request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật phòng ban thành công", department));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable Long id) {
        try {
            departmentService.deleteDepartment(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa phòng ban thành công", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

