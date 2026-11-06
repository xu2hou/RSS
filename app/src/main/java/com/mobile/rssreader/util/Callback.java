package com.mobile.rssreader.util;

/**
 * 极简回调：把后台线程结果送回 ViewModel。
 * （替代 Kotlin 协程里的 suspend / 回调 lambda）
 */
public interface Callback<T> {

    void onSuccess(T data);

    void onError(String message);
}
