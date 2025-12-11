package vn.gov.bacninh.ninhxareport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.gov.bacninh.ninhxareport.entity.Position;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    
    List<Position> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    List<Position> findAllByOrderByDisplayOrderAsc();
    
    Optional<Position> findByCode(String code);
    
    boolean existsByCode(String code);
    
    boolean existsByCodeAndIdNot(String code, Long id);
}

