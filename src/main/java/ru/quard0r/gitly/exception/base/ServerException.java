package ru.quard0r.gitly.exception.base;

import org.springframework.http.HttpStatus;

public class ServerException extends HttpException {

    private static final HttpStatus STATUS = HttpStatus.INTERNAL_SERVER_ERROR;

    public ServerException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return STATUS;
    }
}
