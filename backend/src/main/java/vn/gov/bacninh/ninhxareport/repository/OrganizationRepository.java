package vn.gov.bacninh.ninhxareport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.gov.bacninh.ninhxareport.entity.Organization;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    
    Optional<Organization> findByCode(String code);
    
    List<Organization> findByIsActiveTrue();
    
    boolean existsByCode(String code);
}

