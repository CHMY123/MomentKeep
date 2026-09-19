package cn.edu.scnu.momentkeep.controller;

import cn.edu.scnu.momentkeep.common.Result;
import cn.edu.scnu.momentkeep.service.AiChatService;
import cn.edu.scnu.momentkeep.service.AiQuotaService;
import cn.edu.scnu.momentkeep.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI 助手")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final AiQuotaService aiQuotaService;
    private final UserService userService;

    /**
     * 发起对话
     *
     * <p>返回体中带上剩余次数，前端无需再发一次请求即可刷新"今日剩余 N 次"。</p>
     */
    @PostMapping("/chat")
    @Operation(summary = "AI 对话（每日限次）")
    public Result<Map<String, Object>> chat(@RequestBody @Valid ChatRequest request) {
        Long userId = userService.getCurrentUserId();

        String reply = aiChatService.chat(userId, request.getMessage().trim());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reply", reply);
        data.put("quota", aiQuotaService.getStatus(userId));
        return Result.success(data);
    }

    /**
     * 查询当前用户今日 AI 配额
     */
    @GetMapping("/quota")
    @Operation(summary = "查询 AI 对话剩余次数")
    public Result<Map<String, Object>> getQuota() {
        return Result.success(aiQuotaService.getStatus(userService.getCurrentUserId()));
    }

    @GetMapping("/history")
    @Operation(summary = "获取会话历史")
    public Result<List<Map<String, String>>> getHistory() {
        return Result.success(aiChatService.getChatHistory(userService.getCurrentUserId()));
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清空会话历史")
    public Result<Void> clearHistory() {
        aiChatService.clearChatHistory(userService.getCurrentUserId());
        return Result.success();
    }

    /**
     * 对话请求体
     *
     * <p>消息长度限制是防刷的第一道闸门：超长输入会成倍放大 token 消耗。</p>
     */
    @Data
    public static class ChatRequest {

        @NotBlank(message = "消息内容不能为空")
        @Size(max = 500, message = "单条消息不能超过 500 个字符")
        private String message;
    }
}
