package com.mobile.rssreader.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.mobile.rssreader.data.prefs.SettingsPreferences;
import com.mobile.rssreader.data.repo.FeedRepository;
import com.mobile.rssreader.notify.NotificationHelper;

/**
 * 后台刷新 Worker。
 * <p>
 * doWork 运行在 WorkManager 的工作线程，可以直接做同步的抓取与入库；
 * 抓取结果里若有新文章且用户开启了通知，则发一条汇总通知（无权限时静默降级）。
 */
public class RefreshWorker extends Worker {

    private static final String TAG = "RefreshWorker";

    public RefreshWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        try {
            FeedRepository.RefreshSummary summary = FeedRepository.get(context).refreshAllSync();
            Log.i(TAG, "后台刷新完成：" + summary.toMessage());

            if (summary.newArticles > 0 && SettingsPreferences.isNotifyEnabled(context)) {
                NotificationHelper.notifyNewArticles(context, summary.newArticles);
            }
            return Result.success();
        } catch (Exception e) {
            // 单次失败交给 WorkManager 的重试退避处理，不把整个任务标成永久失败
            Log.w(TAG, "后台刷新异常，稍后重试", e);
            return Result.retry();
        }
    }
}
