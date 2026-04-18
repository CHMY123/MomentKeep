package cn.edu.scnu.momentkeep.service.impl;

import cn.edu.scnu.momentkeep.common.BusinessException;
import cn.edu.scnu.momentkeep.config.S3Config;
import cn.edu.scnu.momentkeep.dto.request.ChangePasswordDTO;
import cn.edu.scnu.momentkeep.dto.request.LoginDTO;
import cn.edu.scnu.momentkeep.dto.request.UpdateProfileDTO;
import cn.edu.scnu.momentkeep.dto.request.UserRegisterDTO;
import cn.edu.scnu.momentkeep.entity.Checkin;
import cn.edu.scnu.momentkeep.entity.Countdown;
import cn.edu.scnu.momentkeep.entity.Feedback;
import cn.edu.scnu.momentkeep.entity.Todo;
import cn.edu.scnu.momentkeep.entity.User;
import cn.edu.scnu.momentkeep.entity.UserSetting;
import cn.edu.scnu.momentkeep.mapper.CheckinMapper;
import cn.edu.scnu.momentkeep.mapper.CountdownMapper;
import cn.edu.scnu.momentkeep.mapper.FeedbackMapper;
import cn.edu.scnu.momentkeep.mapper.TodoMapper;
import cn.edu.scnu.momentkeep.mapper.UserMapper;
import cn.edu.scnu.momentkeep.mapper.UserSettingMapper;
import cn.edu.scnu.momentkeep.security.JwtTokenProvider;
import cn.edu.scnu.momentkeep.service.UserService;
import cn.edu.scnu.momentkeep.vo.LoginResponseVO;
import cn.edu.scnu.momentkeep.vo.UserProfileVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import java.io.IOException;
import java.util.UUID;

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
    private final S3Client s3Client;
    private final S3Config s3Config;
    private final UserSettingMapper userSettingMapper;
    
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

        UserSetting setting = userSettingMapper.selectOne(
            new QueryWrapper<UserSetting>().eq("user_id", user.getId())
        );
        if (setting != null) {
            profile.setBackgroundImage(setting.getBackgroundImage());
        }

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
        try {
            // 生成唯一的文件名
            String fileName = "avatars/" + UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

            // 上传文件到缤纷云 S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(fileName)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            // 生成文件的 URL - 使用正确的缤纷云 URL 格式
            String fileUrl = "https://" + s3Config.getBucketName() + ".s3.bitiful.net/" + fileName;

            // 更新数据库中用户的头像字段
            User currentUser = getCurrentUser();
            userMapper.updateAvatarById(currentUser.getId(), fileUrl);

            return fileUrl;
        } catch (IOException e) {
            throw new BusinessException("头像上传失败：" + e.getMessage());
        }
    }
    
    @Override
    public String uploadBackground(MultipartFile file) {
        try {
            String fileName = "backgrounds/" + UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(fileName)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            String fileUrl = "https://" + s3Config.getBucketName() + ".s3.bitiful.net/" + fileName;

            User currentUser = getCurrentUser();
            UserSetting setting = userSettingMapper.selectOne(
                new QueryWrapper<UserSetting>().eq("user_id", currentUser.getId())
            );
            if (setting == null) {
                setting = new UserSetting();
                setting.setUserId(currentUser.getId());
                setting.setBackgroundImage(fileUrl);
                setting.setTheme("light");
                setting.setAiAutoFill(true);
                setting.setNotifications(true);
                userSettingMapper.insert(setting);
            } else {
                setting.setBackgroundImage(fileUrl);
                userSettingMapper.updateById(setting);
            }

            return fileUrl;
        } catch (IOException e) {
            throw new BusinessException("背景图上传失败：" + e.getMessage());
        }
    }

    @Override
    public void clearBackground() {
        User currentUser = getCurrentUser();
        UserSetting setting = userSettingMapper.selectOne(
            new QueryWrapper<UserSetting>().eq("user_id", currentUser.getId())
        );
        if (setting != null) {
            if (setting.getBackgroundImage() != null) {
                String backgroundImageUrl = setting.getBackgroundImage();
                String objectKey = backgroundImageUrl.substring(backgroundImageUrl.lastIndexOf(".s3.bitiful.net/") + 17);

                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(s3Config.getBucketName())
                        .key(objectKey)
                        .build();
                s3Client.deleteObject(deleteObjectRequest);
            }

            UpdateWrapper<UserSetting> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("user_id", currentUser.getId())
                    .set("background_image", null);
            userSettingMapper.update(null, updateWrapper);
        }
    }
    
    @Override
    public void logout() {
        String token = getTokenFromRequest();
        if (token != null) {
            redisTemplate.delete("token:" + token);
        }
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
        String token = getTokenFromRequest();
        if (token != null) {
            redisTemplate.delete("token:" + token);
        }
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
        String token = getTokenFromRequest();
        if (token != null) {
            redisTemplate.delete("token:" + token);
        }
    }
    
    @Override
    public User getByUsername(String username) {
        return userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    }
    
    @Override
    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = null;
        if (principal instanceof String) {
            username = (String) principal;
        } else if (principal instanceof org.springframework.security.core.userdetails.User) {
            username = ((org.springframework.security.core.userdetails.User) principal).getUsername();
        }
        if (username == null) {
            throw new BusinessException("无法获取当前用户信息");
        }
        return getByUsername(username);
    }
    
    /**
     * 从请求头中获取 token
     */
    private String getTokenFromRequest() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
