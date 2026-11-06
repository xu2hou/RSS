package com.mobile.rssreader.logic;

import com.mobile.rssreader.data.entity.Article;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 自定义推荐排序（本项目的「非增删改查」业务逻辑之一）。
 * <p>
 * 纯函数、无 Android 依赖，可直接用 JUnit 覆盖。
 * <p>
 * 评分模型（参考 Hacker News 的时间衰减，做了简化以便解释）：
 * <pre>
 *   base      = 1.0
 *   收藏      + 3.0
 *   未读      + 1.0
 *   源健康度  + 2.0 * health   （health ∈ [0,1]，连续抓取失败会拉低）
 *   score     = 权重和 / (ageHours/6 + 1)^1.2
 * </pre>
 * 除以时间项让「新文章」有机会压过「老的高分文章」，
 * 指数 1.2 越大衰减越快；6 小时为半衰参考尺。
 */
public final class ArticleRanker {

    private static final double BASE = 1.0;
    private static final double WEIGHT_STARRED = 3.0;
    private static final double WEIGHT_UNREAD = 1.0;
    private static final double WEIGHT_HEALTH = 2.0;
    private static final double GRAVITY = 1.2;
    private static final double HALF_LIFE_HOURS = 6.0;

    private ArticleRanker() {
    }

    public static List<Article> rank(List<Article> source, double feedHealth, int limit) {
        final double health = feedHealth;
        return rank(source, feedId -> health, limit);
    }

    /** 每篇文章按它所属订阅源的健康度加权 */
    public static List<Article> rank(List<Article> source, HealthProvider provider, int limit) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        long now = System.currentTimeMillis();
        List<Scored> scored = new ArrayList<>(source.size());
        for (Article article : source) {
            double health = provider == null ? 1.0 : provider.healthOf(article.feedId);
            scored.add(new Scored(article, score(article, health, now)));
        }
        // 分数相同的时候按发布时间兜底，保证顺序稳定
        scored.sort((a, b) -> {
            int byScore = Double.compare(b.score, a.score);
            return byScore != 0 ? byScore : Long.compare(b.article.publishedAt, a.article.publishedAt);
        });

        int size = limit <= 0 ? scored.size() : Math.min(limit, scored.size());
        List<Article> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(scored.get(i).article);
        }
        return result;
    }

    /** 让仓库把「feedId → 健康度」的查询交给排序器，避免把 Map 塞进算法层 */
    public interface HealthProvider {
        double healthOf(long feedId);
    }

    /** 暴露给单元测试，便于对具体数值断言 */
    public static double score(Article article, double feedHealth, long now) {
        double raw = BASE;
        if (article.starred) {
            raw += WEIGHT_STARRED;
        }
        if (!article.read) {
            raw += WEIGHT_UNREAD;
        }
        double health = clamp(feedHealth, 0.0, 1.0);
        raw += WEIGHT_HEALTH * health;

        long published = article.publishedAt > 0 ? article.publishedAt : article.fetchedAt;
        double ageHours = Math.max(0, now - published) / 3_600_000.0;
        double decay = Math.pow(ageHours / HALF_LIFE_HOURS + 1.0, GRAVITY);
        return raw / decay;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Scored {
        final Article article;
        final double score;

        Scored(Article article, double score) {
            this.article = article;
            this.score = score;
        }
    }
}
