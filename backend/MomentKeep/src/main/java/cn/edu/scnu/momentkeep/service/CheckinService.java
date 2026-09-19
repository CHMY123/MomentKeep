package cn.edu.scnu.momentkeep.service;

import cn.edu.scnu.momentkeep.common.PageResult;
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

    /**
     * 打卡时间分布汇总（12 个时段分桶）
     *
     * @param userId    当前用户
     * @param startDate 起始日期（含），null 表示不限
     * @param endDate   结束日期（含），null 表示不限
     * @return 含 total / buckets / typeCounts 的汇总结果
     */
    Map<String, Object> getTimeDistributionSummary(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取历史打卡记录
     */
    List<Checkin> getHistoryCheckins(Long userId, int limit);

    /**
     * 分页查询历史打卡记录（支持日期区间与类型筛选）
     *
     * @param userId    当前用户
     * @param page      页码（从 1 开始）
     * @param size      每页条数
     * @param startDate 起始日期（含），null 表示不限
     * @param endDate   结束日期（含），null 表示不限
     * @param type      打卡类型，null/空 表示全部
     * @param subType   子类型（用餐/运动的细分），null/空 表示不限
     * @return 分页结果
     */
    PageResult<Checkin> getHistoryPage(Long userId, int page, int size,
                                       LocalDate startDate, LocalDate endDate,
                                       String type, String subType);
}
