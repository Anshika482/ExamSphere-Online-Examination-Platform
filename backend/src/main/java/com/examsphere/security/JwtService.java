package com.examsphere.security;

import com.examsphere.entity.User;
import com.examsphere.util.TokenUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Creates and validates the signed JWT access tokens (HMAC-SHA256). */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${app.jwt.expiration-ms:7200000}") long expirationMs) {
        String effectiveSecret = secret;
        if (!StringUtils.hasText(effectiveSecret)) {
            // No secret is ever hard-coded. Without JWT_SECRET a random one is generated for this run only.
            effectiveSecret = TokenUtil.generateToken() + TokenUtil.generateToken();
            log.warn("JWT_SECRET is not set. A temporary random secret is in use; all sessions end when the server restarts.");
        }
        // Hashing the configured secret always yields a 256-bit key, whatever its length.
        this.key = Keys.hmacShaKeyFor(TokenUtil.sha256Bytes(effectiveSecret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Returns the user id when the token is authentic and not expired. */
    public Optional<Long> extractUserId(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }
}
