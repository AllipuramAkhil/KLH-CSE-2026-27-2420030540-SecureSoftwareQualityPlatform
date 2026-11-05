package com.assqp.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtService {

    private static final String INSECURE_DEFAULT_SECRET =
            "change-this-to-a-long-random-secret-value-in-production-min-256-bits";
    private static final String REFRESH_TOKEN_TYPE_CLAIM = "type";
    private static final String REFRESH_TOKEN_TYPE_VALUE = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpirationMs;

    private final Environment environment;

    public JwtService(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validateSecretOnStartup() {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (isProd && INSECURE_DEFAULT_SECRET.equals(secret)) {
            // Belt-and-suspenders on top of application-prod.properties requiring
            // JWT_SECRET with no fallback: refuse to boot rather than sign tokens
            // with a publicly-known key.
            throw new IllegalStateException(
                    "JWT_SECRET must be set to a real secret when running with the 'prod' profile. " +
                            "Refusing to start with the known insecure default value.");
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, accessTokenExpirationMs);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return buildToken(claims, userDetails, refreshTokenExpirationMs);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expirationMs) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Security fix: generateRefreshToken() stamps a "type=refresh" claim, but until now
     * nothing checked it, so an access token could be replayed at the /refresh endpoint
     * and still validate. Use this at the refresh endpoint instead of isTokenValid().
     */
    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        if (!isTokenValid(token, userDetails)) {
            return false;
        }
        Object type = extractClaim(token, claims -> claims.get(REFRESH_TOKEN_TYPE_CLAIM));
        return REFRESH_TOKEN_TYPE_VALUE.equals(type);
    }

    public boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}
