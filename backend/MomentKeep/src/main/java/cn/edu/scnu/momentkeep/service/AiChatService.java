package cn.edu.scnu.momentkeep.service;

import java.util.List;
import java.util.Map;

public interface AiChatService {

    List<Map<String, String>> getChatHistory(Long userId);

    void saveChatHistory(Long userId, String messages);

    String chat(Long userId, String message);

    void clearChatHistory(Long userId);
}