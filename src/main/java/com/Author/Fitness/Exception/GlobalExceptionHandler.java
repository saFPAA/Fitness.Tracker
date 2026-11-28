package com.Author.Fitness.Exception;

import com.Author.Fitness.Auth.InvalidRefreshTokenException;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException e){
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.NOT_FOUND.value(), e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Resource not found"));
    }

    @ExceptionHandler(EntityExistsException.class)
    public ResponseEntity<ErrorResponse> handleEntityExists(EntityExistsException e){
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.CONFLICT.value(), e.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e){
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.error(message);
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        "Validation failed"
                ));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException e){
        String message = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error(message);
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e){
        log.error("Access denied: {}", e.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Access denied"),
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e){
        log.error("Data integrity violation", e);
        return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.CONFLICT.value(), "Operation violates data integrity constraints"),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException e){
        log.error("Authentication failed: bad credentials");
        return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Invalid username or password"),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e){
        log.error("Authentication failed", e);
        return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Authentication failed"),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException e){
        log.error("Refresh token rejected: {}", e.getMessage());
        return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), e.getMessage()),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e){
        log.error("Unexpected error occurred", e);
        return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Something went wrong"),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCurrentPassword(
            InvalidCurrentPasswordException e
    ) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(400, e.getMessage()));
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyAttempts(
            TooManyAttemptsException e
    ) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(e.getRetryAfterSeconds()))
                .body(new ErrorResponse(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        e.getMessage()
                ));
    }

    @ExceptionHandler(PasswordValidationException.class)
    public ResponseEntity<ErrorResponse> handlePasswordValidation(
            PasswordValidationException e
    ) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(400, e.getMessage()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e
    ) {
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse(405, "Method not allowed"));
    }
}
