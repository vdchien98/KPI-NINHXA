package vn.gov.bacninh.ninhxareport.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.gov.bacninh.ninhxareport.dto.ApiResponse;
import vn.gov.bacninh.ninhxareport.dto.CreateOrganizationRequest;
import vn.gov.bacninh.ninhxareport.dto.OrganizationDTO;
import vn.gov.bacninh.ninhxareport.service.OrganizationService;

import java.util.List;

@RestController
@RequestMapping("/admin/organizations")
@PreAuthorize("hasRole('ADMIN')")
public class OrganizationController {
    
    @Autowired
    private OrganizationService organizationService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizationDTO>>> getAllOrganizations() {
        List<OrganizationDTO> organizations = organizationService.getAllOrganizations();
        return ResponseEntity.ok(ApiResponse.success(organizations));
    }
    
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<OrganizationDTO>>> getActiveOrganizations() {
        List<OrganizationDTO> organizations = organizationService.getActiveOrganizations();
        return ResponseEntity.ok(ApiResponse.success(organizations));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationDTO>> getOrganizationById(@PathVariable Long id) {
        try {
            OrganizationDTO organization = organizationService.getOrganizationById(id);
            return ResponseEntity.ok(ApiResponse.success(organization));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationDTO>> createOrganization(
            @Valid @RequestBody CreateOrganizationRequest request) {
        try {
            OrganizationDTO organization = organizationService.createOrganization(request);
            return ResponseEntity.ok(ApiResponse.success("Tạo cơ quan thành công", organization));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationDTO>> updateOrganization(
            @PathVariable Long id, 
            @Valid @RequestBody CreateOrganizationRequest request) {
        try {
            OrganizationDTO organization = organizationService.updateOrganization(id, request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật cơ quan thành công", organization));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(@PathVariable Long id) {
        try {
            organizationService.deleteOrganization(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa cơ quan thành công", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

