package kr.fitdaero.common.api;

import java.util.List;

public record ApiErrorResponse(String code, String message, List<FieldError> fieldErrors) {

  public record FieldError(String field, String message) {}
}
