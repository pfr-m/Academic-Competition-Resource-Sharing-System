package com.competition.hub.auth;

import com.competition.hub.store.DomainModels;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService2 {
//    签发时用key给token盖章(签名)
//    验证时使用同一个key进行验证
    private final SecretKey key;
    private final long expirationMinutes;
//去配置文件里面找相关的变量解析出来
    public JwtService2(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
//        加密算法只认字节，
//        jwt 库（io.jsonwebtoken.security.Keys）的工具方法，把字节数组包装成一个标准 SecretKey：
//         根据字节数组构造 HMAC 密钥对象
//2. 检查密钥长度——HS256 要求密钥至少 32 字节（256 位），不够会直接抛异常（这就是 README 里写 at-least-32-bytes 的原因）
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }
    public String createToken(AuthPrincipal principal){
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(principal.id()))
                .claim("role",principal.role().name())
                .issuedAt(Date.from((now)))
                .expiration(Date.from(now.plusSeconds(expirationMinutes*60)))
                .signWith(key)
                .compact();
    }
//    下面是解析了
    public AuthPrincipal parseToken(String token){
        Claims clamis = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AuthPrincipal(
                Long.parseLong(clamis.getSubject()),
                clamis.get("username",String.class),
                DomainModels.Role.valueOf(clamis.get("role",String.class))
        );
    }
}
