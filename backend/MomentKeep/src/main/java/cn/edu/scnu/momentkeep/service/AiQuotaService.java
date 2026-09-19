package cn.edu.scnu.momentkeep.service;

import java.util.Map;

/**
 * AI 对话配额服务
 */
public interface AiQuotaService {

    /**
     * 原子扣减一次配额，超限抛 {@code AiQuotaExceededException}
     */
    void consume(Long userId);

    /**
     * 归还一次配额（AI 服务调用失败时调用）
     */
    void refund(Long userId);

    /**
     * 查询配额状态
     *
     * @return 包含 limit / used / remaining / date 的只读视图
     */
    Map<String, Object> getStatus(Long userId);
}
