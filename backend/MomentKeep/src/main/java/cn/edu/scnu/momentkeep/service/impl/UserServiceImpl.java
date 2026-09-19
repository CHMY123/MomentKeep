package cn.edu.scnu.momentkeep.service.impl;

import cn.edu.scnu.momentkeep.common.BusinessException;
import cn.edu.scnu.momentkeep.config.S3Config;
import cn.edu.scnu.momentkeep.dto.request.ChangePasswordDTO;
import cn.edu.scnu.momentkeep.dto.request.DeleteAccountDTO;
import cn.edu.scnu.momentkeep.dto.request.LoginDTO;
import cn.edu.scnu.momentkeep.dto.request.UpdateProfileDTO;
import cn.edu.scnu.momentkeep.dto.request.UserRegisterDTO;
import cn.edu.scnu.momentkeep.entity.AiChat;
import cn.edu.scnu.momentkeep.entity.AiChatQuota;
import cn.edu.scnu.momentkeep.entity.Checkin;
import cn.edu.scnu.momentkeep.entity.Countdown;
import cn.edu.scnu.momentkeep.entity.Feedback;
import cn.edu.scnu.momentkeep.entity.FocusRecord;
import cn.edu.scnu.momentkeep.entity.Todo;
import cn.edu.scnu.momentkeep.entity.User;
import cn.edu.scnu.momentkeep.entity.UserSetting;
import cn.edu.scnu.momentkeep.mapper.AiChatMapper;
import cn.edu.scnu.momentkeep.mapper.AiChatQuotaMapper;
import cn.edu.scnu.momentkeep.mapper.CheckinMapper;
import cn.edu.scnu.momentkeep.mapper.CountdownMapper;
import cn.edu.scnu.momentkeep.mapper.FeedbackMapper;
import cn.edu.scnu.momentkeep.mapper.FocusRecordMapper;
import cn.edu.scnu.momentkeep.mapper.TodoMapper;
import cn.edu.scnu.momentkeep.mapper.UserMapper;
import cn.edu.scnu.momentkeep.mapper.UserSettingMapper;
import cn.edu.scnu.momentkeep.security.JwtTokenProvider;
import cn.edu.scnu.momentkeep.security.LoginUser;
import cn.edu.scnu.momentkeep.service.UserService;
import cn.edu.scnu.momentkeep.vo.LoginResponseVO;
import cn.edu.scnu.momentkeep.vo.UserProfileVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /** 允许上传的图片扩展名 */
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp", "gif");

    /** 单张图片大小上限（字节） */
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    /** 正常状态 */
    private static final int STATUS_ACTIVE = 1;

    /** 已注销状态 */
    private static final int STATUS_DELETED = -1;

    private final UserMapper userMapper;
    private final CheckinMapper checkinMapper;
    private final TodoMapper todoMapper;
    private final CountdownMapper countdownMapper;
    private final FeedbackMapper feedbackMapper;
    private final AiChatMapper aiChatMapper;
    private final FocusRecordMapper focusRecordMapper;
    private final AiChatQuotaMapper aiChatQuotaMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
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
        user.setStatus(STATUS_ACTIVE);
        user.setRole("USER");
        user.setTokenVersion(0);

        userMapper.insert(user);
    }

    @Override
    public LoginResponseVO login(LoginDTO dto) {
        // 查找用户
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", dto.getUsername()));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 已注销/被禁用的账号必须拒绝登录（否则注销形同虚设）
        if (user.getStatus() == null || user.getStatus() != STATUS_ACTIVE) {
            throw new BusinessException("账号已注销或被禁用，无法登录");
        }

        // 生成token（不使用Redis存储，简化部署；通过令牌版本号支持吊销）
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getTokenVersion());

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
        Long userId = user.getId();

        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userId);

        if (dto.getNickname() != null) {
            if (dto.getNickname().trim().isEmpty()) {
                throw new BusinessException("昵称不能为空");
            }
            updateWrapper.set("nickname", dto.getNickname().trim());
        }
        if (dto.getEmail() != null) {
            updateWrapper.set("email", dto.getEmail());
        }
        if (dto.getPhone() != null) {
            updateWrapper.set("phone", dto.getPhone());
        }
        if (dto.getAvatar() != null) {
            updateWrapper.set("avatar", validateImageUrl(dto.getAvatar()));
        }

        super.update(updateWrapper);
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        String objectKey = buildImageObjectKey(file, "avatars");

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            log.error("头像上传失败：key={}", objectKey, e);
            throw new BusinessException("头像上传失败，请稍后重试");
        }

        String fileUrl = buildPublicUrl(objectKey);
        User currentUser = getCurrentUser();
        userMapper.updateAvatarById(currentUser.getId(), fileUrl);
        return fileUrl;
    }

    @Override
    public String uploadBackground(MultipartFile file) {
        String objectKey = buildImageObjectKey(file, "backgrounds");

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            log.error("背景图上传失败：key={}", objectKey, e);
            throw new BusinessException("背景图上传失败，请稍后重试");
        }

        String fileUrl = buildPublicUrl(objectKey);
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
    }

    @Override
    public void clearBackground() {
        User currentUser = getCurrentUser();
        UserSetting setting = userSettingMapper.selectOne(
            new QueryWrapper<UserSetting>().eq("user_id", currentUser.getId())
        );
        if (setting == null) {
            return;
        }

        // 对象存储删除失败不应阻塞业务，数据库状态仍需清空
        deleteObjectQuietly(setting.getBackgroundImage(), "背景图");

        UpdateWrapper<UserSetting> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id", currentUser.getId())
                .set("background_image", null);
        userSettingMapper.update(null, updateWrapper);
    }

    @Override
    public void logout() {
        // JWT 无状态，通过自增令牌版本号让已签发的令牌立即失效
        revokeTokens(getCurrentUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(DeleteAccountDTO dto) {
        // 确认字段此前被完全忽略，导致传 false 也会注销账号，这里必须显式校验
        if (dto == null || !Boolean.TRUE.equals(dto.getConfirmation())) {
            throw new BusinessException("请确认注销操作");
        }

        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();

        // 0. 先取出对象存储上的资源地址（删掉数据行之后就查不到了）
        String avatarUrl = currentUser.getAvatar();
        UserSetting setting = userSettingMapper.selectOne(
                new QueryWrapper<UserSetting>().eq("user_id", userId));
        String backgroundUrl = setting == null ? null : setting.getBackgroundImage();

        // 1. 删除用户相关数据（多表写操作必须在一个事务内，避免中途失败留下脏数据）
        checkinMapper.delete(new QueryWrapper<Checkin>().eq("user_id", userId));
        todoMapper.delete(new QueryWrapper<Todo>().eq("user_id", userId));
        countdownMapper.delete(new QueryWrapper<Countdown>().eq("user_id", userId));
        feedbackMapper.delete(new QueryWrapper<Feedback>().eq("user_id", userId));
        userSettingMapper.delete(new QueryWrapper<UserSetting>().eq("user_id", userId));
        aiChatMapper.delete(new QueryWrapper<AiChat>().eq("user_id", userId));
        // 此前漏删这两张表：注销后仍残留专注记录与 AI 配额行（个人数据未真正清空）
        focusRecordMapper.delete(new QueryWrapper<FocusRecord>().eq("user_id", userId));
        aiChatQuotaMapper.delete(new QueryWrapper<AiChatQuota>().eq("user_id", userId));

        // 2. 让所有已签发令牌立即失效
        userMapper.incrementTokenVersion(userId);

        // 3. 更新用户状态为注销
        User user = new User();
        user.setId(userId);
        user.setStatus(STATUS_DELETED);
        userMapper.updateById(user);

        // 4. 尽力清理对象存储上的头像与背景图（失败只记日志，不影响注销结果）
        deleteObjectQuietly(avatarUrl, "头像");
        deleteObjectQuietly(backgroundUrl, "背景图");

        log.info("用户已注销：userId={}", userId);
    }

    /**
     * 校验图片地址
     *
     * <p>头像/背景图地址会被前端拼进 CSS（{@code url(...)}）或 img.src，
     * 若允许 {@code javascript:}、{@code data:} 等协议会形成注入面，因此只接受 http(s)。</p>
     *
     * @param url 待校验地址，空串表示清空
     * @return 校验后的地址；空值返回 null
     */
    private String validateImageUrl(String url) {
        String trimmed = url == null ? null : url.trim();
        if (trimmed == null || trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 512) {
            throw new BusinessException("图片地址过长");
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw new BusinessException("图片地址格式不正确");
        }
        return trimmed;
    }

    /**
     * 尽力删除对象存储中的对象（失败只记日志，不影响主流程）
     *
     * @param fileUrl 公网访问地址
     * @param label   用于日志描述的资源名称
     */
    private void deleteObjectQuietly(String fileUrl, String label) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        String objectKey = extractObjectKey(fileUrl);
        if (objectKey == null) {
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(objectKey)
                    .build());
        } catch (Exception e) {
            log.warn("删除{}对象失败：key={}", label, objectKey, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

        if (dto.getNewPassword().equals(dto.getOldPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }

        // 更新密码
        User user = new User();
        user.setId(currentUser.getId());
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);

        // 改密后让旧令牌全部失效（含其他设备），用户需重新登录
        revokeTokens(currentUser.getId());
    }

    @Override
    public User getByUsername(String username) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    @Override
    public User getCurrentUser() {
        Long userId = getCurrentUserIdOrNull();
        if (userId == null) {
            throw new BusinessException("无法获取当前用户信息");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("无法获取当前用户信息");
        }
        return user;
    }

    @Override
    public Long getCurrentUserId() {
        Long userId = getCurrentUserIdOrNull();
        if (userId == null) {
            throw new BusinessException("无法获取当前用户信息");
        }
        return userId;
    }

    /**
     * 从 SecurityContext 解析当前用户 ID，未登录返回 null。
     *
     * <p>优先使用 {@link LoginUser} 中携带的 ID，避免每个请求都多查一次用户表。</p>
     */
    private Long getCurrentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            LoginUser loginUser = LoginUser.from(userDetails);
            if (loginUser != null && loginUser.getUserId() != null) {
                return loginUser.getUserId();
            }
            // 兜底：非 LoginUser 主体（如自定义扩展）时按用户名回查
            if (userDetails.getUsername() != null) {
                User user = userMapper.selectOne(
                        new QueryWrapper<User>().eq("username", userDetails.getUsername()));
                return user == null ? null : user.getId();
            }
        }
        return null;
    }

    /**
     * 自增令牌版本号，使该用户已签发的所有 JWT 立即失效
     */
    private void revokeTokens(Long userId) {
        if (userId == null) {
            return;
        }
        int affected = userMapper.incrementTokenVersion(userId);
        log.info("已吊销用户令牌：userId={}, affected={}", userId, affected);
    }

    /**
     * 校验上传图片并生成对象存储 key。
     *
     * <p>原实现直接把用户可控的原始文件名拼进 key，既可能被路径穿越/特殊字符污染，
     * 也不校验类型与大小，这里统一收敛：扩展名白名单 + 大小上限 + UUID 重命名。</p>
     *
     * @param file   上传文件
     * @param prefix 存储目录前缀（avatars / backgrounds）
     * @return 形如 {@code avatars/7f3a...8c.png} 的对象 key
     */
    private String buildImageObjectKey(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException("图片大小不能超过 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException("仅支持上传图片文件");
        }

        String extension = resolveExtension(file.getOriginalFilename());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 jpg、jpeg、png、webp、gif 格式");
        }

        // 不使用任何用户可控的文件名，彻底杜绝路径穿越与脏字符
        return prefix + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 根据对象 key 拼接公开访问地址
     */
    private String buildPublicUrl(String objectKey) {
        return "https://" + s3Config.getBucketName() + ".s3.bitiful.net/" + objectKey;
    }

    /**
     * 从完整 URL 中解析对象 key（依赖 URL 路径，而不是硬编码域名反查，
     * 换域名或接入 CDN 后依然可用）
     *
     * @return 对象 key，解析失败返回 null
     */
    private String extractObjectKey(String url) {
        try {
            String path = URI.create(url).getPath();
            if (path == null || path.length() <= 1) {
                return null;
            }
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (IllegalArgumentException e) {
            log.warn("无法解析背景图 URL：{}", url);
            return null;
        }
    }
}
