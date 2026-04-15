package cn.edu.scnu.momentkeep.controller;

import cn.edu.scnu.momentkeep.common.Result;
import cn.edu.scnu.momentkeep.dto.request.FeedbackDTO;
import cn.edu.scnu.momentkeep.service.FeedbackService;
import cn.edu.scnu.momentkeep.common.PageResult;
import cn.edu.scnu.momentkeep.vo.FeedbackVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@Tag(name = "反馈管理")
@RequiredArgsConstructor
public class FeedbackController {
    
    private final FeedbackService feedbackService;
    
    @PostMapping
    @Operation(summary = "提交反馈")
    public Result<Void> submitFeedback(@RequestBody @Valid FeedbackDTO dto) {
        feedbackService.submitFeedback(dto);
        return Result.success();
    }
    
    @GetMapping("/my")
    @Operation(summary = "获取我的反馈")
    public Result<PageResult<FeedbackVO>> getMyFeedback(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageResult<FeedbackVO> feedback = feedbackService.getMyFeedback(page, size);
        return Result.success(feedback);
    }
}
