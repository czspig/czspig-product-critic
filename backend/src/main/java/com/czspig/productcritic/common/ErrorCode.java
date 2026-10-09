package com.czspig.productcritic.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    UNAUTHORIZED("UNAUTHORIZED", "登录已失效，请重新登录", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "请求验证失败，请刷新页面", HttpStatus.FORBIDDEN),
    RATE_LIMITED("RATE_LIMITED", "操作过于频繁，请稍后重试", HttpStatus.TOO_MANY_REQUESTS),
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
