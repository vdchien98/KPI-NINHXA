package vn.gov.bacninh.ninhxareport.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminUpdateReportResponseDTO {
    private String note;
    
    private Double score;
    
    private String comment;
    
    private Double selfScore;
    
    private List<ItemUpdateDTO> items;
    
    @Data
    public static class ItemUpdateDTO {
        private String title;
        private String content;
        private Integer progress;
        private String difficulties;
    }
}

