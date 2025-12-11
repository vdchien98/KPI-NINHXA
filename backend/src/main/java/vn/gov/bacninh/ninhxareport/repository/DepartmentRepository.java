package vn.gov.bacninh.ninhxareport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.gov.bacninh.ninhxareport.entity.Department;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
    List<Department> findByOrganizationId(Long organizationId);
    
    List<Department> findByOrganizationIdAndIsActiveTrue(Long organizationId);
    
    Optional<Department> findByCodeAndOrganizationId(String code, Long organizationId);
    
    boolean existsByCodeAndOrganizationId(String code, Long organizationId);
}

