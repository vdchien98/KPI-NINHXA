package vn.gov.bacninh.ninhxareport.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gov.bacninh.ninhxareport.dto.CreateDepartmentRequest;
import vn.gov.bacninh.ninhxareport.dto.DepartmentDTO;
import vn.gov.bacninh.ninhxareport.entity.Department;
import vn.gov.bacninh.ninhxareport.entity.Organization;
import vn.gov.bacninh.ninhxareport.repository.DepartmentRepository;
import vn.gov.bacninh.ninhxareport.repository.OrganizationRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentService {
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(DepartmentDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<DepartmentDTO> getDepartmentsByOrganization(Long organizationId) {
        return departmentRepository.findByOrganizationId(organizationId).stream()
                .map(DepartmentDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    public DepartmentDTO getDepartmentById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng ban với id: " + id));
        return DepartmentDTO.fromEntity(dept);
    }
    
    @Transactional
    public DepartmentDTO createDepartment(CreateDepartmentRequest request) {
        Organization org = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ quan"));
        
        if (request.getCode() != null && 
            departmentRepository.existsByCodeAndOrganizationId(request.getCode(), request.getOrganizationId())) {
            throw new RuntimeException("Mã phòng ban đã tồn tại trong cơ quan này");
        }
        
        Department dept = Department.builder()
                .name(request.getName())
                .code(request.getCode())
                .organization(org)
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        
        dept = departmentRepository.save(dept);
        return DepartmentDTO.fromEntity(dept);
    }
    
    @Transactional
    public DepartmentDTO updateDepartment(Long id, CreateDepartmentRequest request) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng ban với id: " + id));
        
        if (request.getOrganizationId() != null && 
            !request.getOrganizationId().equals(dept.getOrganization().getId())) {
            Organization org = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ quan"));
            dept.setOrganization(org);
        }
        
        if (request.getCode() != null && !request.getCode().equals(dept.getCode())) {
            if (departmentRepository.existsByCodeAndOrganizationId(
                    request.getCode(), dept.getOrganization().getId())) {
                throw new RuntimeException("Mã phòng ban đã tồn tại trong cơ quan này");
            }
            dept.setCode(request.getCode());
        }
        
        dept.setName(request.getName());
        dept.setDescription(request.getDescription());
        
        if (request.getIsActive() != null) {
            dept.setIsActive(request.getIsActive());
        }
        
        dept = departmentRepository.save(dept);
        return DepartmentDTO.fromEntity(dept);
    }
    
    @Transactional
    public void deleteDepartment(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng ban với id: " + id));
        
        departmentRepository.delete(dept);
    }
}

