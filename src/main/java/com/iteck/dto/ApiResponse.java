package com.iteck.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.iteck.util.ApiStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 포함하지 않음
public class ApiResponse<T> {
    private int statusCode;
    private String message;
    private T data;

    public static ApiResponse<Void> fromResultStatus(ApiStatus status) {
        return new ApiResponse<>(
                status.getStatusCode(),
                status.getMessage(),
                null // metaData는 null
        );
    }

    public static <T> ApiResponse<T> fromResultStatus(ApiStatus status, T data) {
        return new ApiResponse<>(
                status.getStatusCode(),
                status.getMessage(),
                data // 메타데이터가 존재하는 경우만 포함
        );
    }

}
