package cn.edu.scnu.momentkeep.service;

import cn.edu.scnu.momentkeep.dto.request.ChangePasswordDTO;
import cn.edu.scnu.momentkeep.dto.request.LoginDTO;
import cn.edu.scnu.momentkeep.dto.request.UpdateProfileDTO;
import cn.edu.scnu.momentkeep.dto.request.UserRegisterDTO;
import cn.edu.scnu.momentkeep.entity.User;
import cn.edu.scnu.momentkeep.vo.LoginResponseVO;
import cn.edu.scnu.momentkeep.vo.UserProfileVO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

public interface UserService extends IService<User> {
    
    void register(UserRegisterDTO dto);
    
    LoginResponseVO login(LoginDTO dto);
    
    UserProfileVO getProfile();
    
    void updateProfile(UpdateProfileDTO dto);
    
    String uploadAvatar(MultipartFile file);
    
    String uploadBackground(MultipartFile file);
    
    void clearBackground();
    
    void logout();
    
    void deleteAccount();
    
    void changePassword(ChangePasswordDTO dto);
    
    User getByUsername(String username);
    
    User getCurrentUser();
}
