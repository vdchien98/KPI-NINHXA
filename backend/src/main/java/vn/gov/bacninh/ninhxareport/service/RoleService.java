package vn.gov.bacninh.ninhxareport.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gov.bacninh.ninhxareport.dto.CreateRoleRequest;
import vn.gov.bacninh.ninhxareport.dto.RoleDTO;
import vn.gov.bacninh.ninhxareport.entity.Role;
import vn.gov.bacninh.ninhxareport.repository.RoleRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleService {
    
    @Autowired
    private RoleRepository roleRepository;
    
    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(RoleDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<RoleDTO> getRoleTree() {
        List<Role> rootRoles = roleRepository.findAllRootRoles();
        return rootRoles.stream()
                .map(RoleDTO::fromEntityWithChildren)
                .collect(Collectors.toList());
    }
    
    public RoleDTO getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role với id: " + id));
        return RoleDTO.fromEntityWithChildren(role);
    }
    
    @Transactional
    public RoleDTO createRole(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new RuntimeException("Role với tên này đã tồn tại");
        }
        
        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        
        if (request.getParentId() != null) {
            Role parent = roleRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy role cha"));
            role.setParent(parent);
            role.setLevel(parent.getLevel() + 1);
        }
        
        role = roleRepository.save(role);
        return RoleDTO.fromEntity(role);
    }
    
    @Transactional
    public RoleDTO updateRole(Long id, CreateRoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role với id: " + id));
        
        // Không cho phép sửa role Admin (id = 1)
        if (id == 1L) {
            throw new RuntimeException("Không thể sửa role Admin hệ thống");
        }
        
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        
        if (request.getParentId() != null) {
            // Kiểm tra không cho phép set parent là chính nó hoặc con của nó
            if (request.getParentId().equals(id)) {
                throw new RuntimeException("Không thể set role cha là chính nó");
            }
            
            Role parent = roleRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy role cha"));
            role.setParent(parent);
            role.setLevel(parent.getLevel() + 1);
        } else {
            role.setParent(null);
            role.setLevel(0);
        }
        
        role = roleRepository.save(role);
        return RoleDTO.fromEntity(role);
    }
    
    @Transactional
    public void deleteRole(Long id) {
        // Không cho phép xóa role Admin (id = 1)
        if (id == 1L) {
            throw new RuntimeException("Không thể xóa role Admin hệ thống");
        }
        
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role với id: " + id));
        
        roleRepository.delete(role);
    }
}

