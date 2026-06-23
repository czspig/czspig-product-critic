package com.czspig.productcritic.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    OK("OK", "success", HttpStatus.OK),
    VALIDATION_ERROR("VALIDATION_ERROR", "请求参数不合法", HttpStatus.BAD_REQUEST),
    NOT_FOUND("NOT_FOUND", "资源不存在", HttpStatus.NOT_FOUND),
    AI_PROVIDER_ERROR("AI_PROVIDER_ERROR", "AI 评审服务暂时不可用", HttpStatus.BAD_GATEWAY),
    DATABASE_ERROR("DATABASE_ERROR", "数据保存失败", HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_ERROR("INTERNAL_ERROR", "服务暂时不可用", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
