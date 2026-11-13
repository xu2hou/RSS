package com.mobile.rssreader.backup;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.mobile.rssreader.data.prefs.SettingsPreferences;

import java.util.concurrent.TimeUnit;

/**
 * 自动备份的调度入口：每天一次，唯一任务名避免重复登记。
 */
public final class BackupScheduler {

    public static final String WORK_NAME = "daily_auto_backup";
    public static final long INTERVAL_HOURS = 24L;

    private BackupScheduler() {
    }

    public static void apply(Context context) {
        if (SettingsPreferences.isAutoBackupEnabled(context)) {
            schedule(context);
        } else {
            cancel(context);
        }
    }

    public static void schedule(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                AutoBackupWorker.class, INTERVAL_HOURS, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(WORK_NAME);
    }
}
