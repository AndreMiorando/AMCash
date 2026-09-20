package br.com.amcash.shared.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException exception) {

        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(
                        error.getField(),
                        error.getDefaultMessage()
                ));

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(BadRequestException exception) {
        return errorResponse(400, exception.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException exception) {
        return errorResponse(404, exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(ConflictException exception) {
        return errorResponse(409, exception.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(ForbiddenException exception) {
        return errorResponse(403, exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(
            UnauthorizedException exception) {

        return errorResponse(401, exception.getMessage());
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleServiceUnavailable(
            ServiceUnavailableException exception) {

        return errorResponse(503, exception.getMessage());
    }

    @ExceptionHandler(CaptchaRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleCaptchaRequired(
            CaptchaRequiredException exception) {

        Map<String, Object> error = new HashMap<>();
        error.put("message", exception.getMessage());
        error.put("captchaRequired", true);
        return ResponseEntity.status(403).body(error);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Map<String, String>> handleTooManyRequests(
            TooManyRequestsException exception) {

        return errorResponse(429, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedException(Exception exception) {
        return ResponseEntity.internalServerError()
                .body(Map.of("message", "Erro interno no servidor"));
    }

    private ResponseEntity<Map<String, String>> errorResponse(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
