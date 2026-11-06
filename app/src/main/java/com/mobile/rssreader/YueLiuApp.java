package com.mobile.rssreader;

import android.app.Application;

/**
 * 应用入口：仅做全局初始化占位。
 * 数据库与线程池都是懒加载单例，首次使用时自动创建。
 */
public class YueLiuApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
