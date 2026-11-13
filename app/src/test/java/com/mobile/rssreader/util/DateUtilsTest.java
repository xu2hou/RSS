package com.mobile.rssreader.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DateUtilsTest {

    @Test
    public void parsesRfc822() {
        // Fri, 06 Nov 2026 08:24:39 +0800
        long actual = DateUtils.parse("Fri, 06 Nov 2026 08:24:39 +0800");
        long expected = 1793924679000L;
        assertTrue("RFC822 解析偏差过大: " + actual, Math.abs(actual - expected) < 1000L);
    }

    @Test
    public void parsesIso8601Utc() {
        // 2026-11-05T23:43:55Z
        long actual = DateUtils.parse("2026-11-05T23:43:55Z");
        assertEquals(1793922235000L, actual);
    }

    @Test
    public void parsesIso8601WithMillis() {
        long actual = DateUtils.parse("2026-11-05T23:43:55.500Z");
        assertEquals(1793922235500L, actual);
    }

    @Test
    public void invalidInputReturnsZeroInsteadOfThrowing() {
        // 一条脏数据不应该让整次抓取失败
        assertEquals(0L, DateUtils.parse(null));
        assertEquals(0L, DateUtils.parse(""));
        assertEquals(0L, DateUtils.parse("昨天"));
    }

    @Test
    public void relativeTimeFormatting() {
        long now = System.currentTimeMillis();
        assertEquals("刚刚", DateUtils.toRelative(now - 10_000L));
        assertTrue(DateUtils.toRelative(now - 5 * 60_000L).contains("分钟前"));
        assertTrue(DateUtils.toRelative(now - 3 * 60 * 60_000L).contains("小时前"));
        assertEquals("未知时间", DateUtils.toRelative(0L));
    }
}
