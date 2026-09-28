package ru.quard0r.gitly.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.quard0r.gitly.dto.response.AuthResponse;
import ru.quard0r.gitly.dto.request.LoginRequest;
import ru.quard0r.gitly.dto.request.RegisterRequest;
import ru.quard0r.gitly.entity.Person;
import ru.quard0r.gitly.entity.Role;
import ru.quard0r.gitly.exception.CredentialsAlreadyExistsException;
import ru.quard0r.gitly.exception.InvalidCredentialsException;
import ru.quard0r.gitly.exception.UserNotFoundException;
import ru.quard0r.gitly.exception.base.ServerException;
import ru.quard0r.gitly.repository.PersonRepository;
import ru.quard0r.gitly.repository.RoleRepository;
import ru.quard0r.gitly.security.JwtService;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PersonRepository personRepository;
    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (personRepository.existsByEmail(request.email())) {
            throw new CredentialsAlreadyExistsException("Email already taken");
        }
        if (personRepository.existsByUsername(request.username())) {
            throw new CredentialsAlreadyExistsException("Username already taken");
        }
        Role personRole = roleRepository.findByRoleName("ROLE_USER")
                .orElseThrow(() -> new ServerException("Default role ROLE_USER not found"));

        Person person = Person.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(Set.of(personRole))
                .build();

        personRepository.save(person);
        return issueTokens(person);
    }

    public AuthResponse login(LoginRequest request) {
        Person person = personRepository
                .findByEmailOrUsername(request.login(), request.login())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), person.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        return issueTokens(person);
    }

    public AuthResponse refresh(String refreshToken) {
        String username = refreshTokenService.findUsernameByToken(refreshToken)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        Person person = personRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String newRefreshToken = refreshTokenService.rotate(refreshToken, username);

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(person))
                .refreshToken(newRefreshToken)
                .build();
    }

    private AuthResponse issueTokens
            (Person person) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(person))
                .refreshToken(refreshTokenService.createRefreshToken(person.getUsername()))
                .build();
    }
}
