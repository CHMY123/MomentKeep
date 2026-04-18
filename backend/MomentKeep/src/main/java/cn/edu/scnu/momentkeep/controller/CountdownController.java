package cn.edu.scnu.momentkeep.controller;

import cn.edu.scnu.momentkeep.common.Result;
import cn.edu.scnu.momentkeep.entity.Countdown;
import cn.edu.scnu.momentkeep.service.CountdownService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/countdown")
@Tag(name = "倒计时管理")
@RequiredArgsConstructor
public class CountdownController {

    private final CountdownService countdownService;

    @PostMapping
    @Operation(summary = "创建倒计时")
    public Result<Countdown> createCountdown(@RequestBody Countdown countdown) {
        Countdown createdCountdown = countdownService.createCountdown(countdown);
        return Result.success(createdCountdown);
    }

    @PutMapping
    @Operation(summary = "更新倒计时")
    public Result<Countdown> updateCountdown(@RequestBody Countdown countdown) {
        Countdown updatedCountdown = countdownService.updateCountdown(countdown);
        return Result.success(updatedCountdown);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除倒计时")
    public Result<Void> deleteCountdown(@PathVariable Long id) {
        countdownService.deleteCountdown(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取倒计时详情")
    public Result<Countdown> getCountdownById(@PathVariable Long id) {
        Countdown countdown = countdownService.getCountdownById(id);
        return Result.success(countdown);
    }

    @GetMapping
    @Operation(summary = "获取用户所有倒计时")
    public Result<List<Countdown>> getCountdownsByUserId() {
        // 从当前登录用户获取userId
        Long userId = 1L; // 这里需要从SecurityContext中获取，暂时硬编码
        List<Countdown> countdowns = countdownService.getCountdownsByUserId(userId);
        return Result.success(countdowns);
    }

    @GetMapping("/active")
    @Operation(summary = "获取活跃倒计时")
    public Result<List<Countdown>> getActiveCountdowns() {
        // 从当前登录用户获取userId
        Long userId = 1L; // 这里需要从SecurityContext中获取，暂时硬编码
        List<Countdown> countdowns = countdownService.getActiveCountdowns(userId);
        return Result.success(countdowns);
    }
}