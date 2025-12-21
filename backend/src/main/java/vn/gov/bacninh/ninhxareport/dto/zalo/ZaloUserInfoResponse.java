package vn.gov.bacninh.ninhxareport.dto.zalo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ZaloUserInfoResponse {
    private Integer error;
    private String message;
    private ZaloUserInfoData data;

    @Data
    public static class ZaloUserInfoData {
        @JsonProperty("user_id")
        private String userId; // Zalo trả về string, không phải số
        
        @JsonProperty("user_id_by_app")
        private String userIdByApp;
        
        private String avatar;
        
        private Map<String, String> avatars; // {"120": String, "240": String}
        
        @JsonProperty("display_name")
        private String displayName;
        
        @JsonProperty("user_gender")
        private Integer userGender;
        
        @JsonProperty("is_sensitive")
        private Boolean isSensitive;
        
        @JsonProperty("birth_date")
        private Long birthDate;
        
        @JsonProperty("tags_and_notes_info")
        private TagsAndNotesInfo tagsAndNotesInfo;
    }
    
    @Data
    public static class TagsAndNotesInfo {
        @JsonProperty("tag_names")
        private List<String> tagNames;
        
        private List<String> notes;
    }
}

