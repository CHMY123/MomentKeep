package cn.edu.scnu.momentkeep.controller;

import cn.edu.scnu.momentkeep.common.Result;
import cn.edu.scnu.momentkeep.entity.User;
import cn.edu.scnu.momentkeep.service.AiChatService;
import cn.edu.scnu.momentkeep.service.UserService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiChatService aiChatService;
    private final UserService userService;

    public AiChatController(AiChatService aiChatService, UserService userService) {
        this.aiChatService = aiChatService;
        this.userService = userService;
    }

    @PostMapping("/chat")
    public Result<String> chat(@RequestBody ChatRequest request) {
        User currentUser = userService.getCurrentUser();
        String response = aiChatService.chat(currentUser.getId(), request.getMessage());
        return Result.success(response);
    }

    @GetMapping("/history")
    public Result<List<Map<String, String>>> getHistory() {
        User currentUser = userService.getCurrentUser();
        return Result.success(aiChatService.getChatHistory(currentUser.getId()));
    }

    @DeleteMapping("/clear")
    public Result<Void> clearHistory() {
        User currentUser = userService.getCurrentUser();
        aiChatService.clearChatHistory(currentUser.getId());
        return Result.success();
    }

    @Data
    public static class ChatRequest {
        private String message;
    }
}