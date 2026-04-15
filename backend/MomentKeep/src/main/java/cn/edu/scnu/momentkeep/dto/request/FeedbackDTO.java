package cn.edu.scnu.momentkeep.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FeedbackDTO {
    
    @NotBlank(message = "反馈内容不能为空")
    private String content;
    
    private String contact;
}
