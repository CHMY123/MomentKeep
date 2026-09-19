package cn.edu.scnu.momentkeep.service;

import java.util.List;
import java.util.Map;

public interface AiChatService {

    List<Map<String, String>> getChatHistory(Long userId);

    void saveChatHistory(Long userId, String messages);

    /**
     * 发起一次 AI 对话（会消耗一次每日配额）
     *
     * @param userId  用户 ID
     * @param message 用户消息
     * @return AI 回复内容
     */
    String chat(Long userId, String message);

    void clearChatHistory(Long userId);
}
