package cn.edu.scnu.momentkeep.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterDTO {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度在3-20之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少为6位")
    private String password;
    
    @NotBlank(message = "昵称不能为空")
    private String nickname;
    
    private String email;
    
    @NotBlank(message = "手机号不能为空")
    private String phone;
    
    private boolean agreement;
}
