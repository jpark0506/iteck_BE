package com.iteck.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ApiStatus {

    SUC_EXPERIMENT_CREATE(201, "Experiment Created"),
    SUC_EXPERIMENT_READ(200, "Experiment Read"),
    SUC_EXPERIMENT_UPDATE(200, "Experiment Update"),
    SUC_EXPERIMENT_DELETE(200, "Experiment Delete"),

    SUC_FACTOR_CREATE(201, "Factor Created"),
    SUC_FACTOR_READ(200, "Factor Read"),
    SUC_FACTOR_UPDATE(200, "Factor Update"),
    SUC_FACTOR_DELETE(200, "Factor Delete"),

    // 400번대: 클라이언트 오류 응답
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    CONFLICT(409, "Conflict"),

    // 500번대: 서버 오류 응답
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    BAD_GATEWAY(502, "Bad Gateway"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable");



    private final int statusCode;
    private final String message;
}
