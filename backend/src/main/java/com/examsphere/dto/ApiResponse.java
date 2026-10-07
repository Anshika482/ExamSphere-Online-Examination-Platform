package com.examsphere.dto;

/** Uniform success envelope: { "message": "...", "data": ... }. */
public record ApiResponse<T>(String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("OK", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(message, data);
    }

    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(message, null);
    }
}
