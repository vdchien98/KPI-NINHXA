package vn.gov.bacninh.ninhxareport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.gov.bacninh.ninhxareport.entity.Role;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    Optional<Role> findByName(String name);
    
    @Query("SELECT r FROM Role r WHERE r.parent IS NULL")
    List<Role> findAllRootRoles();
    
    List<Role> findByParentId(Long parentId);
    
    boolean existsByName(String name);
}

