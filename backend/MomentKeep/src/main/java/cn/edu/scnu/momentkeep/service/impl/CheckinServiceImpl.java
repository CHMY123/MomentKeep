package cn.edu.scnu.momentkeep.service.impl;

import cn.edu.scnu.momentkeep.common.PageResult;
import cn.edu.scnu.momentkeep.entity.Checkin;
import cn.edu.scnu.momentkeep.mapper.CheckinMapper;
import cn.edu.scnu.momentkeep.service.CheckinService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CheckinServiceImpl implements CheckinService {

    /** 时间分布分桶数：每 2 小时一个桶，共 12 个 */
    private static final int TIME_BUCKET_COUNT = 12;

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
    
    /**
     * 历史打卡记录
     *
     * <p>原先使用 {@code .last("LIMIT " + limit)} 拼接 SQL，存在注入隐患；
     * 改为分页插件查询（limit 已由控制器收敛到 1~200）。</p>
     */
    @Override
    public List<Checkin> getHistoryCheckins(Long userId, int limit) {
        LambdaQueryWrapper<Checkin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Checkin::getUserId, userId)
                .orderByDesc(Checkin::getCheckinTime);

        Page<Checkin> page = new Page<>(1, limit);
        return checkinMapper.selectPage(page, queryWrapper).getRecords();
    }

    /**
     * 打卡时间分布汇总（全量历史，或指定日期区间）
     *
     * <p>此前前端只能取最近 30 条记录在客户端聚合，「历史分布」名不副实。
     * 这里把聚合下推到数据库（{@code GROUP BY HOUR(checkin_time)}），
     * 只回传 12 个时段分桶，避免把全部明细拉进内存。</p>
     *
     * <p>除「次数」外，同时给出「打卡率」所需的分子分母：
     * {@code COUNT(DISTINCT DATE(checkin_time))} 取每个时段的「有打卡天数」，
     * 再除以区间内的活跃天数。次数量的是「打得多不多」，打卡率量的是「坚持得久不久」，
     * 两者互相独立，折线与柱状因此各表一义，而不是同一份数据画两遍。</p>
     *
     * @param userId    当前用户
     * @param startDate 起始日期（含），可为 null 表示不限
     * @param endDate   结束日期（含），可为 null 表示不限
     * @return total / activeDays / buckets / typeCounts / typeBuckets / bucketActiveDays / typeBucketDays
     */
    @Override
    public Map<String, Object> getTimeDistributionSummary(Long userId, LocalDate startDate, LocalDate endDate) {
        // 查询 1：按「小时 × 类型」一次分组，同时取次数与有打卡天数（两个维度一次扫描即可拿到）
        QueryWrapper<Checkin> typeWrapper = new QueryWrapper<>();
        typeWrapper.select("HOUR(checkin_time) AS hour_of_day", "type AS type",
                        "COUNT(*) AS cnt", "COUNT(DISTINCT DATE(checkin_time)) AS day_cnt")
                .eq("user_id", userId)
                .groupBy("HOUR(checkin_time)", "type");
        applyDateRange(typeWrapper, startDate, endDate);

        int[] bucketCounts = new int[TIME_BUCKET_COUNT];
        Map<String, Integer> typeCounts = new LinkedHashMap<>();
        Map<String, int[]> typeBucketCounts = new LinkedHashMap<>();
        Map<String, int[]> typeBucketDays = new LinkedHashMap<>();
        int total = 0;

        for (Map<String, Object> row : checkinMapper.selectMaps(typeWrapper)) {
            int hour = toInt(row.get("hour_of_day"));
            int count = toInt(row.get("cnt"));
            int days = toInt(row.get("day_cnt"));
            String type = row.get("type") == null ? "unknown" : row.get("type").toString();

            int index = Math.min(Math.max(hour, 0) / 2, TIME_BUCKET_COUNT - 1);
            bucketCounts[index] += count;
            typeCounts.merge(type, count, Integer::sum);
            typeBucketCounts.computeIfAbsent(type, k -> new int[TIME_BUCKET_COUNT])[index] += count;
            // 该 (小时, 类型) 只有一行，直接落位即可，不需要累加
            typeBucketDays.computeIfAbsent(type, k -> new int[TIME_BUCKET_COUNT])[index] = days;
            total += count;
        }

        // 查询 2：全类型口径下每个时段的有打卡天数。
        // 必须单独查：同一天在 8 点既早起又用餐，按类型相加会被算成 2 天，去重必须在"全部类型"层面做。
        QueryWrapper<Checkin> dayWrapper = new QueryWrapper<>();
        dayWrapper.select("HOUR(checkin_time) AS hour_of_day", "COUNT(DISTINCT DATE(checkin_time)) AS day_cnt")
                .eq("user_id", userId)
                .groupBy("HOUR(checkin_time)");
        applyDateRange(dayWrapper, startDate, endDate);

        int[] bucketActiveDays = new int[TIME_BUCKET_COUNT];
        for (Map<String, Object> row : checkinMapper.selectMaps(dayWrapper)) {
            int index = Math.min(Math.max(toInt(row.get("hour_of_day")), 0) / 2, TIME_BUCKET_COUNT - 1);
            bucketActiveDays[index] += toInt(row.get("day_cnt"));
        }

        // 查询 3：区间内的活跃天数（有任意打卡的天数），作为打卡率的分母。
        // 用「活跃天数」而非「自然天数」：全部历史口径下跨度可能几年，用自然天数会让比率低到没有意义。
        QueryWrapper<Checkin> activeWrapper = new QueryWrapper<>();
        activeWrapper.select("COUNT(DISTINCT DATE(checkin_time)) AS active_days").eq("user_id", userId);
        applyDateRange(activeWrapper, startDate, endDate);

        List<Map<String, Object>> activeRows = checkinMapper.selectMaps(activeWrapper);
        int activeDays = activeRows.isEmpty() ? 0 : toInt(activeRows.get(0).get("active_days"));

        List<Map<String, Object>> buckets = new ArrayList<>(TIME_BUCKET_COUNT);
        for (int i = 0; i < TIME_BUCKET_COUNT; i++) {
            Map<String, Object> bucket = new LinkedHashMap<>(2);
            bucket.put("label", bucketLabel(i));
            bucket.put("count", bucketCounts[i]);
            buckets.add(bucket);
        }

        Map<String, Object> result = new LinkedHashMap<>(7);
        result.put("total", total);
        result.put("activeDays", activeDays);
        result.put("buckets", buckets);
        result.put("typeCounts", typeCounts);
        result.put("typeBuckets", typeBucketCounts);
        result.put("bucketActiveDays", bucketActiveDays);
        result.put("typeBucketDays", typeBucketDays);
        return result;
    }

    /** 时段标签，如 0 -> "00-02" */
    private String bucketLabel(int index) {
        return String.format("%02d-%02d", index * 2, index * 2 + 2);
    }

    /** 给查询附加日期区间条件（endDate 含当天，故用「< 次日零点」） */
    private void applyDateRange(QueryWrapper<Checkin> wrapper, LocalDate startDate, LocalDate endDate) {
        if (startDate != null) {
            wrapper.ge("checkin_time", startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.lt("checkin_time", endDate.plusDays(1).atStartOfDay());
        }
    }

    /**
     * 分页查询历史打卡记录（支持日期区间与类型筛选）
     */
    @Override
    public PageResult<Checkin> getHistoryPage(Long userId, int page, int size,
                                              LocalDate startDate, LocalDate endDate,
                                              String type, String subType) {
        LambdaQueryWrapper<Checkin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Checkin::getUserId, userId);

        if (startDate != null) {
            wrapper.ge(Checkin::getCheckinTime, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.lt(Checkin::getCheckinTime, endDate.plusDays(1).atStartOfDay());
        }
        // type 为空或 all 时表示不限类型
        if (StringUtils.hasText(type) && !"all".equalsIgnoreCase(type)) {
            wrapper.eq(Checkin::getType, type);
        }
        // 子类型：用餐对应 mealType，运动对应 exerciseType，其余类型不受影响
        if (StringUtils.hasText(subType) && !"all".equalsIgnoreCase(subType)) {
            wrapper.and(w -> w.eq(Checkin::getMealType, subType)
                    .or()
                    .eq(Checkin::getExerciseType, subType));
        }
        wrapper.orderByDesc(Checkin::getCheckinTime);

        Page<Checkin> result = checkinMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(),
                (int) result.getCurrent(), (int) result.getSize());
    }

    /** 数据库返回值可能是 Long/Integer/BigDecimal，统一转 int */
    private static int toInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
