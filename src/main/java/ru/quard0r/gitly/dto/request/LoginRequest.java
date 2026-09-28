package ru.quard0r.gitly.dto.request;

public record LoginRequest(
        String login,
        String password
){ }
