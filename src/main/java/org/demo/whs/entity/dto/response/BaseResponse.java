package org.demo.whs.entity.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * BaseResponse: A generic response wrapper for API responses.
 * @param <T> the type of the response data
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BaseResponse<T> {

    private Boolean success;
    private String errorCode;
    private String message;
    private T data;
    private List<FieldError> fieldErrors;
    private LocalDateTime timestamp;

    public static <T> BaseResponse<T> success(T data) {
        return BaseResponse.<T>builder()
                .success(true)
                .errorCode(null)
                .message("Success")
                .data(data)
                .fieldErrors(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> BaseResponse<T> success(T data, String message) {
        return BaseResponse.<T>builder()
                .success(true)
                .errorCode(null)
                .message(message)
                .data(data)
                .fieldErrors(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> BaseResponse<T> error(String errorCode, String message, List<FieldError> fieldErrors) {
        return BaseResponse.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .data(null)
                .fieldErrors(fieldErrors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

