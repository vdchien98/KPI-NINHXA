package vn.gov.bacninh.ninhxareport.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.gov.bacninh.ninhxareport.dto.*;
import vn.gov.bacninh.ninhxareport.service.DepartmentService;
import vn.gov.bacninh.ninhxareport.service.OrganizationService;
import vn.gov.bacninh.ninhxareport.service.UserService;

import java.util.List;

/**
 * Controller for common data that can be accessed by any authenticated user
 */
@RestController
@RequestMapping("/common")
public class CommonDataController {
    
    @Autowired
    private OrganizationService organizationService;
    
    @Autowired
    private DepartmentService departmentService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/organizations")
    public ResponseEntity<ApiResponse<List<OrganizationDTO>>> getActiveOrganizations() {
        List<OrganizationDTO> organizations = organizationService.getActiveOrganizations();
        return ResponseEntity.ok(ApiResponse.success(organizations));
    }
    
    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<DepartmentDTO>>> getAllDepartments() {
        List<DepartmentDTO> departments = departmentService.getAllDepartments();
        return ResponseEntity.ok(ApiResponse.success(departments));
    }
    
    @GetMapping("/departments/by-organization/{organizationId}")
    public ResponseEntity<ApiResponse<List<DepartmentDTO>>> getDepartmentsByOrganization(
            @PathVariable Long organizationId) {
        List<DepartmentDTO> departments = departmentService.getDepartmentsByOrganization(organizationId);
        return ResponseEntity.ok(ApiResponse.success(departments));
    }
    
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }
    
    @GetMapping("/users/by-department/{departmentId}")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getUsersByDepartment(
            @PathVariable Long departmentId) {
        List<UserDTO> users = userService.getUsersByDepartment(departmentId);
        return ResponseEntity.ok(ApiResponse.success(users));
    }
    
    @GetMapping("/users/by-organization/{organizationId}")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getUsersByOrganization(
            @PathVariable Long organizationId) {
        List<UserDTO> users = userService.getUsersByOrganization(organizationId);
        return ResponseEntity.ok(ApiResponse.success(users));
    }
}

