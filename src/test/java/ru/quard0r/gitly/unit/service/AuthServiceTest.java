package ru.quard0r.gitly.unit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.quard0r.gitly.dto.request.LoginRequest;
import ru.quard0r.gitly.dto.request.RegisterRequest;
import ru.quard0r.gitly.dto.response.AuthResponse;
import ru.quard0r.gitly.entity.Person;
import ru.quard0r.gitly.entity.Role;
import ru.quard0r.gitly.exception.CredentialsAlreadyExistsException;
import ru.quard0r.gitly.exception.InvalidCredentialsException;
import ru.quard0r.gitly.exception.UserNotFoundException;
import ru.quard0r.gitly.exception.base.ServerException;
import ru.quard0r.gitly.repository.PersonRepository;
import ru.quard0r.gitly.repository.RoleRepository;
import ru.quard0r.gitly.security.JwtService;
import ru.quard0r.gitly.service.AuthService;
import ru.quard0r.gitly.service.RefreshTokenService;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
public class AuthServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private Person person;
    private Role role;

    @BeforeEach
    void setUp() {
        role = Role.builder()
                .id(1L)
                .roleName("ROLE_USER")
                .build();

        person = Person.builder()
                .id(1L)
                .username("john")
                .email("john@example.com")
                .password("encodedPassword")
                .roles(Set.of(role)).build();
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should register new user and return token")
        void shouldRegisterSuccessfully() {
            RegisterRequest request = new RegisterRequest("john", "john@example.com", "pass123");

            when(personRepository.existsByEmail(request.email())).thenReturn(false);
            when(personRepository.existsByUsername(request.username())).thenReturn(false);
            when(roleRepository.findByRoleName("ROLE_USER")).thenReturn(Optional.of(role));
            when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
            when(personRepository.save(any(Person.class))).thenReturn(person);
            when(jwtService.generateAccessToken(any(Person.class))).thenReturn("access-token");
            when(refreshTokenService.createRefreshToken(request.username())).thenReturn("refresh-token");

            AuthResponse response = authService.register(request);

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");

            verify(personRepository).save(argThat(p ->
                    p.getEmail().equals("john@example.com") &&
                    p.getUsername().equals("john") &&
                    p.getRoles().contains(role)
            ));
        }

        @Test
        @DisplayName("should throw when email exists")
        void shouldThrowWhenEmailExists() {
            RegisterRequest request = new RegisterRequest("john","john@example.com","pass123");

            when(personRepository.existsByEmail(request.email())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(CredentialsAlreadyExistsException.class);
        }

        @Test
        @DisplayName("should throw when login exists")
        void shouldThrowWhenUsernameExists() {
            RegisterRequest request = new RegisterRequest("john", "john@example.com","pass123");
            when(personRepository.existsByEmail(request.email())).thenReturn(false);
            when(personRepository.existsByUsername(request.email())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(CredentialsAlreadyExistsException.class);
        }

        @Test
        @DisplayName("should throw when default role not found")
        void shouldThrowWhenRoleNotFound() {
            RegisterRequest request = new RegisterRequest("john", "john@example.com", "password123");

            when(personRepository.existsByEmail(request.email())).thenReturn(false);
            when(personRepository.existsByUsername(request.username())).thenReturn(false);
            when(roleRepository.findByRoleName("ROLE_USER")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(ServerException.class);
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("should login successfully by email")
        void shouldLoginByEmail() {
            LoginRequest request = new LoginRequest("john@example.com", "password123");

            when(personRepository.findByEmailOrUsername(request.login(), request.login()))
                    .thenReturn(Optional.of(person));
            when(passwordEncoder.matches(request.password(), person.getPassword())).thenReturn(true);
            when(jwtService.generateAccessToken(person)).thenReturn("access-token");
            when(refreshTokenService.createRefreshToken(person.getEmail())).thenReturn("refresh-token");

            AuthResponse response = authService.login(request);

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
        }

        @Test
        @DisplayName("should login successfully by login")
        void shouldLoginByUsername() {
            LoginRequest request = new LoginRequest("john", "password123");

            when(personRepository.findByEmailOrUsername(request.login(), request.login()))
                    .thenReturn(Optional.of(person));
            when(passwordEncoder.matches(request.password(), person.getPassword())).thenReturn(true);
            when(jwtService.generateAccessToken(person)).thenReturn("access-token");
            when(refreshTokenService.createRefreshToken(person.getEmail())).thenReturn("refresh-token");

            AuthResponse response = authService.login(request);

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            LoginRequest request = new LoginRequest("unknown", "password123");

            when(personRepository.findByEmailOrUsername(request.login(), request.login()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("should throw when password is wrong")
        void shouldThrowWhenPasswordWrong() {
            LoginRequest request = new LoginRequest("john@example.com", "wrong-password");

            when(personRepository.findByEmailOrUsername(request.login(), request.login()))
                    .thenReturn(Optional.of(person));
            when(passwordEncoder.matches(request.password(), person.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("should refresh successfully")
        void shouldRefreshSuccessfully() {
            String oldRefreshToken = "old-refresh-token";

            when(refreshTokenService.findUsernameByToken(oldRefreshToken)).thenReturn(Optional.of(person.getUsername()));
            when(personRepository.findByUsername(person.getUsername())).thenReturn(Optional.of(person));
            when(refreshTokenService.rotate(oldRefreshToken, person.getUsername())).thenReturn("new-refresh-token");
            when(jwtService.generateAccessToken(person)).thenReturn("new-access-token");

            AuthResponse response = authService.refresh(oldRefreshToken);

            assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
            assertThat(response.accessToken()).isEqualTo("new-access-token");
        }

        @Test
        @DisplayName("should throw when refresh token is invalid")
        void shouldThrowOnInvalidRefreshToken() {
            when(refreshTokenService.findUsernameByToken("invalid")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("invalid"))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        @DisplayName("should throw when user not found during refresh")
        void shouldThrowWhenUserNotFoundOnRefresh() {
            String token = "valid-token";

            when(refreshTokenService.findUsernameByToken(token))
                    .thenReturn(Optional.of("john"));
            when(personRepository.findByUsername("john")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(token))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}
