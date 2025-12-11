package vn.gov.bacninh.ninhxareport.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.gov.bacninh.ninhxareport.dto.ApiResponse;
import vn.gov.bacninh.ninhxareport.dto.CreateRoleRequest;
import vn.gov.bacninh.ninhxareport.dto.RoleDTO;
import vn.gov.bacninh.ninhxareport.service.RoleService;

import java.util.List;

@RestController
@RequestMapping("/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {
    
    @Autowired
    private RoleService roleService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDTO>>> getAllRoles() {
        List<RoleDTO> roles = roleService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }
    
    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<RoleDTO>>> getRoleTree() {
        List<RoleDTO> roles = roleService.getRoleTree();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDTO>> getRoleById(@PathVariable Long id) {
        try {
            RoleDTO role = roleService.getRoleById(id);
            return ResponseEntity.ok(ApiResponse.success(role));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<RoleDTO>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        try {
            RoleDTO role = roleService.createRole(request);
            return ResponseEntity.ok(ApiResponse.success("Tạo role thành công", role));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDTO>> updateRole(
            @PathVariable Long id, 
            @Valid @RequestBody CreateRoleRequest request) {
        try {
            RoleDTO role = roleService.updateRole(id, request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật role thành công", role));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa role thành công", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

