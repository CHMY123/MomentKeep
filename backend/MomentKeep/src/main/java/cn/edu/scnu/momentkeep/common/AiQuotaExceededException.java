package cn.edu.scnu.momentkeep.common;

/**
 * AI 对话配额超限异常
 *
 * <p>由全局异常处理器映射为 HTTP 429，前端据此提示"今日次数已用完"，
 * 且不会触发登录态失效逻辑。</p>
 */
public class AiQuotaExceededException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AiQuotaExceededException(String message) {
        super(message);
    }
}
