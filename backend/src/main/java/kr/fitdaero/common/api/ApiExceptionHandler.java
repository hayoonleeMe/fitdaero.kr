package kr.fitdaero.common.api;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  private static final String VALIDATION_ERROR = "VALIDATION_ERROR";
  private static final String VALIDATION_MESSAGE = "요청값이 올바르지 않습니다.";

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(
      MethodArgumentNotValidException exception) {
    List<ApiErrorResponse.FieldError> fieldErrors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(
                error ->
                    new ApiErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
            .toList();
    return validationError(fieldErrors);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleUnreadableMessage() {
    return validationError(
        List.of(new ApiErrorResponse.FieldError("request", "요청 본문 형식이 올바르지 않습니다.")));
  }

  private ResponseEntity<ApiErrorResponse> validationError(
      List<ApiErrorResponse.FieldError> fieldErrors) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiErrorResponse(VALIDATION_ERROR, VALIDATION_MESSAGE, fieldErrors));
  }
}
