package cn.edu.scnu.momentkeep.controller;

import cn.edu.scnu.momentkeep.common.BusinessException;
import cn.edu.scnu.momentkeep.common.Result;
import cn.edu.scnu.momentkeep.entity.FocusRecord;
import cn.edu.scnu.momentkeep.service.FocusService;
import cn.edu.scnu.momentkeep.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/focus")
@Tag(name = "专注计时管理")
@RequiredArgsConstructor
public class FocusController {

    /** 单次专注时长上限（秒）：24 小时，防止客户端提交离谱数值 */
    private static final int MAX_DURATION_SECONDS = 24 * 60 * 60;

    /** 允许的专注模式，与前端 focus.vue 的 currentMode 取值保持一致 */
    private static final Set<String> ALLOWED_MODES = Set.of("stopwatch", "countdown", "pomodoro");

    private final FocusService focusService;
    private final UserService userService;

    @PostMapping("/record")
    @Operation(summary = "保存专注记录")
    public Result<FocusRecord> saveRecord(@RequestBody @Valid FocusRecord record) {
        if (record == null) {
            throw new BusinessException("参数不能为空");
        }

        // 主键与归属必须由服务端决定：
        // 此前未清 id，客户端可指定主键写入（撞主键则直接报错），属于典型的批量赋值风险
        record.setId(null);
        record.setUserId(userService.getCurrentUserId());
        record.setVersion(null);

        Integer duration = record.getDuration();
        if (duration == null || duration <= 0 || duration > MAX_DURATION_SECONDS) {
            throw new BusinessException("专注时长不合法");
        }
        if (record.getMode() != null && !ALLOWED_MODES.contains(record.getMode())) {
            throw new BusinessException("专注模式不合法");
        }
        if (record.getStartTime() == null) {
            record.setStartTime(LocalDateTime.now());
        }

        return Result.success(focusService.saveRecord(record));
    }

    @GetMapping("/records")
    @Operation(summary = "获取今日专注记录和统计")
    public Result<Map<String, Object>> getRecords() {
        Long userId = userService.getCurrentUserId();
        List<FocusRecord> todayRecords = focusService.getTodayRecords(userId);
        Map<String, Object> stats = focusService.getFocusStats(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("todayRecords", todayRecords);
        result.put("todayTotal", stats.get("todayTotal"));
        result.put("weekTotal", stats.get("weekTotal"));
        result.put("total", stats.get("total"));

        return Result.success(result);
    }

    @GetMapping("/record/todo/{todoId}")
    @Operation(summary = "获取指定待办的专注记录")
    public Result<Map<String, Object>> getRecordsByTodoId(@PathVariable Long todoId) {
        return Result.success(focusService.getRecordsByTodoId(userService.getCurrentUserId(), todoId));
    }
}
