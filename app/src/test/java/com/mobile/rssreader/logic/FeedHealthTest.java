package com.mobile.rssreader.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.mobile.rssreader.data.entity.Feed;

import org.junit.Test;

public class FeedHealthTest {

    private static final long NOW = 1_800_000_000_000L;
    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;

    private Feed feed(int failCount, long lastFetchedAt) {
        Feed f = new Feed();
        f.id = 1;
        f.title = "测试源";
        f.url = "https://example.com/feed";
        f.failCount = failCount;
        f.lastFetchedAt = lastFetchedAt;
        return f;
    }

    @Test
    public void backoffGrowsWithFailures() {
        assertEquals(0, FeedHealth.backoffMillis(0));
        assertEquals(10 * MINUTE, FeedHealth.backoffMillis(1));
        assertEquals(1 * HOUR, FeedHealth.backoffMillis(2));
        assertEquals(6 * HOUR, FeedHealth.backoffMillis(3));
        assertEquals(24 * HOUR, FeedHealth.backoffMillis(9));
    }

    @Test
    public void canFetchRespectsBackoffWindow() {
        Feed justFailed = feed(1, NOW - MINUTE); // 失败 1 次，退避 10 分钟
        assertFalse(FeedHealth.canFetchNow(justFailed, NOW));

        Feed longAgo = feed(1, NOW - 20 * MINUTE);
        assertTrue(FeedHealth.canFetchNow(longAgo, NOW));
    }

    @Test
    public void healthyFeedIsAlwaysFetchable() {
        Feed healthy = feed(0, NOW - 5 * MINUTE);
        assertTrue(FeedHealth.canFetchNow(healthy, NOW));
        assertEquals("正常", FeedHealth.describe(healthy, NOW));
    }

    @Test
    public void unhealthyThresholdAndHealthScore() {
        assertFalse(FeedHealth.isUnhealthy(feed(2, NOW)));
        assertTrue(FeedHealth.isUnhealthy(feed(3, NOW)));

        assertEquals(1.0, FeedHealth.healthScore(feed(0, NOW)), 1e-6);
        assertEquals(0.0, FeedHealth.healthScore(feed(3, NOW)), 1e-6);
        // 失败一次应衰减到 2/3
        assertEquals(2.0 / 3.0, FeedHealth.healthScore(feed(1, NOW)), 1e-6);
    }

    @Test
    public void describeShowsRetryHintForFailedFeed() {
        String text = FeedHealth.describe(feed(1, NOW - MINUTE), NOW);
        assertTrue("应提示还在退避中: " + text, text.contains("分钟"));
    }
}
