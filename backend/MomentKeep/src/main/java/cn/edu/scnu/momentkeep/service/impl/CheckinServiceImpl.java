package cn.edu.scnu.momentkeep.service.impl;

import cn.edu.scnu.momentkeep.entity.Checkin;
import cn.edu.scnu.momentkeep.mapper.CheckinMapper;
import cn.edu.scnu.momentkeep.service.CheckinService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CheckinServiceImpl implements CheckinService {
    
    @Autowired
    private CheckinMapper checkinMapper;
    
    @Override
    public Checkin saveCheckin(Checkin checkin) {
        checkinMapper.insert(checkin);
        return checkin;
    }
    
    @Override
    public List<Checkin> getCheckinsByDate(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        LambdaQueryWrapper<Checkin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Checkin::getUserId, userId)
                .ge(Checkin::getCheckinTime, startOfDay)
                .le(Checkin::getCheckinTime, endOfDay);
        
        return checkinMapper.selectList(queryWrapper);
    }
    
    @Override
    public Map<String, Object> getCheckinStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 计算最近30天的打卡数据
        LocalDate now = LocalDate.now();
        LocalDate thirtyDaysAgo = now.minusDays(30);
        
        LocalDateTime startOfPeriod = thirtyDaysAgo.atStartOfDay();
        LocalDateTime endOfPeriod = now.atTime(LocalTime.MAX);
        
        // 统计各类型打卡次数
        LambdaQueryWrapper<Checkin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Checkin::getUserId, userId)
                .ge(Checkin::getCheckinTime, startOfPeriod)
                .le(Checkin::getCheckinTime, endOfPeriod);
        
        List<Checkin> checkins = checkinMapper.selectList(queryWrapper);
        
        int earlyCount = 0;
        int sleepCount = 0;
        int mealCount = 0;
        int exerciseCount = 0;
        
        for (Checkin checkin : checkins) {
            String type = checkin.getType();
            switch (type) {
                case "early":
                    earlyCount++;
                    break;
                case "sleep":
                    sleepCount++;
                    break;
                case "meal":
                    mealCount++;
                    break;
                case "exercise":
                    exerciseCount++;
                    break;
            }
        }
        
        // 计算打卡率
        int daysInPeriod = 30;
        double earlyRate = (double) earlyCount / daysInPeriod * 100;
        double sleepRate = (double) sleepCount / daysInPeriod * 100;
        double exerciseRate = (double) exerciseCount / daysInPeriod * 100;
        // 计算餐食打卡率（每天3餐）
        double mealRate = (double) mealCount / (daysInPeriod * 3) * 100;
        
        stats.put("earlyRate", Math.round(earlyRate));
        stats.put("sleepRate", Math.round(sleepRate));
        stats.put("mealCount", mealCount / daysInPeriod);
        stats.put("mealRate", Math.round(mealRate));
        stats.put("exerciseRate", Math.round(exerciseRate));
        
        return stats;
    }
    
    @Override
    public boolean hasCheckedIn(Long userId, String type, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        LambdaQueryWrapper<Checkin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Checkin::getUserId, userId)
                .eq(Checkin::getType, type)
                .ge(Checkin::getCheckinTime, startOfDay)
                .le(Checkin::getCheckinTime, endOfDay);
        
        return checkinMapper.selectCount(queryWrapper) > 0;
    }
    
    @Override
    public void deleteCheckin(Long userId, String type, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        
        LambdaQueryWrapper<Checkin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Checkin::getUserId, userId)
                .eq(Checkin::getType, type)
                .ge(Checkin::getCheckinTime, startOfDay)
                .le(Checkin::getCheckinTime, endOfDay);
        
        checkinMapper.delete(queryWrapper);
    }
    
    @Override
    public List<Map<String, Object>> getTimeDistribution(Long userId, String type, String subType) {
        // 定义时间区间
        String[][] timeRanges = {
            {"00:00-02:00", "0", "2"},
            {"02:00-04:00", "2", "4"},
            {"04:00-06:00", "4", "6"},
            {"06:00-08:00", "6", "8"},
            {"08:00-10:00", "8", "10"},
            {"10:00-12:00", "10", "12"},
            {"12:00-14:00", "12", "14"},
            {"14:00-16:00", "14", "16"},
            {"16:00-18:00", "16", "18"},
            {"18:00-20:00", "18", "20"},
            {"20:00-22:00", "20", "22"},
            {"22:00-24:00", "22", "24"}
        };
        
        // 初始化结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (String[] range : timeRanges) {
            Map<String, Object> item = new HashMap<>();
            item.put("timeRange", range[0]);
            item.put("count", 0);
            result.add(item);
        }
        
        // 获取用户的所有打卡记录
        LambdaQueryWrapper<Checkin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Checkin::getUserId, userId)
                .eq(Checkin::getType, type);
        
        // 如果指定了子类型
        if (subType != null && !subType.equals("all")) {
            if (type.equals("meal")) {
                if (subType.equals("other")) {
                    // 其他用餐类型
                    queryWrapper.notIn(Checkin::getMealType, "早餐", "午餐", "晚餐", "宵夜");
                } else {
                    queryWrapper.eq(Checkin::getMealType, subType);
                }
            } else if (type.equals("exercise")) {
                if (subType.equals("other")) {
                    // 其他运动类型
                    queryWrapper.notIn(Checkin::getExerciseType, "跑步", "健身", "瑜伽", "游泳");
                } else {
                    queryWrapper.eq(Checkin::getExerciseType, subType);
                }
            }
        }
        
        List<Checkin> checkins = checkinMapper.selectList(queryWrapper);
        
        // 统计每个时间区间的打卡次数
        for (Checkin checkin : checkins) {
            LocalTime time = checkin.getCheckinTime().toLocalTime();
            int hour = time.getHour();
            
            for (int i = 0; i < timeRanges.length; i++) {
                int startHour = Integer.parseInt(timeRanges[i][1]);
                int endHour = Integer.parseInt(timeRanges[i][2]);
                
                if (hour >= startHour && hour < endHour) {
                    Map<String, Object> item = result.get(i);
                    item.put("count", (int) item.get("count") + 1);
                    break;
                }
            }
        }
        
        return result;
    }
}
