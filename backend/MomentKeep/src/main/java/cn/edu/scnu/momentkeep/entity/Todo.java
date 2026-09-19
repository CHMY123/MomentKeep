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
@TableName("todo")
public class Todo {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;

    @Size(max = 100, message = "待办标题不能超过 100 个字符")
    private String title;

    @Size(max = 500, message = "待办描述不能超过 500 个字符")
    private String description;
    
    private Integer priority;
    
    private Boolean completed;
    
    private LocalDateTime completedTime;

    @Size(max = 500, message = "完成备注不能超过 500 个字符")
    private String completionNote;
    
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @Version
    private Integer version;
}
