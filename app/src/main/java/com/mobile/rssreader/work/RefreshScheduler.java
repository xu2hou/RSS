package com.mobile.rssreader.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.mobile.rssreader.data.prefs.SettingsPreferences;

import java.util.concurrent.TimeUnit;

/**
 * 后台定时刷新的调度入口。
 * 唯一工作任务名 + {@link ExistingPeriodicWorkPolicy#UPDATE}，
 * 保证重复调用不会产生多个并行任务（这是 WorkManager 最常见的坑）。
 */
public final class RefreshScheduler {

    public static final String WORK_NAME = "periodic_feed_refresh";
    /** 刷新的周期；WorkManager 允许的最小周期是 15 分钟 */
    public static final long INTERVAL_HOURS = 6L;

    private RefreshScheduler() {
    }

    /** 按当前偏好决定是登记还是取消任务 */
    public static void apply(Context context) {
        if (SettingsPreferences.isAutoRefreshEnabled(context)) {
            schedule(context);
        } else {
            cancel(context);
        }
    }

    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
                // 只在联网时跑，避免断网时白白消耗一个周期
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                RefreshWorker.class, INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(WORK_NAME);
    }
}
