package cn.edu.scnu.momentkeep.controller;

import cn.edu.scnu.momentkeep.common.Result;
import cn.edu.scnu.momentkeep.entity.FocusRecord;
import cn.edu.scnu.momentkeep.service.FocusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/focus")
@Tag(name = "专注计时管理")
public class FocusController {

    @Autowired
    private FocusService focusService;

    @PostMapping("/record")
    @Operation(summary = "保存专注记录")
    public Result<FocusRecord> saveRecord(@RequestBody FocusRecord record,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        record.setUserId(userId);
        FocusRecord savedRecord = focusService.saveRecord(record);
        return Result.success(savedRecord);
    }

    @GetMapping("/records")
    @Operation(summary = "获取今日专注记录和统计")
    public Result<Map<String, Object>> getRecords(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
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
    public Result<Map<String, Object>> getRecordsByTodoId(@PathVariable Long todoId,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserIdFromUserDetails(userDetails);
        Map<String, Object> result = focusService.getRecordsByTodoId(userId, todoId);
        return Result.success(result);
    }

    private Long getUserIdFromUserDetails(UserDetails userDetails) {
        if (userDetails == null) {
            return 1L;
        }
        try {
            return Long.parseLong(userDetails.getUsername());
        } catch (NumberFormatException e) {
            return 1L;
        }
    }
}
