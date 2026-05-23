package cn.edu.scnu.momentkeep.service.impl;

import cn.edu.scnu.momentkeep.entity.FocusRecord;
import cn.edu.scnu.momentkeep.mapper.FocusRecordMapper;
import cn.edu.scnu.momentkeep.service.FocusService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FocusServiceImpl implements FocusService {

    @Autowired
    private FocusRecordMapper focusRecordMapper;

    @Override
    public FocusRecord saveRecord(FocusRecord record) {
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        focusRecordMapper.insert(record);
        return record;
    }

    @Override
    public List<FocusRecord> getTodayRecords(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        QueryWrapper<FocusRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .ge("start_time", startOfDay)
                .lt("start_time", endOfDay)
                .orderByDesc("start_time");

        return focusRecordMapper.selectList(queryWrapper);
    }

    @Override
    public Map<String, Object> getFocusStats(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfWeekDateTime = startOfWeek.atStartOfDay();

        QueryWrapper<FocusRecord> todayQuery = new QueryWrapper<>();
        todayQuery.eq("user_id", userId)
                .ge("start_time", startOfToday);
        List<FocusRecord> todayRecords = focusRecordMapper.selectList(todayQuery);

        QueryWrapper<FocusRecord> weekQuery = new QueryWrapper<>();
        weekQuery.eq("user_id", userId)
                .ge("start_time", startOfWeekDateTime);
        List<FocusRecord> weekRecords = focusRecordMapper.selectList(weekQuery);

        QueryWrapper<FocusRecord> totalQuery = new QueryWrapper<>();
        totalQuery.eq("user_id", userId);
        List<FocusRecord> totalRecords = focusRecordMapper.selectList(totalQuery);

        int todayTotal = todayRecords.stream().mapToInt(FocusRecord::getDuration).sum();
        int weekTotal = weekRecords.stream().mapToInt(FocusRecord::getDuration).sum();
        int total = totalRecords.stream().mapToInt(FocusRecord::getDuration).sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayTotal", todayTotal);
        stats.put("weekTotal", weekTotal);
        stats.put("total", total);

        return stats;
    }

    @Override
    public Map<String, Object> getRecordsByTodoId(Long userId, Long todoId) {
        QueryWrapper<FocusRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("todo_id", todoId)
                .orderByDesc("start_time");
        
        List<FocusRecord> records = focusRecordMapper.selectList(queryWrapper);
        
        int totalDuration = records.stream().mapToInt(FocusRecord::getDuration).sum();
        int recordCount = records.size();
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalDuration", totalDuration);
        result.put("recordCount", recordCount);
        result.put("records", records);
        
        return result;
    }
}
