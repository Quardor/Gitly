package ru.quard0r.gitly.service;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import ru.quard0r.gitly.entity.Person;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    private static final String PREFIX = "refresh:";

    public String createRefreshToken(String username) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                PREFIX + token,
                username,
                Duration.ofMillis(refreshTokenExpiration)
        );
        return token;
    }

    public Optional<String> findEmailByToken(String token) {
        String email = redisTemplate.opsForValue().get(PREFIX + token);
        return Optional.ofNullable(email);
    }

    public void deleteByToken(String token) {
        redisTemplate.delete(PREFIX + token);
    }

    public String rotate(String oldToken, String username) {
        deleteByToken(oldToken);
        return createRefreshToken(username);
    }
}
