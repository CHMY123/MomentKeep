/**
 * MomentKeep 朝暮记 - JWT令牌提供者
 *
 * @description 提供JWT令牌的生成、验证和解析功能
 * @author MomentKeep Team
 * @since 2026-04-18
 */
package cn.edu.scnu.momentkeep.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT令牌工具类
 * 负责令牌的生成、验证和用户信息提取
 */
@Slf4j
@Component
public class JwtTokenProvider {

    /** 令牌中承载令牌版本号的声明名 */
    public static final String CLAIM_TOKEN_VERSION = "tv";

    /** JWT签名密钥 */
    @Value("${jwt.secret.key}")
    private String jwtSecret;

    /** JWT过期时间（毫秒） */
    @Value("${jwt.expiration.time}")
    private Long jwtExpiration;

    /** 签发方，用于防止不同系统间令牌串用 */
    @Value("${jwt.issuer:momentkeep}")
    private String issuer;

    /**
     * 获取签名密钥
     * @return HMAC-SHA密钥
     */
    private SecretKey getSigningKey() {
        if (jwtSecret == null) {
            throw new IllegalArgumentException("JWT secret key cannot be null");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 32 characters long");
        }
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成JWT令牌
     *
     * @param username     用户名
     * @param tokenVersion 用户当前的令牌版本号，登出/改密后版本自增即可让旧令牌失效
     * @return JWT令牌字符串
     */
    public String generateToken(String username, Integer tokenVersion) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(username)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expirationDate)
                .claim(CLAIM_TOKEN_VERSION, tokenVersion == null ? 0 : tokenVersion)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析令牌载荷（不校验过期以外的业务规则）
     *
     * @param token JWT令牌
     * @return 载荷
     * @throws JwtException 令牌非法或已过期
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从令牌中提取用户名
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 从令牌中提取令牌版本号
     * @param token JWT令牌
     * @return 令牌版本号，缺失时返回 0
     */
    public Integer getTokenVersion(String token) {
        Object value = parseClaims(token).get(CLAIM_TOKEN_VERSION);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * 验证令牌有效性
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 区分过期与非法，便于排查问题（不打印令牌内容）
            log.debug("JWT 校验失败：{}", e.getMessage());
            return false;
        }
    }
}
