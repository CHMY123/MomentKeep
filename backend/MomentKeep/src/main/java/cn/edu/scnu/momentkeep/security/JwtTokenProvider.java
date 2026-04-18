/**
 * MomentKeep 朝暮记 - JWT令牌提供者
 *
 * @description 提供JWT令牌的生成、验证和解析功能
 * @author MomentKeep Team
 * @since 2026-04-18
 */
package cn.edu.scnu.momentkeep.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT令牌工具类
 * 负责令牌的生成、验证和用户信息提取
 */
@Component
public class JwtTokenProvider {

    /** JWT签名密钥 */
    @Value("${jwt.secret.key}")
    private String jwtSecret;

    /** JWT过期时间（毫秒） */
    @Value("${jwt.expiration.time}")
    private Long jwtExpiration;

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
     * @param userDetails 用户详情
     * @return JWT令牌字符串
     */
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .claim("sub", userDetails.getUsername())
                .claim("iat", now)
                .claim("exp", expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从令牌中提取用户名
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * 验证令牌有效性
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
