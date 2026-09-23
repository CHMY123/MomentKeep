package cn.edu.scnu.momentkeep.controller;

import cn.edu.scnu.momentkeep.common.PageResult;
import cn.edu.scnu.momentkeep.common.Result;
import cn.edu.scnu.momentkeep.entity.Checkin;
import cn.edu.scnu.momentkeep.service.CheckinService;
import cn.edu.scnu.momentkeep.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/checkin")
@Tag(name = "打卡管理")
@RequiredArgsConstructor
public class CheckinController {

    /** 历史记录单次查询上限，避免前端传入超大 limit 拖垮数据库 */
    private static final int MAX_HISTORY_LIMIT = 200;

    /** 历史分页单页条数上限 */
    private static final int MAX_HISTORY_PAGE_SIZE = 100;

    private final CheckinService checkinService;
    private final UserService userService;

    /**
     * 保存打卡记录
     */
    @PostMapping
    @Operation(summary = "保存打卡记录")
    public Result<Checkin> saveCheckin(@RequestBody @Valid Checkin checkin) {
        checkin.setId(null);
        checkin.setUserId(userService.getCurrentUserId());
        checkin.setCheckinTime(LocalDateTime.now());
        // 审计与并发字段必须由服务端决定：MyMetaObjectHandler 只在字段为 null 时填充，
        // 若客户端传入 createTime / version 会被原样写库（批量赋值风险）
        checkin.setCreateTime(null);
        checkin.setUpdateTime(null);
        checkin.setVersion(null);
        return Result.success(checkinService.saveCheckin(checkin));
    }

    /**
     * 获取用户某天的打卡记录
     */
    @GetMapping("/by-date")
    @Operation(summary = "按日期获取打卡记录")
    public Result<List<Checkin>> getCheckinsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String date) {
        LocalDate checkinDate = LocalDate.parse(date);
        return Result.success(checkinService.getCheckinsByDate(userService.getCurrentUserId(), checkinDate));
    }

    /**
     * 获取用户打卡统计数据
     */
    @GetMapping("/stats")
    @Operation(summary = "获取打卡统计")
    public Result<Map<String, Object>> getCheckinStats() {
        return Result.success(checkinService.getCheckinStats(userService.getCurrentUserId()));
    }

    /**
     * 检查用户某天是否已打卡
     */
    @GetMapping("/has-checked-in")
    @Operation(summary = "检查是否已打卡")
    public Result<Boolean> hasCheckedIn(@RequestParam String type, @RequestParam String date) {
        LocalDate checkinDate = LocalDate.parse(date);
        return Result.success(checkinService.hasCheckedIn(userService.getCurrentUserId(), type, checkinDate));
    }

    /**
     * 删除打卡记录
     */
    @DeleteMapping
    @Operation(summary = "取消打卡")
    public Result<String> deleteCheckin(@RequestParam String type,
                                        @RequestParam String date,
                                        @RequestParam(required = false) String time) {
        LocalDate checkinDate = LocalDate.parse(date);
        /*
         * time 为可选参数：传了则只取消那一条记录，不传则维持原语义（取消该类型当天全部）。
         * 加可选参数而不是改必填，是为了向后兼容——老客户端不传仍能正常工作。
         */
        checkinService.deleteCheckin(userService.getCurrentUserId(), type, checkinDate, time);
        return Result.success("取消打卡成功");
    }

    /**
     * 获取打卡时间分布
     */
    @GetMapping("/time-distribution")
    @Operation(summary = "获取打卡时间分布")
    public Result<List<Map<String, Object>>> getTimeDistribution(
            @RequestParam String type,
            @RequestParam(required = false) String subType) {
        return Result.success(
                checkinService.getTimeDistribution(userService.getCurrentUserId(), type, subType));
    }

    /**
     * 获取历史打卡记录（最近 N 条，供每日打卡页使用）
     */
    @GetMapping("/history")
    @Operation(summary = "获取历史打卡记录")
    public Result<List<Checkin>> getHistoryCheckins(
            @RequestParam(required = false, defaultValue = "30") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_HISTORY_LIMIT);
        return Result.success(checkinService.getHistoryCheckins(userService.getCurrentUserId(), safeLimit));
    }

    /**
     * 打卡时间分布汇总（图表数据源）
     * @description 在数据库侧完成聚合，默认统计全量历史；传日期区间则只统计区间内数据
     */
    @GetMapping("/time-distribution/summary")
    @Operation(summary = "打卡时间分布汇总")
    public Result<Map<String, Object>> getTimeDistributionSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(checkinService.getTimeDistributionSummary(
                userService.getCurrentUserId(), startDate, endDate));
    }

    /**
     * 分页查询历史打卡记录（供历史打卡页面使用）
     */
    @GetMapping("/history/page")
    @Operation(summary = "分页查询历史打卡记录")
    public Result<PageResult<Checkin>> getHistoryPage(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String subType) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_HISTORY_PAGE_SIZE);
        return Result.success(checkinService.getHistoryPage(
                userService.getCurrentUserId(), safePage, safeSize, startDate, endDate, type, subType));
    }
}
