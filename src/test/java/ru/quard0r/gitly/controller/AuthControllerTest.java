package ru.quard0r.gitly.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.quard0r.gitly.dto.request.LoginRequest;
import ru.quard0r.gitly.dto.request.RegisterRequest;
import ru.quard0r.gitly.dto.response.AuthResponse;
import ru.quard0r.gitly.exception.CredentialsAlreadyExistsException;
import ru.quard0r.gitly.exception.InvalidCredentialsException;
import ru.quard0r.gitly.security.JwtService;
import ru.quard0r.gitly.service.AuthService;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/auth/register → 200 + tokens")
    void registerShouldReturnTokens() throws Exception {
        RegisterRequest request = new RegisterRequest("john", "john@example.com", "password123");
        AuthResponse response = new AuthResponse("access-token", "refresh-token");

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("POST /api/auth/register when email exists → 409")
    void registerShouldReturn409() throws Exception {
        RegisterRequest request = new RegisterRequest("john", "john@example.com", "password123");

        when(authService.register(any())).thenThrow(new CredentialsAlreadyExistsException("username already used"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/auth/register with invalid body → 400")
    void registerWithInvalidBodyShouldReturn400() throws Exception {
        RegisterRequest request = new RegisterRequest("", "not-an-email", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("POST /api/auth/login → 200 + tokens")
    void loginShouldReturnTokens() throws Exception {
        LoginRequest request = new LoginRequest("john@example.com", "password123");
        AuthResponse response = new AuthResponse("access-token", "refresh-token");

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("POST /api/auth/login with invalid credentials → 401")
    void loginShouldReturn401() throws Exception {
        LoginRequest request = new LoginRequest("john@example.com", "wrong");

        when(authService.login(any())).thenThrow(new InvalidCredentialsException("invalid password or username"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/login with blank fields → 400")
    void loginWithBlankFieldsShouldReturn400() throws Exception {
        LoginRequest request = new LoginRequest("", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("POST /api/auth/refresh → 200 + new tokens")
    void refreshShouldReturnNewTokens() throws Exception {
        AuthResponse response = new AuthResponse("new-access", "new-refresh");

        when(authService.refresh(anyString())).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "old-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh with invalid token → 401")
    void refreshShouldReturn401() throws Exception {
        when(authService.refresh(anyString())).thenThrow(new InvalidCredentialsException("invalid refresh token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "invalid"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/refresh with missing token → 400")
    void refreshWithMissingTokenShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(authService, never()).refresh(anyString());
    }

    @Test
    @DisplayName("POST /api/auth/logout → 204")
    void logoutShouldReturn204() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "some-token"))))
                .andExpect(status().isNoContent());

        verify(authService).logout("some-token");
    }

    @Test
    @DisplayName("POST /api/auth/logout with missing token → 400")
    void logoutWithMissingTokenShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(authService, never()).logout(anyString());
    }
}