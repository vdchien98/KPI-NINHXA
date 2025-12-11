package vn.gov.bacninh.ninhxareport.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gov.bacninh.ninhxareport.dto.CreatePositionRequest;
import vn.gov.bacninh.ninhxareport.dto.PositionDTO;
import vn.gov.bacninh.ninhxareport.entity.Position;
import vn.gov.bacninh.ninhxareport.repository.PositionRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PositionService {
    
    @Autowired
    private PositionRepository positionRepository;
    
    public List<PositionDTO> getAllPositions() {
        return positionRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(PositionDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<PositionDTO> getActivePositions() {
        return positionRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(PositionDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    public PositionDTO getPositionById(Long id) {
        Position position = positionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chức vụ với id: " + id));
        return PositionDTO.fromEntity(position);
    }
    
    @Transactional
    public PositionDTO createPosition(CreatePositionRequest request) {
        if (request.getCode() != null && !request.getCode().isEmpty() && 
            positionRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Mã chức vụ đã tồn tại");
        }
        
        Position position = Position.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        
        position = positionRepository.save(position);
        return PositionDTO.fromEntity(position);
    }
    
    @Transactional
    public PositionDTO updatePosition(Long id, CreatePositionRequest request) {
        Position position = positionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chức vụ với id: " + id));
        
        if (request.getCode() != null && !request.getCode().isEmpty() && 
            !request.getCode().equals(position.getCode())) {
            if (positionRepository.existsByCodeAndIdNot(request.getCode(), id)) {
                throw new RuntimeException("Mã chức vụ đã tồn tại");
            }
            position.setCode(request.getCode());
        }
        
        position.setName(request.getName());
        position.setDescription(request.getDescription());
        
        if (request.getDisplayOrder() != null) {
            position.setDisplayOrder(request.getDisplayOrder());
        }
        
        if (request.getIsActive() != null) {
            position.setIsActive(request.getIsActive());
        }
        
        position = positionRepository.save(position);
        return PositionDTO.fromEntity(position);
    }
    
    @Transactional
    public void deletePosition(Long id) {
        Position position = positionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chức vụ với id: " + id));
        
        positionRepository.delete(position);
    }
}

