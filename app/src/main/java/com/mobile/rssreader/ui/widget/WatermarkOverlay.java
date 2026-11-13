package com.mobile.rssreader.ui.widget;

import android.app.Activity;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.mobile.rssreader.data.prefs.SettingsPreferences;

/**
 * 演示水印：在界面左下角常驻显示「学号 + 姓名」。
 * <p>
 * 之所以做进 App 而不是录屏后叠加：本机没有 ffmpeg / OBS，后期叠字要么装工具、
 * 要么手工剪辑，容易出错；做进界面后录屏天然带水印，且位置、透明度稳定可复现。
 * 是否显示由设置页开关控制（默认开启），方便平时使用时关掉。
 */
public final class WatermarkOverlay {

    private static final String TEXT = "202405100113 邬逸帆";

    private WatermarkOverlay() {
    }

    public static void attach(Activity activity) {
        if (activity == null || !SettingsPreferences.isWatermarkEnabled(activity)) {
            return;
        }
        TextView view = new TextView(activity);
        view.setText(TEXT);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        view.setTextColor(Color.parseColor("#CCFFFFFF"));
        view.setBackgroundColor(Color.parseColor("#66000000"));
        int padH = dp(activity, 6);
        int padV = dp(activity, 2);
        view.setPadding(padH, padV, padH, padV);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        // 放左下角、抬到底部导航栏之上，避免遮挡「订阅/文章/设置」
        params.gravity = Gravity.BOTTOM | Gravity.START;
        params.leftMargin = dp(activity, 12);
        params.bottomMargin = dp(activity, 128);

        // android.R.id.content 是 FrameLayout，直接叠在内容之上
        activity.addContentView(view, params);
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
