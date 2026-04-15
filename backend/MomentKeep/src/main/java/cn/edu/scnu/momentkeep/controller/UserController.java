package cn.edu.scnu.momentkeep.controller;

import cn.edu.scnu.momentkeep.common.Result;
import cn.edu.scnu.momentkeep.dto.request.ChangePasswordDTO;
import cn.edu.scnu.momentkeep.dto.request.DeleteAccountDTO;
import cn.edu.scnu.momentkeep.dto.request.LoginDTO;
import cn.edu.scnu.momentkeep.dto.request.UpdateProfileDTO;
import cn.edu.scnu.momentkeep.dto.request.UserRegisterDTO;
import cn.edu.scnu.momentkeep.service.UserService;
import cn.edu.scnu.momentkeep.vo.LoginResponseVO;
import cn.edu.scnu.momentkeep.vo.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Void> register(@RequestBody @Valid UserRegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }
    
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginResponseVO> login(@RequestBody @Valid LoginDTO dto) {
        LoginResponseVO response = userService.login(dto);
        return Result.success(response);
    }
    
    @GetMapping("/profile")
    @Operation(summary = "获取用户信息")
    public Result<UserProfileVO> getProfile() {
        UserProfileVO profile = userService.getProfile();
        return Result.success(profile);
    }
    
    @PutMapping("/profile")
    @Operation(summary = "更新用户信息")
    public Result<Void> updateProfile(@RequestBody @Valid UpdateProfileDTO dto) {
        userService.updateProfile(dto);
        return Result.success();
    }
    
    @PostMapping("/avatar")
    @Operation(summary = "上传头像")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String avatarUrl = userService.uploadAvatar(file);
        return Result.success(avatarUrl);
    }
    
    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout() {
        userService.logout();
        return Result.success();
    }
    
    @PostMapping("/delete")
    @Operation(summary = "注销账户")
    public Result<Void> deleteAccount(@RequestBody @Valid DeleteAccountDTO dto) {
        userService.deleteAccount();
        return Result.success();
    }
    
    @PostMapping("/change-password")
    @Operation(summary = "修改密码")
    public Result<Void> changePassword(@RequestBody @Valid ChangePasswordDTO dto) {
        userService.changePassword(dto);
        return Result.success();
    }
}
