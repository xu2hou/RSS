package com.mobile.rssreader.util;

/**
 * 统一的结果包装：把「成功数据 / 失败原因」一起往上抛，
 * 让 ViewModel 只用关心三态 UI 的渲染，不用写 try-catch。
 */
public final class Result<T> {

    public final T data;
    public final String error;

    private Result(T data, String error) {
        this.data = data;
        this.error = error;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(data, null);
    }

    public static <T> Result<T> failure(String error) {
        return new Result<>(null, error);
    }

    public boolean isSuccess() {
        return error == null;
    }
}
