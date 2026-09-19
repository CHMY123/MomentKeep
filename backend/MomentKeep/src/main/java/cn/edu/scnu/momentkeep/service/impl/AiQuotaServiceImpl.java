package cn.edu.scnu.momentkeep.service.impl;

import cn.edu.scnu.momentkeep.common.AiQuotaExceededException;
import cn.edu.scnu.momentkeep.mapper.AiChatQuotaMapper;
import cn.edu.scnu.momentkeep.service.AiQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 配额实现
 *
 * <p>三层防滥用：</p>
 * <ol>
 *   <li>单用户每日上限（默认 3 次）——业务规则；</li>
 *   <li>全局每日上限（默认 200 次）——注册接口被批量刷账号时的兜底，保护钱包；</li>
 *   <li>Nginx 层 IP 限流 + 消息长度上限——阻断脚本化攻击（见部署文档）。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuotaServiceImpl implements AiQuotaService {

    /** 统一按北京时间切分自然日，避免服务器时区不同导致提前/延后重置 */
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final AiChatQuotaMapper quotaMapper;

    /** 单用户每日对话次数上限 */
    @Value("${ai.quota.daily-limit:3}")
    private int dailyLimit;

    /** 全局每日对话次数上限（所有用户合计） */
    @Value("${ai.quota.global-daily-limit:200}")
    private int globalDailyLimit;

    @Override
    public void consume(Long userId) {
        LocalDate date = today();

        Long globalUsed = quotaMapper.sumUsedByDate(date);
        if (globalUsed != null && globalDailyLimit > 0 && globalUsed >= globalDailyLimit) {
            log.warn("AI 全局配额已用尽：used={}, limit={}", globalUsed, globalDailyLimit);
            throw new AiQuotaExceededException("今日 AI 服务总次数已用尽，请明天再试");
        }

        quotaMapper.ensureRow(userId, date);

        int affected = quotaMapper.tryConsume(userId, date, dailyLimit);
        if (affected != 1) {
            throw new AiQuotaExceededException(
                    "今日 AI 对话次数已用完（每日 " + dailyLimit + " 次），明天再来吧");
        }
    }

    @Override
    public void refund(Long userId) {
        try {
            quotaMapper.refund(userId, today());
        } catch (Exception e) {
            // 归还失败不影响主流程，最多让用户少一次额度
            log.warn("归还 AI 配额失败：userId={}", userId, e);
        }
    }

    @Override
    public Map<String, Object> getStatus(Long userId) {
        LocalDate date = today();
        Integer used = quotaMapper.selectUsedCount(userId, date);
        int usedCount = used == null ? 0 : used;

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("date", date.toString());
        status.put("limit", dailyLimit);
        status.put("used", usedCount);
        status.put("remaining", Math.max(dailyLimit - usedCount, 0));
        return status;
    }

    private LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
