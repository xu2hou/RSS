package com.mobile.rssreader.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * RSS 用 RFC 822（如 {@code Fri, 06 Nov 2026 08:24:39 +0800}），
 * Atom 用 ISO 8601（如 {@code 2026-11-05T23:43:55Z}）。
 * 这里统一解析成毫秒时间戳，解析失败返回 0（不抛异常，避免一条脏数据毁掉整次抓取）。
 */
public final class DateUtils {

    private static final String[] PATTERNS = {
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, d MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm Z",
            "dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd HH:mm:ss"
    };

    private DateUtils() {
    }

    public static long parse(String raw) {
        if (raw == null) {
            return 0L;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return 0L;
        }
        for (String pattern : PATTERNS) {
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
            format.setLenient(true);
            if (pattern.endsWith("'Z'")) {
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
            }
            try {
                Date date = format.parse(value);
                if (date != null) {
                    return date.getTime();
                }
            } catch (ParseException ignored) {
                // 换下一个 pattern
            }
        }
        return 0L;
    }

    /** 列表页展示：刚刚 / N 分钟前 / N 小时前 / MM-dd */
    public static String toRelative(long timeMillis) {
        if (timeMillis <= 0) {
            return "未知时间";
        }
        long diff = System.currentTimeMillis() - timeMillis;
        if (diff < 0) {
            diff = 0;
        }
        long minute = 60_000L;
        long hour = 60 * minute;
        long day = 24 * hour;
        if (diff < minute) {
            return "刚刚";
        }
        if (diff < hour) {
            return (diff / minute) + " 分钟前";
        }
        if (diff < day) {
            return (diff / hour) + " 小时前";
        }
        if (diff < 7 * day) {
            return (diff / day) + " 天前";
        }
        return new SimpleDateFormat("MM-dd", Locale.CHINA).format(new Date(timeMillis));
    }
}
