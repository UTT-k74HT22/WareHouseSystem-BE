package org.demo.whs.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class JwtProvider {

    @Value("${app.jwt.secret}")
    private String secret;
    @Value("${app.jwt.access-token-expiration}")
    private Duration accessExpiration;
    @Value("${app.jwt.refresh-token-expiration}")
    private Duration refreshExpiration;
    @Value("${app.jwt.issuer:whs-api}")
    private String issuer;

    /**
     * Validate JWT configuration on startup
     */
    @PostConstruct
    public void init() {
        try {
            // Decode and validate secret key
            byte[] keyBytes = java.util.Base64.getDecoder().decode(secret);

            // HS512 requires at least 512 bits (64 bytes)
            if (keyBytes.length < 64) {
                throw new IllegalArgumentException(
                    "JWT secret key must be at least 512 bits (64 bytes) for HS512 algorithm. " +
                    "Current size: " + (keyBytes.length * 8) + " bits"
                );
            }

            log.info("JWT configuration validated successfully");
            log.info("Access token expiration: {}", accessExpiration);
            log.info("Refresh token expiration: {}", refreshExpiration);
            log.info("JWT Issuer: {}", issuer);

        } catch (IllegalArgumentException e) {
            log.error("Invalid JWT secret key configuration: {}", e.getMessage());
            throw new IllegalStateException("JWT secret key configuration is invalid. Please check application.yml", e);
        }
    }

    /**
     * Tạo access token với thông tin account và roles
     * @param account - thông tin tài khoản
     * @param roles - danh sách roles của user
     * @return access token
     */
    public String buildAccessToken(Account account, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("userId", account.getId());
        claims.put("type", "accessToken");
        return buildToken(claims, account.getUsername(), accessExpiration);
    }

    /**
     * Tạo refresh token
     * @param account - thông tin tài khoản
     * @return refresh token
     */
    public String buildRefreshToken(Account account) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", account.getId());
        claims.put("type", "refreshToken");
        return buildToken(claims, account.getUsername(), refreshExpiration);
    }

    /**
     * Get expiration time of access token
     * @param token - JWT access token
     * @return expiration time in milliseconds
     */
    public String getExpirationAccessToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Date expirationDate = claims.getExpiration();
        return String.valueOf(expirationDate.getTime());
    }

    /**
     * Get expiration time of refresh token
     * @param token - JWT refresh token
     * @return expiration time in milliseconds
     */
    public String getExpirationRefreshToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Date expirationDate = claims.getExpiration();
        return String.valueOf(expirationDate.getTime());
    }

    /**
     * Build token chung
     * @param claims - thông tin bổ sung
     * @param username - tên đăng nhập
     * @param expiration - thời gian hết hạn (milliseconds)
     * @return JWT token
     */
    private String buildToken(Map<String, Object> claims, String username, Duration expiration) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration.toMillis());
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .setIssuer(issuer)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Validate token
     * @param token - JWT token cần validate
     * @return true nếu token hợp lệ
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid token: {}", e.getMessage());
            return false;
        } catch (ExpiredJwtException e) {
            log.error("Token has expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.error("Tokens are not supported: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.error("Empty tokens: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.error("Invalid token signature: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Lấy username từ token
     * @param token - JWT token
     * @return username
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * Lấy userId từ token
     * @param token - JWT token
     * @return userId
     */
    public String getUserIdFromToken(String token) {
        return getClaimsFromToken(token).get("userId", String.class);
    }

    /**
     * Lấy roles từ token
     * @param token - JWT token
     * @return danh sách roles
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return (List<String>) claims.get("roles");
    }

    /**
     * Lấy tất cả claims từ token
     * @param token - JWT token
     * @return Claims object
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Kiểm tra token có phải refresh token không
     * @param token - JWT token
     * @return true nếu là refresh token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String type = claims.get("type", String.class);
            return "refreshToken".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get signing key from secret
     * @return SecretKey
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = java.util.Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}