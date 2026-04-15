package cn.edu.scnu.momentkeep.service.impl;

import cn.edu.scnu.momentkeep.common.BusinessException;
import cn.edu.scnu.momentkeep.dto.request.ChangePasswordDTO;
import cn.edu.scnu.momentkeep.dto.request.LoginDTO;
import cn.edu.scnu.momentkeep.dto.request.UpdateProfileDTO;
import cn.edu.scnu.momentkeep.dto.request.UserRegisterDTO;
import cn.edu.scnu.momentkeep.entity.Checkin;
import cn.edu.scnu.momentkeep.entity.Countdown;
import cn.edu.scnu.momentkeep.entity.Feedback;
import cn.edu.scnu.momentkeep.entity.Todo;
import cn.edu.scnu.momentkeep.entity.User;
import cn.edu.scnu.momentkeep.mapper.CheckinMapper;
import cn.edu.scnu.momentkeep.mapper.CountdownMapper;
import cn.edu.scnu.momentkeep.mapper.FeedbackMapper;
import cn.edu.scnu.momentkeep.mapper.TodoMapper;
import cn.edu.scnu.momentkeep.mapper.UserMapper;
import cn.edu.scnu.momentkeep.security.JwtTokenProvider;
import cn.edu.scnu.momentkeep.service.UserService;
import cn.edu.scnu.momentkeep.vo.LoginResponseVO;
import cn.edu.scnu.momentkeep.vo.UserProfileVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    private final UserMapper userMapper;
    private final CheckinMapper checkinMapper;
    private final TodoMapper todoMapper;
    private final CountdownMapper countdownMapper;
    private final FeedbackMapper feedbackMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final HttpServletRequest request;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public void register(UserRegisterDTO dto) {
        // 检查用户是否存在
        User existingUser = userMapper.selectOne(new QueryWrapper<User>().eq("username", dto.getUsername()));
        if (existingUser != null) {
            throw new BusinessException("用户已存在");
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setAvatar("https://img.icons8.com/ios-filled/50/000000/user.png");
        user.setStatus(1); // 1表示正常
        user.setRole("USER");
        
        userMapper.insert(user);
    }
    
    @Override
    public LoginResponseVO login(LoginDTO dto) {
        // 查找用户
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", dto.getUsername()));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 验证密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        
        // 生成token
        org.springframework.security.core.userdetails.User springUser = 
            new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), java.util.Collections.emptyList());
        String token = jwtTokenProvider.generateToken(springUser);
        
        // 保存token到Redis
        redisTemplate.opsForValue().set("token:" + token, user.getUsername());
        
        // 构建响应
        LoginResponseVO response = new LoginResponseVO();
        response.setToken(token);
        
        UserProfileVO profile = new UserProfileVO();
        profile.setId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setNickname(user.getNickname());
        profile.setEmail(user.getEmail());
        profile.setPhone(user.getPhone());
        profile.setAvatar(user.getAvatar());
        response.setUser(profile);
        
        return response;
    }
    
    @Override
    public UserProfileVO getProfile() {
        User user = getCurrentUser();
        
        UserProfileVO profile = new UserProfileVO();
        profile.setId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setNickname(user.getNickname());
        profile.setEmail(user.getEmail());
        profile.setPhone(user.getPhone());
        profile.setAvatar(user.getAvatar());
        
        return profile;
    }
    
    @Override
    public void updateProfile(UpdateProfileDTO dto) {
        User user = getCurrentUser();
        
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        
        userMapper.updateById(user);
    }
    
    @Override
    public String uploadAvatar(MultipartFile file) {
        // 这里简化处理，实际应该上传到云存储
        // 暂时返回一个默认的头像URL
        return "https://img.icons8.com/ios-filled/50/000000/user.png";
    }
    
    @Override
    public void logout() {
        String token = request.getHeader("Authorization").substring(7);
        redisTemplate.delete("token:" + token);
    }
    
    @Override
    public void deleteAccount() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();
        
        // 1. 删除用户相关数据
        checkinMapper.delete(new QueryWrapper<Checkin>().eq("user_id", userId));
        todoMapper.delete(new QueryWrapper<Todo>().eq("user_id", userId));
        countdownMapper.delete(new QueryWrapper<Countdown>().eq("user_id", userId));
        feedbackMapper.delete(new QueryWrapper<Feedback>().eq("user_id", userId));
        
        // 2. 更新用户状态为注销
        User user = new User();
        user.setId(userId);
        user.setStatus(-1); // -1表示已注销
        userMapper.updateById(user);
        
        // 3. 清除用户token
        String token = request.getHeader("Authorization").substring(7);
        redisTemplate.delete("token:" + token);
    }
    
    @Override
    public void changePassword(ChangePasswordDTO dto) {
        User currentUser = getCurrentUser();
        
        // 验证旧密码
        if (!passwordEncoder.matches(dto.getOldPassword(), currentUser.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        
        // 验证新密码和确认密码
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        
        // 更新密码
        User user = new User();
        user.setId(currentUser.getId());
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        
        // 清除用户token，强制重新登录
        String token = request.getHeader("Authorization").substring(7);
        redisTemplate.delete("token:" + token);
    }
    
    @Override
    public User getByUsername(String username) {
        return userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    }
    
    @Override
    public User getCurrentUser() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return getByUsername(username);
    }
}
