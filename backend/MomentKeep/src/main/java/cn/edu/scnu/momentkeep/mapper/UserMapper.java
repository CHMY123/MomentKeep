package cn.edu.scnu.momentkeep.mapper;

import cn.edu.scnu.momentkeep.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Update;

public interface UserMapper extends BaseMapper<User> {
    @Update("UPDATE user SET avatar = #{avatar}, update_time = NOW() WHERE id = #{id}")
    void updateAvatarById(Long id, String avatar);
}
