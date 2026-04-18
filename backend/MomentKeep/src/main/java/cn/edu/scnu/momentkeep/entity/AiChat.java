package cn.edu.scnu.momentkeep.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_chat")
public class AiChat {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String messages;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}