package cn.edu.scnu.momentkeep.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("focus_record")
public class FocusRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 专注模式：stopwatch / countdown / pomodoro */
    @Size(max = 20, message = "专注模式不合法")
    private String mode;

    private Integer duration;

    private Long todoId;

    @Size(max = 100, message = "关联待办标题不能超过 100 个字符")
    private String todoTitle;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Version
    private Integer version;
}
