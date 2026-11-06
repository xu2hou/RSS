package com.mobile.rssreader.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 全局线程池。Java 项目里用 ExecutorService + 主线程 Handler 代替 Kotlin 协程：
 * <ul>
 *   <li>{@link #diskIO()} 单线程，保证数据库写入顺序</li>
 *   <li>{@link #networkIO()} 固定 3 线程，允许并发抓取多个源</li>
 *   <li>{@link #main(Runnable)} 把结果切回主线程更新 UI</li>
 * </ul>
 */
public final class AppExecutors {

    private static volatile AppExecutors instance;

    private final ExecutorService diskIO;
    private final ExecutorService networkIO;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AppExecutors() {
        diskIO = Executors.newSingleThreadExecutor(daemonFactory("disk-io"));
        networkIO = Executors.newFixedThreadPool(3, daemonFactory("network-io"));
    }

    public static AppExecutors get() {
        if (instance == null) {
            synchronized (AppExecutors.class) {
                if (instance == null) {
                    instance = new AppExecutors();
                }
            }
        }
        return instance;
    }

    public ExecutorService diskIO() {
        return diskIO;
    }

    public ExecutorService networkIO() {
        return networkIO;
    }

    /** 若当前不在主线程则投递到主线程执行 */
    public void main(Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            mainHandler.post(action);
        }
    }

    private static ThreadFactory daemonFactory(final String prefix) {
        final AtomicInteger counter = new AtomicInteger(1);
        return r -> {
            Thread t = new Thread(r, prefix + "-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
    }
}
