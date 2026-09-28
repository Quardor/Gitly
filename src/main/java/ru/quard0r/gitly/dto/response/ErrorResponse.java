package ru.quard0r.gitly.dto.response;

public record ErrorResponse (
        String message,
        long timestamp
) {

    public ErrorResponse(String message) {
        this(message, System.currentTimeMillis());
    }
}
