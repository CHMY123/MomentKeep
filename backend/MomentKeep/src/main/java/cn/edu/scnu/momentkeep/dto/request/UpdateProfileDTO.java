package cn.edu.scnu.momentkeep.dto.request;

import lombok.Data;

@Data
public class UpdateProfileDTO {
    
    private String nickname;
    
    private String email;
    
    private String phone;
    
    private String avatar;
}
