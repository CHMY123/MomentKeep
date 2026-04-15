package cn.edu.scnu.momentkeep.vo;

import lombok.Data;

@Data
public class LoginResponseVO {
    
    private String token;
    private UserProfileVO user;
}
