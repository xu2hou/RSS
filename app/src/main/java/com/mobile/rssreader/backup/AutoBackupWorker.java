package com.mobile.rssreader.backup;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;

/**
 * 每日自动备份（伴生模块的后台能力）。
 * 备份纯粹是本地文件写入，不需要网络约束；失败时交给 WorkManager 退避重试。
 */
public class AutoBackupWorker extends Worker {

    private static final String TAG = "AutoBackupWorker";

    public AutoBackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            BackupManager manager = new BackupManager(getApplicationContext());
            File file = manager.backupToLocal();
            int pruned = manager.pruneOldBackups();
            Log.i(TAG, "自动备份完成：" + file.getName() + "，清理旧备份 " + pruned + " 份");
            return Result.success();
        } catch (Exception e) {
            Log.w(TAG, "自动备份失败，稍后重试", e);
            return Result.retry();
        }
    }
}
