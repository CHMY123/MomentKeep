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
@TableName("checkin")
public class Checkin {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;

    /** 打卡类型：early / sleep / meal / exercise */
    @Size(max = 20, message = "打卡类型不合法")
    private String type;
    
    private LocalDateTime checkinTime;

    /** 用餐类型（含用户自定义） */
    @Size(max = 50, message = "用餐类型不能超过 50 个字符")
    private String mealType;

    /** 运动类型（含用户自定义） */
    @Size(max = 50, message = "运动类型不能超过 50 个字符")
    private String exerciseType;

    @Size(max = 50, message = "自定义类型不能超过 50 个字符")
    private String customType;

    /** 备注：此前无长度上限，可提交超长字符串 */
    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String note;
    
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @Version
    private Integer version;
}
