package cn.edu.scnu.momentkeep.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI 对话每日配额
 *
 * <p>按 (user_id, quota_date) 唯一，天然按自然日重置。
 * 全局配额通过对该表 {@code quota_date} 求和得到，无需额外表。</p>
 */
@Data
@TableName("ai_chat_quota")
public class AiChatQuota {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 配额所属自然日 */
    private LocalDate quotaDate;

    /** 当日已使用次数 */
    private Integer usedCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
