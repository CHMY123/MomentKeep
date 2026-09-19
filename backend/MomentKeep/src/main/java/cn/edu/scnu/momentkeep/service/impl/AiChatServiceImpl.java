package cn.edu.scnu.momentkeep.service.impl;

import cn.edu.scnu.momentkeep.common.BusinessException;
import cn.edu.scnu.momentkeep.entity.AiChat;
import cn.edu.scnu.momentkeep.mapper.AiChatMapper;
import cn.edu.scnu.momentkeep.service.AiChatService;
import cn.edu.scnu.momentkeep.service.AiQuotaService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    /** 每次上送给模型的最大历史消息条数（不含 system），防止上下文无上限膨胀 */
    private static final int MAX_CONTEXT_MESSAGES = 10;

    /** 本地最多保留的消息条数（含 system 与 assistant），避免单行 JSON 无限增长 */
    private static final int MAX_STORED_MESSAGES = 40;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

    private static final String SYSTEM_PROMPT =
            "你是MomentKeep的AI助手，一个温柔、贴心的小助手。你可以帮助用户记录日常生活、追踪健康习惯（打卡）、"
                    + "管理待办事项、设置倒计时等。你的回答应该简洁、温暖、有帮助。";

    private final AiChatMapper aiChatMapper;
    private final AiQuotaService aiQuotaService;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${deepseek.api.key}")
    private String deepseekApiKey;

    @Value("${deepseek.api.url}")
    private String deepseekApiUrl;

    public AiChatServiceImpl(AiChatMapper aiChatMapper,
                             AiQuotaService aiQuotaService,
                             OkHttpClient okHttpClient) {
        this.aiChatMapper = aiChatMapper;
        this.aiQuotaService = aiQuotaService;
        this.okHttpClient = okHttpClient;
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
            log.error("解析 AI 会话历史失败：userId={}", userId, e);
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
            aiChatMapper.insert(newChat);
        }
    }

    /**
     * AI 对话
     *
     * <p>配额在调用前原子扣减（防止并发绕过），只有真正调用外部服务失败时才归还，
     * 这样"超额"与"失败"对用户是两种不同的体验。</p>
     */
    @Override
    public String chat(Long userId, String message) {
        // 步骤 1：扣减配额（超限会抛 AiQuotaExceededException → HTTP 429）
        aiQuotaService.consume(userId);

        try {
            // 步骤 2：只携带最近若干条历史，控制 token 消耗
            List<Map<String, String>> recentMessages = loadRecentMessages(userId);

            if (recentMessages.isEmpty()) {
                recentMessages.add(buildMessage("system", SYSTEM_PROMPT));
            }
            recentMessages.add(buildMessage("user", message));

            // 步骤 3：调用外部服务（失败即抛异常）
            String response = callDeepSeekApi(recentMessages);

            recentMessages.add(buildMessage("assistant", response));
            saveMessages(userId, recentMessages);

            return response;
        } catch (Exception e) {
            log.error("AI 对话失败，已归还本次配额：userId={}", userId, e);
            aiQuotaService.refund(userId);
            throw new BusinessException("AI 助手暂时不可用，请稍后再试");
        }
    }

    private List<Map<String, String>> loadRecentMessages(Long userId) {
        List<Map<String, String>> history = getChatHistory(userId);
        if (history.size() <= MAX_CONTEXT_MESSAGES) {
            return new ArrayList<>(history);
        }
        return new ArrayList<>(history.subList(history.size() - MAX_CONTEXT_MESSAGES, history.size()));
    }

    private void saveMessages(Long userId, List<Map<String, String>> messages) {
        // 落库前裁剪，避免单条 JSON 随使用时间无限增长
        List<Map<String, String>> toStore = messages.size() > MAX_STORED_MESSAGES
                ? new ArrayList<>(messages.subList(messages.size() - MAX_STORED_MESSAGES, messages.size()))
                : messages;
        try {
            saveChatHistory(userId, objectMapper.writeValueAsString(toStore));
        } catch (JsonProcessingException e) {
            log.error("序列化 AI 会话历史失败：userId={}", userId, e);
        }
    }

    private Map<String, String> buildMessage(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    /**
     * 调用 DeepSeek Chat Completions
     *
     * @throws IOException 网络异常、非 2xx 响应或响应体结构异常
     */
    private String callDeepSeekApi(List<Map<String, String>> messages) throws IOException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", messages);
        requestBody.put("stream", false);

        Request request = new Request.Builder()
                .url(deepseekApiUrl + "/chat/completions")
                .post(RequestBody.create(objectMapper.writeValueAsString(requestBody), JSON_MEDIA_TYPE))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + deepseekApiKey)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String body = response.body() == null ? "" : response.body().string();

            if (!response.isSuccessful()) {
                // 记录响应体才可能定位到 401（密钥失效）/ 429（余额或限频）等问题
                log.error("DeepSeek 调用失败：status={}, body={}", response.code(), body);
                throw new IOException("DeepSeek 返回状态码 " + response.code());
            }

            return extractContent(body);
        }
    }

    private String extractContent(String responseBody) throws IOException {
        Map<String, Object> responseMap;
        try {
            responseMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("DeepSeek 响应不是合法 JSON：{}", responseBody);
            throw new IOException("DeepSeek 响应解析失败", e);
        }

        Object choicesObj = responseMap.get("choices");
        if (choicesObj instanceof List<?> choicesList && !choicesList.isEmpty()
                && choicesList.get(0) instanceof Map<?, ?> choiceMap) {
            Object messageObj = choiceMap.get("message");
            if (messageObj instanceof Map<?, ?> messageMap) {
                Object contentObj = messageMap.get("content");
                if (contentObj instanceof String content && !content.isBlank()) {
                    return content;
                }
            }
        }
        log.error("DeepSeek 响应缺少 choices[0].message.content：{}", responseBody);
        throw new IOException("DeepSeek 响应结构异常");
    }

    @Override
    public void clearChatHistory(Long userId) {
        QueryWrapper<AiChat> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        aiChatMapper.delete(wrapper);
    }
}
