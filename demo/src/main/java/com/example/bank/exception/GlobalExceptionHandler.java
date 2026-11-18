package com.example.bank.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
		Map<String, String> errors = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors()
				.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return error(HttpStatus.BAD_REQUEST, "Request validation failed", errors);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
		return error(HttpStatus.CONFLICT, "A record with the supplied unique value already exists", Map.of());
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiError> handleAuthentication(AuthenticationException exception) {
		return error(HttpStatus.UNAUTHORIZED, "Invalid email or password", Map.of());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
	}

	@ExceptionHandler(AccountNotFoundException.class)
	public ResponseEntity<ApiError> handleAccountNotFound(AccountNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
	}

	@ExceptionHandler(ResourceAccessDeniedException.class)
	public ResponseEntity<ApiError> handleResourceAccessDenied(ResourceAccessDeniedException exception) {
		return error(HttpStatus.FORBIDDEN, exception.getMessage(), Map.of());
	}

	@ExceptionHandler(InsufficientBalanceException.class)
	public ResponseEntity<ApiError> handleInsufficientBalance(InsufficientBalanceException exception) {
		return error(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
	}

	@ExceptionHandler(DailyLimitExceededException.class)
	public ResponseEntity<ApiError> handleDailyLimitExceeded(DailyLimitExceededException exception) {
		return error(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception) {
		return error(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception) {
		HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
		return error(status, exception.getReason() == null ? status.getReasonPhrase() : exception.getReason(),
				Map.of());
	}

	private ResponseEntity<ApiError> error(HttpStatus status, String message,
										   Map<String, String> validationErrors) {
		ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
				message, validationErrors);
		return ResponseEntity.status(status).body(body);
	}
}
