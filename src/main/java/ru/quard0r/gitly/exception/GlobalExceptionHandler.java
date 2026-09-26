package ru.quard0r.gitly.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.quard0r.gitly.dto.response.ErrorResponse;
import ru.quard0r.gitly.exception.base.HttpException;
import ru.quard0r.gitly.exception.base.ServerException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    private ResponseEntity<ErrorResponse> exceptionHandler(HttpException e) {
        return new ResponseEntity<>(new ErrorResponse(e.getMessage()), e.getHttpStatus());
    }
}
