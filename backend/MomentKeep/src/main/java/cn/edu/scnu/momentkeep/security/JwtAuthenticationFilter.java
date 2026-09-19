package cn.edu.scnu.momentkeep.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * JWT 认证过滤器
 *
 * <p>只在令牌通过「签名有效 + 账号未被注销 + 令牌版本号一致」三重校验时
 * 才写入 SecurityContext，任何一项不满足都保持匿名，由
 * {@code AuthenticationEntryPoint} 统一返回 401。</p>
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    // 加 @Lazy 打破 UserDetailsServiceImpl 与 UserService 之间的循环依赖
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, @Lazy UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = getTokenFromRequest(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            try {
                String username = jwtTokenProvider.getUsernameFromToken(token);
                Integer tokenVersion = jwtTokenProvider.getTokenVersion(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (!userDetails.isEnabled()) {
                    // 账号已注销/禁用：旧令牌一律不再放行
                    log.info("拒绝已注销账号的令牌访问：username={}, uri={}", username, request.getRequestURI());
                } else if (!isTokenVersionMatched(userDetails, tokenVersion)) {
                    log.info("令牌版本已失效（可能已登出或改密）：username={}", username);
                } else {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // 用户已被删除等异常场景，按未认证处理即可，避免出现 500
                log.warn("解析令牌失败，按未认证处理：{}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTokenVersionMatched(UserDetails userDetails, Integer tokenVersion) {
        LoginUser loginUser = LoginUser.from(userDetails);
        if (loginUser == null) {
            return true;
        }
        return Objects.equals(loginUser.getTokenVersion(), tokenVersion == null ? 0 : tokenVersion);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
