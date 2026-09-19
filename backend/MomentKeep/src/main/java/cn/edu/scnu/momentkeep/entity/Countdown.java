package cn.edu.scnu.momentkeep.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("countdown")
public class Countdown {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;

    @Size(max = 100, message = "倒计时标题不能超过 100 个字符")
    private String title;

    @Size(max = 500, message = "倒计时描述不能超过 500 个字符")
    private String description;
    
    private LocalDateTime targetTime;

    @Size(max = 32, message = "颜色值不合法")
    private String color;

    @Min(value = 0, message = "排序值不合法")
    @Max(value = 9999, message = "排序值不合法")
    private Integer sortOrder;
    
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @Version
    private Integer version;
}
