package com.mobile.rssreader.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.mobile.rssreader.data.entity.Article;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ArticleRankerTest {

    private static final long NOW = 1_800_000_000_000L;
    private static final long HOUR = 3_600_000L;

    private Article article(long id, long ageHours, boolean read, boolean starred) {
        Article a = new Article();
        a.id = id;
        a.feedId = 1;
        a.title = "文章" + id;
        a.link = "https://example.com/" + id;
        a.publishedAt = NOW - ageHours * HOUR;
        a.fetchedAt = a.publishedAt;
        a.read = read;
        a.starred = starred;
        return a;
    }

    @Test
    public void newerArticlesScoreHigherWhenOtherFactorsEqual() {
        Article older = article(1, 24, false, false);
        Article newer = article(2, 1, false, false);
        assertTrue(ArticleRanker.score(newer, 1.0, NOW) > ArticleRanker.score(older, 1.0, NOW));
    }

    @Test
    public void starredBeatsUnstarredAtSameAge() {
        Article normal = article(1, 2, false, false);
        Article starred = article(2, 2, false, true);
        assertTrue(ArticleRanker.score(starred, 1.0, NOW) > ArticleRanker.score(normal, 1.0, NOW));
    }

    @Test
    public void unhealthyFeedLowersScore() {
        Article a = article(1, 2, false, false);
        double healthy = ArticleRanker.score(a, 1.0, NOW);
        double broken = ArticleRanker.score(a, 0.0, NOW);
        assertTrue("健康度低的源应得分更低", healthy > broken);
        // 权重差 2.0 会被时间衰减除低，所以差值等于 2.0 / decay，必然小于 2.0
        assertTrue("差值应小于权重本身（已被时间衰减缩过）", (healthy - broken) < 2.0);
        assertTrue("差值应仍为正", (healthy - broken) > 0);
    }

    @Test
    public void rankSortsByScoreAndRespectsLimit() {
        List<Article> list = new ArrayList<>();
        list.add(article(1, 48, true, false));   // 最旧且已读
        list.add(article(2, 1, false, true));    // 最新且收藏
        list.add(article(3, 6, false, false));

        List<Article> ranked = ArticleRanker.rank(list, 1.0, 2);
        assertEquals(2, ranked.size());
        assertEquals(2L, ranked.get(0).id);
        assertEquals(3L, ranked.get(1).id);
    }

    @Test
    public void emptyInputReturnsEmptyList() {
        assertTrue(ArticleRanker.rank(new ArrayList<>(), 1.0, 10).isEmpty());
        assertTrue(ArticleRanker.rank(null, 1.0, 10).isEmpty());
    }

    @Test
    public void zeroPublishedAtFallsBackToFetchedAt() {
        Article a = article(1, 0, false, false);
        a.publishedAt = 0;
        a.fetchedAt = NOW - 2 * HOUR;
        double score = ArticleRanker.score(a, 1.0, NOW);
        assertTrue("不应因为发布时间缺失而得负分或 NaN", score > 0 && !Double.isNaN(score));
    }
}
