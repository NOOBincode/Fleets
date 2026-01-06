package org.example.fleets.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
@Component
public class JwtUtils {

    @Value("${jwt.secret:jrcMVDHYt2haPsDGxDKambIjjXOKojLvKv6TycexYjo=}")
    private String secret;

    @Value("${jwt.expiration:86400}")
    private long expiration; // 默认24小时，单位：秒

    /**
     * -- GETTER --
     *  获取token头部名称
     *
     * @return token头部名称
     */
    @Getter
    @Value("${jwt.header:Authorization}")
    private String header;

    /**
     * -- GETTER --
     *  获取token前缀
     *
     * @return token前缀
     */
    @Getter
    @Value("${jwt.tokenHead:Bearer }")
    private String tokenHead;

    /**
     * 生成JWT token
     * @param userId 用户ID
     * @param username 用户名
     * @return JWT token
     */
    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        return generateToken(claims);
    }

    /**
     * 从token中获取用户ID
     * @param token JWT token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return Long.valueOf(claims.get("userId").toString());
    }

    /**
     * 从token中获取用户名
     * @param token JWT token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("username").toString();
    }

    /**
     * 验证token是否有效
     * @param token JWT token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 刷新token
     * @param token 原token
     * @return 新token
     */
    public String refreshToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return generateToken(claims);
    }

    /**
     * 获取token过期时间
     * @param token JWT token
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * 获取完整的token（添加tokenHead前缀）
     * @param token JWT token
     * @return 完整token
     */
    public String getFullToken(String token) {
        return tokenHead + token;
    }

    /**
     * 从完整token中提取原始token
     * @param fullToken 完整token
     * @return 原始token
     */
    public String getTokenFromFullToken(String fullToken) {
        if (fullToken != null && fullToken.startsWith(tokenHead)) {
            return fullToken.substring(tokenHead.length());
        }
        return null;
    }

    // 私有方法

    private String generateToken(Map<String, Object> claims) {
        Date createdDate = new Date();
        Date expirationDate = new Date(createdDate.getTime() + expiration * 1000);

        JwtBuilder builder = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(createdDate)
                .setExpiration(expirationDate)
                .signWith(getSignKey(), SignatureAlgorithm.HS256);

        return builder.compact();
    }

    private Key getSignKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims getClaimsFromToken(String token) {
        JwtParser parser = Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build();
        return parser.parseClaimsJws(token).getBody();
    }

    private boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public long getExpiration(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.getTime();
    }
}
