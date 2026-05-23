package cn.edu.scnu.momentkeep.service;

import cn.edu.scnu.momentkeep.entity.FocusRecord;
import java.util.List;
import java.util.Map;

public interface FocusService {
    FocusRecord saveRecord(FocusRecord record);
    List<FocusRecord> getTodayRecords(Long userId);
    Map<String, Object> getFocusStats(Long userId);
    Map<String, Object> getRecordsByTodoId(Long userId, Long todoId);
}
