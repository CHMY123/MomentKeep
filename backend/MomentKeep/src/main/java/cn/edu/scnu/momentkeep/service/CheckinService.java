package cn.edu.scnu.momentkeep.service;

import cn.edu.scnu.momentkeep.entity.Checkin;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CheckinService {
    /**
     * 保存打卡记录
     */
    Checkin saveCheckin(Checkin checkin);
    
    /**
     * 获取用户某天的打卡记录
     */
    List<Checkin> getCheckinsByDate(Long userId, LocalDate date);
    
    /**
     * 获取用户打卡统计数据
     */
    Map<String, Object> getCheckinStats(Long userId);
    
    /**
     * 检查用户某天是否已打卡
     */
    boolean hasCheckedIn(Long userId, String type, LocalDate date);
    
    /**
     * 删除打卡记录
     */
    void deleteCheckin(Long userId, String type, LocalDate date);
    
    /**
     * 获取打卡时间分布
     */
    List<Map<String, Object>> getTimeDistribution(Long userId, String type, String subType);
}
