package vn.gov.bacninh.ninhxareport.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gov.bacninh.ninhxareport.dto.CreateOrganizationRequest;
import vn.gov.bacninh.ninhxareport.dto.OrganizationDTO;
import vn.gov.bacninh.ninhxareport.entity.Organization;
import vn.gov.bacninh.ninhxareport.repository.OrganizationRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrganizationService {
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    public List<OrganizationDTO> getAllOrganizations() {
        return organizationRepository.findAll().stream()
                .map(OrganizationDTO::fromEntityWithDepartments)
                .collect(Collectors.toList());
    }
    
    public List<OrganizationDTO> getActiveOrganizations() {
        return organizationRepository.findByIsActiveTrue().stream()
                .map(OrganizationDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    public OrganizationDTO getOrganizationById(Long id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ quan với id: " + id));
        return OrganizationDTO.fromEntityWithDepartments(org);
    }
    
    @Transactional
    public OrganizationDTO createOrganization(CreateOrganizationRequest request) {
        if (request.getCode() != null && organizationRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Mã cơ quan đã tồn tại");
        }
        
        Organization org = Organization.builder()
                .name(request.getName())
                .code(request.getCode())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        
        org = organizationRepository.save(org);
        return OrganizationDTO.fromEntity(org);
    }
    
    @Transactional
    public OrganizationDTO updateOrganization(Long id, CreateOrganizationRequest request) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ quan với id: " + id));
        
        if (request.getCode() != null && !request.getCode().equals(org.getCode())) {
            if (organizationRepository.existsByCode(request.getCode())) {
                throw new RuntimeException("Mã cơ quan đã tồn tại");
            }
            org.setCode(request.getCode());
        }
        
        org.setName(request.getName());
        org.setAddress(request.getAddress());
        org.setPhone(request.getPhone());
        org.setEmail(request.getEmail());
        
        if (request.getIsActive() != null) {
            org.setIsActive(request.getIsActive());
        }
        
        org = organizationRepository.save(org);
        return OrganizationDTO.fromEntity(org);
    }
    
    @Transactional
    public void deleteOrganization(Long id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ quan với id: " + id));
        
        organizationRepository.delete(org);
    }
}

