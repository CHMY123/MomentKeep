package cn.edu.scnu.momentkeep.service.impl;

import cn.edu.scnu.momentkeep.entity.AiChat;
import cn.edu.scnu.momentkeep.mapper.AiChatMapper;
import cn.edu.scnu.momentkeep.service.AiChatService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private final AiChatMapper aiChatMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${deepseek.api.key}")
    private String deepseekApiKey;

    @Value("${deepseek.api.url}")
    private String deepseekApiUrl;

    private static final String SYSTEM_PROMPT = "你是MomentKeep的AI助手，一个温柔、贴心的小助手。你可以帮助用户记录日常生活、追踪健康习惯（打卡）、管理待办事项、设置倒计时等。你的回答应该简洁、温暖、有帮助。";

    public AiChatServiceImpl(AiChatMapper aiChatMapper) {
        this.aiChatMapper = aiChatMapper;
    }

    @Override
    public List<Map<String, String>> getChatHistory(Long userId) {
        AiChat chat = getLatestChat(userId);
        if (chat == null || chat.getMessages() == null || chat.getMessages().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(chat.getMessages(), new TypeReference<List<Map<String, String>>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse messages: ", e);
            return new ArrayList<>();
        }
    }

    private AiChat getLatestChat(Long userId) {
        QueryWrapper<AiChat> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("create_time").last("LIMIT 1");
        return aiChatMapper.selectOne(wrapper);
    }

    @Override
    public void saveChatHistory(Long userId, String messages) {
        AiChat existingChat = getLatestChat(userId);
        if (existingChat != null) {
            existingChat.setMessages(messages);
            aiChatMapper.updateById(existingChat);
        } else {
            AiChat newChat = new AiChat();
            newChat.setUserId(userId);
            newChat.setMessages(messages);
            newChat.setCreateTime(LocalDateTime.now());
            aiChatMapper.insert(newChat);
        }
    }

    @Override
    public String chat(Long userId, String message) {
        try {
            List<Map<String, String>> messages = loadMessages(userId);

            if (messages.isEmpty()) {
                Map<String, String> systemMsg = new HashMap<>();
                systemMsg.put("role", "system");
                systemMsg.put("content", SYSTEM_PROMPT);
                messages.add(systemMsg);
            }

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", message);
            messages.add(userMsg);

            String response = callDeepSeekApi(messages);

            messages.add(createAssistantMessage(response));
            saveMessages(userId, messages);

            return response;
        } catch (Exception e) {
            log.error("AI chat error: ", e);
            return "抱歉，我现在有点累了，请稍后再试。";
        }
    }

    private List<Map<String, String>> loadMessages(Long userId) {
        return getChatHistory(userId);
    }

    private void saveMessages(Long userId, List<Map<String, String>> messages) {
        try {
            String messagesJson = objectMapper.writeValueAsString(messages);
            saveChatHistory(userId, messagesJson);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize messages: ", e);
        }
    }

    private Map<String, String> createAssistantMessage(String content) {
        Map<String, String> msg = new HashMap<>();
        msg.put("role", "assistant");
        msg.put("content", content);
        return msg;
    }

    private String callDeepSeekApi(List<Map<String, String>> messages) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", messages);
        requestBody.put("stream", false);

        MediaType mediaType = MediaType.parse("application/json");
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        RequestBody body = RequestBody.create(jsonBody, mediaType);
        Request request = new Request.Builder()
                .url(deepseekApiUrl + "/chat/completions")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + deepseekApiKey)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("DeepSeek API error: " + response);
                return "抱歉，AI服务暂时不可用。";
            }

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});

            // 安全处理类型转换
            Object choicesObj = responseMap.get("choices");
            if (choicesObj instanceof List<?>) {
                List<?> choicesList = (List<?>) choicesObj;
                if (!choicesList.isEmpty() && choicesList.get(0) instanceof Map<?, ?>) {
                    Map<?, ?> choiceMap = (Map<?, ?>) choicesList.get(0);
                    Object messageObj = choiceMap.get("message");
                    if (messageObj instanceof Map<?, ?>) {
                        Map<?, ?> messageMap = (Map<?, ?>) messageObj;
                        Object contentObj = messageMap.get("content");
                        if (contentObj instanceof String) {
                            return (String) contentObj;
                        }
                    }
                }
            }
            return "抱歉，我没有收到有效的回复。";
        }
    }

    @Override
    public void clearChatHistory(Long userId) {
        QueryWrapper<AiChat> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        aiChatMapper.delete(wrapper);
    }
}