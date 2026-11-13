package com.mobile.rssreader.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 应用偏好。数据量很小（两个开关），用 SharedPreferences 足够；
 * 业务数据（订阅源、文章）才走 Room，二者职责不混。
 */
public final class SettingsPreferences {

    private static final String FILE = "yueliu_settings";
    private static final String KEY_AUTO_REFRESH = "auto_refresh";
    private static final String KEY_NOTIFY = "notify_new_articles";
    /** 通知权限被拒过一次，用于设置页展示「已关闭通知」而不是反复弹窗 */
    private static final String KEY_NOTIFY_DENIED = "notify_denied";
    /** 伴生模块：每日自动备份 */
    private static final String KEY_AUTO_BACKUP = "auto_backup";

    private SettingsPreferences() {
    }

    private static SharedPreferences sp(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** 默认开启后台刷新 */
    public static boolean isAutoRefreshEnabled(Context context) {
        return sp(context).getBoolean(KEY_AUTO_REFRESH, true);
    }

    public static void setAutoRefreshEnabled(Context context, boolean enabled) {
        sp(context).edit().putBoolean(KEY_AUTO_REFRESH, enabled).apply();
    }

    public static boolean isNotifyEnabled(Context context) {
        return sp(context).getBoolean(KEY_NOTIFY, true);
    }

    public static void setNotifyEnabled(Context context, boolean enabled) {
        sp(context).edit().putBoolean(KEY_NOTIFY, enabled).apply();
    }

    public static void setNotifyDenied(Context context, boolean denied) {
        sp(context).edit().putBoolean(KEY_NOTIFY_DENIED, denied).apply();
    }

    public static boolean isNotifyDenied(Context context) {
        return sp(context).getBoolean(KEY_NOTIFY_DENIED, false);
    }

    public static boolean isAutoBackupEnabled(Context context) {
        return sp(context).getBoolean(KEY_AUTO_BACKUP, false);
    }

    public static void setAutoBackupEnabled(Context context, boolean enabled) {
        sp(context).edit().putBoolean(KEY_AUTO_BACKUP, enabled).apply();
    }
}
