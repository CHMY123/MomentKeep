package cn.edu.scnu.momentkeep.mapper;

import cn.edu.scnu.momentkeep.entity.AiChatQuota;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

/**
 * AI 配额数据访问层
 *
 * <p>扣减与归还全部使用「带条件的 UPDATE + 判断影响行数」实现，
 * 不依赖先查后改，因此在并发请求下也不会超额。</p>
 */
@Mapper
public interface AiChatQuotaMapper extends BaseMapper<AiChatQuota> {

    /**
     * 幂等初始化当日配额行（已存在则忽略），避免首次请求时并发插入报错
     */
    @Insert("INSERT IGNORE INTO ai_chat_quota (user_id, quota_date, used_count, create_time, update_time) "
            + "VALUES (#{userId}, #{quotaDate}, 0, NOW(), NOW())")
    int ensureRow(@Param("userId") Long userId, @Param("quotaDate") LocalDate quotaDate);

    /**
     * 原子扣减一次配额
     *
     * @return 影响行数，1 表示扣减成功，0 表示已达上限
     */
    @Update("UPDATE ai_chat_quota SET used_count = used_count + 1, update_time = NOW() "
            + "WHERE user_id = #{userId} AND quota_date = #{quotaDate} AND used_count < #{limit}")
    int tryConsume(@Param("userId") Long userId,
                   @Param("quotaDate") LocalDate quotaDate,
                   @Param("limit") int limit);

    /**
     * 归还一次配额（外部 AI 服务调用失败时使用）
     */
    @Update("UPDATE ai_chat_quota SET used_count = GREATEST(used_count - 1, 0), update_time = NOW() "
            + "WHERE user_id = #{userId} AND quota_date = #{quotaDate} AND used_count > 0")
    int refund(@Param("userId") Long userId, @Param("quotaDate") LocalDate quotaDate);

    /**
     * 当日全局已用次数（所有账号求和），用于兜底保护 API 额度
     */
    @Select("SELECT COALESCE(SUM(used_count), 0) FROM ai_chat_quota WHERE quota_date = #{quotaDate}")
    Long sumUsedByDate(@Param("quotaDate") LocalDate quotaDate);

    /**
     * 查询单个用户当日已用次数
     */
    @Select("SELECT COALESCE(used_count, 0) FROM ai_chat_quota "
            + "WHERE user_id = #{userId} AND quota_date = #{quotaDate}")
    Integer selectUsedCount(@Param("userId") Long userId, @Param("quotaDate") LocalDate quotaDate);
}
