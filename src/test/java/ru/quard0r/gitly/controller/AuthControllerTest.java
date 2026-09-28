package ru.quard0r.gitly.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.quard0r.gitly.dto.request.LoginRequest;
import ru.quard0r.gitly.dto.request.RegisterRequest;
import ru.quard0r.gitly.dto.response.AuthResponse;
import ru.quard0r.gitly.exception.CredentialsAlreadyExistsException;
import ru.quard0r.gitly.exception.InvalidCredentialsException;
import ru.quard0r.gitly.service.AuthService;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

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
}