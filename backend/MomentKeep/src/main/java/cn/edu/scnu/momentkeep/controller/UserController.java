/**
 * MomentKeep 朝暮记 - 用户管理控制器
 *
 * @description 处理用户注册、登录、信息管理、头像上传等相关请求
 * @author MomentKeep Team
 * @since 2026-04-18
 */
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

/**
 * 用户管理控制器
 * 提供用户注册、登录、资料管理、头像上传等RESTful API
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户注册接口
     * @param dto 注册信息（用户名、密码、昵称、手机号、邮箱）
     * @return 成功返回空结果
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Void> register(@RequestBody @Valid UserRegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    /**
     * 用户登录接口
     * @param dto 登录信息（用户名、密码）
     * @return 登录响应（包含Token和用户信息）
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginResponseVO> login(@RequestBody @Valid LoginDTO dto) {
        LoginResponseVO response = userService.login(dto);
        return Result.success(response);
    }

    /**
     * 获取当前用户信息
     * @return 用户资料信息
     */
    @GetMapping("/profile")
    @Operation(summary = "获取用户信息")
    public Result<UserProfileVO> getProfile() {
        UserProfileVO profile = userService.getProfile();
        return Result.success(profile);
    }

    /**
     * 更新用户信息
     * @param dto 更新后的用户信息
     * @return 成功返回空结果
     */
    @PutMapping("/profile")
    @Operation(summary = "更新用户信息")
    public Result<Void> updateProfile(@RequestBody @Valid UpdateProfileDTO dto) {
        userService.updateProfile(dto);
        return Result.success();
    }

    /**
     * 上传用户头像
     * @param file 头像图片文件
     * @return 头像URL
     */
    @PostMapping("/avatar")
    @Operation(summary = "上传头像")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String avatarUrl = userService.uploadAvatar(file);
        return Result.success(avatarUrl);
    }

    /**
     * 上传背景图片
     * @param file 背景图片文件
     * @return 背景图片URL
     */
    @PostMapping("/background")
    @Operation(summary = "上传背景图")
    public Result<String> uploadBackground(@RequestParam("file") MultipartFile file) {
        String backgroundUrl = userService.uploadBackground(file);
        return Result.success(backgroundUrl);
    }

    /**
     * 清空用户背景图片
     * @return 成功返回空结果
     */
    @DeleteMapping("/background")
    @Operation(summary = "清空背景图")
    public Result<Void> clearBackground() {
        userService.clearBackground();
        return Result.success();
    }

    /**
     * 用户登出
     * @return 成功返回空结果
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout() {
        userService.logout();
        return Result.success();
    }

    /**
     * 注销当前用户账户
     * @return 成功返回空结果
     */
    @PostMapping("/delete")
    @Operation(summary = "注销账户")
    public Result<Void> deleteAccount(@RequestBody @Valid DeleteAccountDTO dto) {
        userService.deleteAccount();
        return Result.success();
    }

    /**
     * 修改用户密码
     * @param dto 密码修改信息（旧密码、新密码）
     * @return 成功返回空结果
     */
    @PostMapping("/change-password")
    @Operation(summary = "修改密码")
    public Result<Void> changePassword(@RequestBody @Valid ChangePasswordDTO dto) {
        userService.changePassword(dto);
        return Result.success();
    }
}
