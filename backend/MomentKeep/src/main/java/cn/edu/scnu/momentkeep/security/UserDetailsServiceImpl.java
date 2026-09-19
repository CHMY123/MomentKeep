package cn.edu.scnu.momentkeep.security;

import cn.edu.scnu.momentkeep.entity.User;
import cn.edu.scnu.momentkeep.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 用户详情加载服务
 *
 * <p>返回带业务字段的 {@link LoginUser}，使认证过滤器和控制器都能直接拿到
 * userId 与令牌版本号，无需重复查库。</p>
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user;
        try {
            user = userService.getByUsername(username);
        } catch (RuntimeException e) {
            // 统一转换为 Spring Security 的语义异常，避免泄露内部异常栈
            throw new UsernameNotFoundException("用户不存在", e);
        }
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return new LoginUser(user);
    }
}
