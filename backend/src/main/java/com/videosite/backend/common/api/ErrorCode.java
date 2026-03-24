package com.videosite.backend.common.api;

/**
 * 通用错误码定义。
 */
public enum ErrorCode {

    SUCCESS(0, "ok"),
    BAD_REQUEST(40000, "请求参数错误"),
    UNAUTHORIZED(40100, "未认证或登录已失效"),
    FORBIDDEN(40300, "无权限访问"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "资源状态冲突"),
    VALIDATION_FAILED(42200, "参数校验失败"),
    INTERNAL_ERROR(50000, "服务器内部错误"),

    UPLOAD_FAILED(51001, "视频上传失败"),
    TRANSCODE_FAILED(51002, "视频转码失败"),
    PLAY_SOURCE_NOT_READY(51003, "播放源尚未就绪");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
