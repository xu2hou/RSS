package com.mobile.rssreader;

import android.app.Application;

import com.mobile.rssreader.backup.BackupScheduler;
import com.mobile.rssreader.work.RefreshScheduler;

/**
 * 应用入口：登记后台定时刷新。
 * 放在 Application 而不是 Activity，保证冷启动、从通知进入等路径都能生效。
 */
public class YueLiuApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        RefreshScheduler.apply(this);
        BackupScheduler.apply(this);
    }
}
