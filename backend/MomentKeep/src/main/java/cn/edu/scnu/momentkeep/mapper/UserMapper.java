package cn.edu.scnu.momentkeep.mapper;

import cn.edu.scnu.momentkeep.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface UserMapper extends BaseMapper<User> {

    @Update("UPDATE user SET avatar = #{avatar}, update_time = NOW() WHERE id = #{id}")
    void updateAvatarById(@Param("id") Long id, @Param("avatar") String avatar);

    /**
     * 自增令牌版本号，使该用户已签发的所有 JWT 立即失效
     *
     * @return 影响行数
     */
    @Update("UPDATE user SET token_version = COALESCE(token_version, 0) + 1, update_time = NOW() WHERE id = #{id}")
    int incrementTokenVersion(@Param("id") Long id);
}
