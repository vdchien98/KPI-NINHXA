package vn.gov.bacninh.ninhxareport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.gov.bacninh.ninhxareport.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.organizations LEFT JOIN FETCH u.department LEFT JOIN FETCH u.role LEFT JOIN FETCH u.position")
    List<User> findAllWithRelations();
    
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.organizations LEFT JOIN FETCH u.department LEFT JOIN FETCH u.role LEFT JOIN FETCH u.position WHERE u.id = :id")
    Optional<User> findByIdWithRelations(@Param("id") Long id);
    
    @Query("SELECT u FROM User u JOIN u.organizations o WHERE o.id = :organizationId")
    List<User> findByOrganizationId(@Param("organizationId") Long organizationId);
    
    List<User> findByDepartmentId(Long departmentId);
    
    List<User> findByRoleId(Long roleId);
    
    @Query("SELECT u FROM User u WHERE u.isActive = true")
    List<User> findAllActiveUsers();
}

