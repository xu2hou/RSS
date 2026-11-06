package com.mobile.rssreader.util;

/**
 * 一次性事件包装：Toast / Snackbar 这类只应消费一次的数据，
 * 用 LiveData 直接存会在旋转屏幕后被重放，包一层可避免重复提示。
 */
public class Event<T> {

    private final T content;
    private boolean handled;

    public Event(T content) {
        this.content = content;
    }

    /** 只在未被消费时返回内容，消费后返回 null */
    public T getContentIfNotHandled() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }

    public T peek() {
        return content;
    }
}
