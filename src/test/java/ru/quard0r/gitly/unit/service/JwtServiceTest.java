package ru.quard0r.gitly.unit.service;

import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.quard0r.gitly.entity.Person;
import ru.quard0r.gitly.entity.Role;
import ru.quard0r.gitly.security.JwtService;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService")
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        String secret = "change-me-super-secret-key-at-least-32-bytes-long!!";
        SecretKey jwtSecretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        jwtService = new JwtService(jwtSecretKey);
        ReflectionTestUtils.setField(jwtService, "accessExpiration", 900_000L);
    }

    @Test
    @DisplayName("should generate valid access token")
    void shouldGenerateValidToken() {
        Person person = Person.builder()
                .email("john@example.com")
                .username("john")
                .roles(Set.of(Role.builder().roleName("ROLE_USER").build()))
                .build();

        String token = jwtService.generateAccessToken(person);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("should extract username correctly")
    void shouldExtractUsername() {
        Person person = Person.builder()
                .email("test@example.com")
                .username("test")
                .roles(Set.of())
                .build();

        String token = jwtService.generateAccessToken(person);

        assertThat(jwtService.extractUsername(token)).isEqualTo("test@example.com");
    }
}