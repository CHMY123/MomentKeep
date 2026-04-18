package cn.edu.scnu.momentkeep.controller;

import cn.edu.scnu.momentkeep.entity.Checkin;
import cn.edu.scnu.momentkeep.entity.User;
import cn.edu.scnu.momentkeep.service.CheckinService;
import cn.edu.scnu.momentkeep.service.UserService;
import cn.edu.scnu.momentkeep.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/checkin")
public class CheckinController {
    
    @Autowired
    private CheckinService checkinService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 保存打卡记录
     */
    @PostMapping
    public Result<Checkin> saveCheckin(@RequestBody Checkin checkin, Authentication authentication) {
        // 获取当前用户ID
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        Long userId = user.getId();
        
        checkin.setUserId(userId);
        checkin.setCheckinTime(LocalDateTime.now());
        
        Checkin savedCheckin = checkinService.saveCheckin(checkin);
        return Result.success(savedCheckin);
    }
    
    /**
     * 获取用户某天的打卡记录
     */
    @GetMapping("/by-date")
    public Result<List<Checkin>> getCheckinsByDate(@RequestParam String date, Authentication authentication) {
        // 获取当前用户ID
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        Long userId = user.getId();
        
        LocalDate checkinDate = LocalDate.parse(date);
        
        List<Checkin> checkins = checkinService.getCheckinsByDate(userId, checkinDate);
        return Result.success(checkins);
    }
    
    /**
     * 获取用户打卡统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getCheckinStats(Authentication authentication) {
        // 获取当前用户ID
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        Long userId = user.getId();
        
        Map<String, Object> stats = checkinService.getCheckinStats(userId);
        return Result.success(stats);
    }
    
    /**
     * 检查用户某天是否已打卡
     */
    @GetMapping("/has-checked-in")
    public Result<Boolean> hasCheckedIn(@RequestParam String type, @RequestParam String date, Authentication authentication) {
        // 获取当前用户ID
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        Long userId = user.getId();
        
        LocalDate checkinDate = LocalDate.parse(date);
        
        boolean hasCheckedIn = checkinService.hasCheckedIn(userId, type, checkinDate);
        return Result.success(hasCheckedIn);
    }
    
    /**
     * 删除打卡记录
     */
    @DeleteMapping
    public Result<?> deleteCheckin(@RequestParam String type, @RequestParam String date, Authentication authentication) {
        // 获取当前用户ID
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        Long userId = user.getId();
        
        LocalDate checkinDate = LocalDate.parse(date);
        checkinService.deleteCheckin(userId, type, checkinDate);
        
        return Result.success("取消打卡成功");
    }
    
    /**
     * 获取打卡时间分布
     */
    @GetMapping("/time-distribution")
    public Result<List<Map<String, Object>>> getTimeDistribution(
            @RequestParam String type,
            @RequestParam(required = false) String subType,
            Authentication authentication) {
        // 获取当前用户ID
        String username = authentication.getName();
        User user = userService.getByUsername(username);
        Long userId = user.getId();
        
        List<Map<String, Object>> distribution = checkinService.getTimeDistribution(userId, type, subType);
        return Result.success(distribution);
    }
}
