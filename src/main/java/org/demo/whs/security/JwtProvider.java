package org.demo.whs.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;

@Service
@Slf4j
public class JwtProvider {

//    @Value("${app.jwt.secret}")
    private String secretKey = "p1zs6HE4bebUL6aVjJhSEtj/Bp4rhQPK3vUR2gpcA06muNhYO77Z9kzb3U5sDXBkdpEZwECJfm0tNuDRiVu29g==";

//    @Value("${app.jwt.access-token-expiry}"
    private long accessTokenExpiry = 900000; // 15 minutes

//    @Value("${app.jwt.refresh-token-expiry}")
    private long refreshTokenExpiry = 604800000; // 7 days

    /**
     * Builds a JWT access token for the given account.
     *
     * @param account the account for which to build the token
     * @return the generated JWT access token
     */
    public String buildAccessToken(Account account) {
        log.info("Generating access token for user: {}", account.getUsername());
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(account.getUsername())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + accessTokenExpiry))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Builds a JWT refresh token for the given username.
     *
     * @param username the username for which to build the token
     * @return the generated JWT refresh token
     */
    public String buildRefreshToken(String username) {
        log.info("Generating refresh token for user: {}", username);
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + refreshTokenExpiry))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the username from the given JWT token.
     *
     * @param token the JWT token
     * @return the username extracted from the token
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Generates a signing key for JWT using the configured secret key.
     *
     * @return the generated SecretKey
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
}
