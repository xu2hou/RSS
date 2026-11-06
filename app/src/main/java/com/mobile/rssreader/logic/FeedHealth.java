package com.mobile.rssreader.logic;

import com.mobile.rssreader.data.entity.Feed;

/**
 * 订阅源健康度与失败退避（本项目的「非增删改查」业务逻辑之二）。
 * <p>
 * 规则：连续失败越多，退避越久；达到阈值标记「异常」，
 * 后台刷新时跳过尚未到期的源，避免一个坏源拖垮整轮刷新。
 * 纯函数、无 Android 依赖，可用 JUnit 覆盖。
 */
public final class FeedHealth {

    /** 连续失败达到该次数即视为异常源 */
    public static final int UNHEALTHY_THRESHOLD = 3;

    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;

    private FeedHealth() {
    }

    /** 失败 N 次后，下次允许重试的间隔（指数式退避） */
    public static long backoffMillis(int failCount) {
        if (failCount <= 0) {
            return 0;
        }
        if (failCount == 1) {
            return 10 * MINUTE;
        }
        if (failCount == 2) {
            return 1 * HOUR;
        }
        if (failCount == 3) {
            return 6 * HOUR;
        }
        return 24 * HOUR;
    }

    /** 距离下次可重试还剩多少毫秒（<=0 表示现在就可以抓） */
    public static long remainingBackoff(Feed feed, long now) {
        if (feed == null || feed.failCount <= 0) {
            return 0;
        }
        long nextAllowed = feed.lastFetchedAt + backoffMillis(feed.failCount);
        return nextAllowed - now;
    }

    public static boolean canFetchNow(Feed feed, long now) {
        return remainingBackoff(feed, now) <= 0;
    }

    public static boolean isUnhealthy(Feed feed) {
        return feed != null && feed.failCount >= UNHEALTHY_THRESHOLD;
    }

    /**
     * 健康度 0..1：供排序算法与列表页徽标使用。
     * 0 次失败 = 1.0；每失败一次衰减，达到阈值后归零。
     */
    public static double healthScore(Feed feed) {
        if (feed == null) {
            return 0.5;
        }
        int fails = Math.max(0, feed.failCount);
        if (fails >= UNHEALTHY_THRESHOLD) {
            return 0.0;
        }
        return 1.0 - (fails / (double) UNHEALTHY_THRESHOLD);
    }

    /** 列表页/详情页展示的文案 */
    public static String describe(Feed feed, long now) {
        if (feed == null) {
            return "未知";
        }
        if (feed.failCount <= 0) {
            return "正常";
        }
        long remaining = remainingBackoff(feed, now);
        if (remaining <= 0) {
            return "待重试";
        }
        long minutes = remaining / MINUTE;
        String wait = minutes < 60 ? (minutes + " 分钟") : ((minutes / 60) + " 小时");
        if (isUnhealthy(feed)) {
            return "异常，约 " + wait + "后重试";
        }
        return "失败 " + feed.failCount + " 次，" + wait + "后重试";
    }
}
