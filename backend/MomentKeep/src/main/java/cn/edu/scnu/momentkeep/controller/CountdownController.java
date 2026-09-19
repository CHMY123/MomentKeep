package cn.edu.scnu.momentkeep.controller;

import cn.edu.scnu.momentkeep.common.Result;
import cn.edu.scnu.momentkeep.entity.Countdown;
import cn.edu.scnu.momentkeep.service.CountdownService;
import cn.edu.scnu.momentkeep.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/countdown")
@Tag(name = "倒计时管理")
@RequiredArgsConstructor
public class CountdownController {

    private final CountdownService countdownService;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "创建倒计时")
    public Result<Countdown> createCountdown(@RequestBody @Valid Countdown countdown) {
        return Result.success(countdownService.createCountdown(countdown, userService.getCurrentUserId()));
    }

    @PutMapping
    @Operation(summary = "更新倒计时")
    public Result<Countdown> updateCountdown(@RequestBody @Valid Countdown countdown) {
        return Result.success(countdownService.updateCountdown(countdown, userService.getCurrentUserId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除倒计时")
    public Result<Void> deleteCountdown(@PathVariable Long id) {
        countdownService.deleteCountdown(id, userService.getCurrentUserId());
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取倒计时详情")
    public Result<Countdown> getCountdownById(@PathVariable Long id) {
        return Result.success(countdownService.getCountdownById(id, userService.getCurrentUserId()));
    }

    @GetMapping
    @Operation(summary = "获取用户所有倒计时")
    public Result<List<Countdown>> getCountdownsByUserId() {
        return Result.success(countdownService.getCountdownsByUserId(userService.getCurrentUserId()));
    }

    @GetMapping("/active")
    @Operation(summary = "获取活跃倒计时")
    public Result<List<Countdown>> getActiveCountdowns() {
        return Result.success(countdownService.getActiveCountdowns(userService.getCurrentUserId()));
    }
}
