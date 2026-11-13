package com.mobile.rssreader.notify;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.mobile.rssreader.R;
import com.mobile.rssreader.ui.MainActivity;

/**
 * 新文章通知。
 * <p>
 * 权限降级策略（对应课程要求的「用户拒绝权限后 App 仍能正常运行」）：
 * 没有通知权限时**不崩溃、不静默失败**，而是把开关状态如实反映到设置页，
 * 刷新与阅读功能完全不受影响。
 */
public final class NotificationHelper {

    public static final String CHANNEL_ID = "new_articles";
    private static final int NOTIFICATION_ID = 1001;

    private NotificationHelper() {
    }

    /** 是否有发通知的权限（13+ 需要运行时授权） */
    public static boolean canNotify(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notify_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.notify_channel_desc));
        manager.createNotificationChannel(channel);
    }

    /** 发现新文章时调用；无权限时直接返回，不抛异常 */
    public static void notifyNewArticles(Context context, int count) {
        if (count <= 0 || !canNotify(context)) {
            return;
        }
        ensureChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nav_feed)
                .setContentTitle(context.getString(R.string.notify_title))
                .setContentText(context.getString(R.string.notify_text_format, count))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        } catch (SecurityException ignored) {
            // 权限在调用瞬间被撤销时兜底，避免后台任务因通知失败而整体失败
        }
    }
}
