package cn.edu.scnu.momentkeep.security;

import cn.edu.scnu.momentkeep.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 登录用户主体
 *
 * <p>在 Spring Security 原生 {@link org.springframework.security.core.userdetails.User}
 * 基础上补充业务字段：</p>
 * <ul>
 *   <li>{@code userId}：控制器可直接从 SecurityContext 取到用户 ID，无需再查一次库；</li>
 *   <li>{@code tokenVersion}：JWT 中的版本号与之比对，实现登出/改密后的令牌吊销；</li>
 *   <li>账号状态 {@code status != 1} 映射为 {@code enabled = false}，注销账号无法继续使用旧令牌。</li>
 * </ul>
 */
public class LoginUser extends org.springframework.security.core.userdetails.User {

    private static final long serialVersionUID = 1L;

    /** 业务用户 ID */
    private final Long userId;

    /** 令牌版本号，与 JWT 中的 tv 声明比对 */
    private final Integer tokenVersion;

    public LoginUser(User user) {
        super(
                user.getUsername(),
                user.getPassword(),
                isActive(user),   // enabled：注销/禁用账号为 false
                true,             // accountNonExpired
                true,             // credentialsNonExpired
                true,             // accountNonLocked
                buildAuthorities(user.getRole())
        );
        this.userId = user.getId();
        this.tokenVersion = user.getTokenVersion();
    }

    private static boolean isActive(User user) {
        return user.getStatus() != null && user.getStatus() == 1;
    }

    private static Collection<? extends GrantedAuthority> buildAuthorities(String role) {
        String normalized = (role == null || role.isBlank()) ? "USER" : role.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + normalized));
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getTokenVersion() {
        return tokenVersion == null ? 0 : tokenVersion;
    }

    /**
     * 便捷判断，避免控制器到处强转
     */
    public static LoginUser from(UserDetails userDetails) {
        return userDetails instanceof LoginUser loginUser ? loginUser : null;
    }
}
