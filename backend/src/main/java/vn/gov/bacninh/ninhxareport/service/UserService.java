package vn.gov.bacninh.ninhxareport.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gov.bacninh.ninhxareport.dto.CreateUserRequest;
import vn.gov.bacninh.ninhxareport.dto.UpdateUserRequest;
import vn.gov.bacninh.ninhxareport.dto.UserDTO;
import vn.gov.bacninh.ninhxareport.entity.Department;
import vn.gov.bacninh.ninhxareport.entity.Organization;
import vn.gov.bacninh.ninhxareport.entity.Position;
import vn.gov.bacninh.ninhxareport.entity.Role;
import vn.gov.bacninh.ninhxareport.entity.User;
import vn.gov.bacninh.ninhxareport.repository.DepartmentRepository;
import vn.gov.bacninh.ninhxareport.repository.OrganizationRepository;
import vn.gov.bacninh.ninhxareport.repository.PositionRepository;
import vn.gov.bacninh.ninhxareport.repository.RoleRepository;
import vn.gov.bacninh.ninhxareport.repository.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private PositionRepository positionRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public List<UserDTO> getAllUsers() {
        // Sử dụng findAllWithRelations để load organizations và department
        return userRepository.findAllWithRelations().stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<UserDTO> getUsersByDepartment(Long departmentId) {
        return userRepository.findByDepartmentId(departmentId).stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<UserDTO> getUsersByOrganization(Long organizationId) {
        return userRepository.findByOrganizationId(organizationId).stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    public UserDTO getUserById(Long id) {
        User user = userRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với id: " + id));
        return UserDTO.fromEntity(user);
    }
    
    @Transactional
    public UserDTO createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }
        
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .avatar(request.getAvatar())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        
        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy role"));
            user.setRole(role);
        }
        
        if (request.getOrganizationIds() != null && !request.getOrganizationIds().isEmpty()) {
            Set<Organization> orgs = new HashSet<>();
            for (Long orgId : request.getOrganizationIds()) {
                Organization org = organizationRepository.findById(orgId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ quan với id: " + orgId));
                orgs.add(org);
            }
            user.setOrganizations(orgs);
        }
        
        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng ban"));
            user.setDepartment(dept);
        }
        
        if (request.getPositionId() != null) {
            Position position = positionRepository.findById(request.getPositionId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chức vụ"));
            user.setPosition(position);
        }
        
        // Set representativeType
        user.setRepresentativeType(request.getRepresentativeType());
        
        user = userRepository.save(user);
        // Reload user với relations để đảm bảo organizations được load
        user = userRepository.findByIdWithRelations(user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng sau khi tạo"));
        return UserDTO.fromEntity(user);
    }
    
    @Transactional
    public UserDTO updateUser(Long id, UpdateUserRequest request) {
        // Load user với organizations và department để có thể update đúng cách
        User user = userRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với id: " + id));
        
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email đã được sử dụng");
            }
            user.setEmail(request.getEmail());
        }
        
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }
        
        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy role"));
            user.setRole(role);
        }
        
        // Xử lý organizations: nếu request có organizationIds (kể cả empty list), cập nhật
        if (request.getOrganizationIds() != null) {
            // Tạo Set mới thay vì clear và add để đảm bảo JPA sync đúng
            Set<Organization> newOrganizations = new HashSet<>();
            if (!request.getOrganizationIds().isEmpty()) {
                for (Long orgId : request.getOrganizationIds()) {
                    Organization org = organizationRepository.findById(orgId)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ quan với id: " + orgId));
                    newOrganizations.add(org);
                }
            }
            user.setOrganizations(newOrganizations);
        }
        
        // Xử lý department: nếu request có departmentId, cập nhật; nếu null, xóa
        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng ban"));
            user.setDepartment(dept);
        } else if (request.getDepartmentId() == null && request.getOrganizationIds() != null) {
            // Nếu departmentId là null trong request, xóa department
            user.setDepartment(null);
        }
        
        if (request.getPositionId() != null) {
            Position position = positionRepository.findById(request.getPositionId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chức vụ"));
            user.setPosition(position);
        }
        
        // Set representativeType
        if (request.getRepresentativeType() != null) {
            user.setRepresentativeType(request.getRepresentativeType());
        } else {
            user.setRepresentativeType(null);
        }
        
        user = userRepository.save(user);
        // Flush để đảm bảo dữ liệu được lưu vào database
        userRepository.flush();
        // Reload user với relations để đảm bảo organizations được load
        user = userRepository.findByIdWithRelations(user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng sau khi cập nhật"));
        return UserDTO.fromEntity(user);
    }
    
    @Transactional
    public void deleteUser(Long id) {
        // Không cho phép xóa admin (id = 1)
        if (id == 1L) {
            throw new RuntimeException("Không thể xóa tài khoản admin hệ thống");
        }
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với id: " + id));
        
        userRepository.delete(user);
    }
}

