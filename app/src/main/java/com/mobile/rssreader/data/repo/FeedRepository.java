package com.mobile.rssreader.data.repo;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.mobile.rssreader.data.AppDatabase;
import com.mobile.rssreader.data.dao.ArticleDao;
import com.mobile.rssreader.data.dao.FeedDao;
import com.mobile.rssreader.data.entity.Article;
import com.mobile.rssreader.data.entity.Feed;
import com.mobile.rssreader.data.entity.FeedWithUnread;
import com.mobile.rssreader.data.net.NetworkErrorMapper;
import com.mobile.rssreader.data.net.RssFetcher;
import com.mobile.rssreader.data.net.RssParser;
import com.mobile.rssreader.logic.FeedHealth;
import com.mobile.rssreader.util.AppExecutors;
import com.mobile.rssreader.util.Callback;
import com.mobile.rssreader.util.HtmlUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订阅源仓库：串起「网络 → 解析 → 数据库」。
 * <p>
 * 线程约定：本类所有抓取/写入都在后台线程完成，回调统一切回主线程，
 * 调用方（ViewModel）不需要关心线程。
 */
public final class FeedRepository {

    private static volatile FeedRepository instance;

    private final AppDatabase database;
    private final FeedDao feedDao;
    private final ArticleDao articleDao;

    private FeedRepository(AppDatabase database) {
        this.database = database;
        this.feedDao = database.feedDao();
        this.articleDao = database.articleDao();
    }

    public static FeedRepository get(Context context) {
        if (instance == null) {
            synchronized (FeedRepository.class) {
                if (instance == null) {
                    instance = new FeedRepository(AppDatabase.get(context.getApplicationContext()));
                }
            }
        }
        return instance;
    }

    public LiveData<List<FeedWithUnread>> observeFeeds() {
        return feedDao.observeAllWithUnread();
    }

    public LiveData<Integer> observeUnreadTotal() {
        return articleDao.observeUnreadTotal();
    }

    /**
     * 添加订阅：校验地址 → 抓取解析 → 事务内写入 feed 与文章。
     */
    public void addFeed(String rawUrl, Callback<Feed> callback) {
        final String url = RssFetcher.normalizeUrl(rawUrl);
        if (url == null) {
            callback.onError("地址格式不正确，请填写以 http(s):// 开头的订阅地址");
            return;
        }
        AppExecutors.get().networkIO().execute(() -> {
            try {
                if (feedDao.findByUrl(url) != null) {
                    AppExecutors.get().main(() -> callback.onError("已经订阅过这个源了"));
                    return;
                }
                RssParser.ParsedFeed parsed = RssFetcher.fetch(url);
                Feed feed = new Feed();
                feed.title = parsed.title;
                feed.url = url;
                feed.siteUrl = parsed.siteUrl;
                feed.description = parsed.description;
                feed.lastFetchedAt = System.currentTimeMillis();

                AppExecutors.get().diskIO().execute(() -> {
                    try {
                        long[] inserted = new long[1];
                        database.runInTransaction(() -> {
                            inserted[0] = feedDao.insert(feed);
                            if (inserted[0] > 0) {
                                feed.id = inserted[0];
                                List<Article> articles = toEntities(inserted[0], parsed.items);
                                if (!articles.isEmpty()) {
                                    articleDao.insertAll(articles);
                                }
                            }
                        });
                        if (inserted[0] <= 0) {
                            AppExecutors.get().main(() -> callback.onError("该订阅地址已存在"));
                            return;
                        }
                        AppExecutors.get().main(() -> callback.onSuccess(feed));
                    } catch (Exception e) {
                        AppExecutors.get().main(() -> callback.onError("保存失败：" + e.getMessage()));
                    }
                });
            } catch (Exception e) {
                AppExecutors.get().main(() -> callback.onError(describeError(e)));
            }
        });
    }

    /**
     * 刷新单个源（用户手动刷新，忽略退避）。
     *
     * @param callback 返回本次新增文章条数
     */
    public void refreshFeed(long feedId, Callback<Integer> callback) {
        AppExecutors.get().networkIO().execute(() -> {
            Feed feed = feedDao.findById(feedId);
            if (feed == null) {
                AppExecutors.get().main(() -> callback.onError("订阅源不存在"));
                return;
            }
            try {
                RssParser.ParsedFeed parsed = RssFetcher.fetch(feed.url);
                List<Article> articles = toEntities(feedId, parsed.items);
                final int before = articleDao.count();
                AppExecutors.get().diskIO().execute(() -> {
                    articleDao.insertAll(articles);
                    int after = articleDao.count();
                    feedDao.markFetchSuccess(feedId, System.currentTimeMillis());
                    final int added = Math.max(0, after - before);
                    AppExecutors.get().main(() -> callback.onSuccess(added));
                });
            } catch (Exception e) {
                AppExecutors.get().diskIO().execute(() -> feedDao.markFetchFailed(feedId));
                AppExecutors.get().main(() -> callback.onError(describeError(e)));
            }
        });
    }

    public LiveData<List<FeedWithUnread>> observeFeedsSorted() {
        return feedDao.observeAllWithUnread();
    }

    public void deleteFeed(long feedId, Runnable done) {
        AppExecutors.get().diskIO().execute(() -> {
            Feed feed = feedDao.findById(feedId);
            if (feed != null) {
                // 外键 ON DELETE CASCADE 会自动清掉该源的文章
                feedDao.delete(feed);
            }
            AppExecutors.get().main(done);
        });
    }

    /**
     * 后台刷新全部源（尊重健康度退避）。
     * 全部失败时 summary.firstError 会带上第一个源的可读错误，供界面展示错误态。
     */
    public void refreshAll(Callback<RefreshSummary> callback) {
        AppExecutors.get().networkIO().execute(() -> {
            RefreshSummary summary = refreshAllSync();
            AppExecutors.get().main(() -> callback.onSuccess(summary));
        });
    }

    /**
     * 同步刷新全部源，供 WorkManager 的 Worker 直接调用
     * （Worker 的 doWork 本身就在后台线程，不需要再切线程）。
     */
    public RefreshSummary refreshAllSync() {
        RefreshSummary summary = new RefreshSummary();
        long now = System.currentTimeMillis();
        List<Feed> feeds = feedDao.getAllSync();
        for (Feed feed : feeds) {
            if (!feed.enabled) {
                summary.skipped++;
                continue;
            }
            if (!FeedHealth.canFetchNow(feed, now)) {
                summary.skipped++;
                continue;
            }
            try {
                RssParser.ParsedFeed parsed = RssFetcher.fetch(feed.url);
                List<Article> articles = toEntities(feed.id, parsed.items);
                int[] added = new int[1];
                database.runInTransaction(() -> {
                    int before = articleDao.count();
                    articleDao.insertAll(articles);
                    added[0] = Math.max(0, articleDao.count() - before);
                    feedDao.markFetchSuccess(feed.id, System.currentTimeMillis());
                });
                summary.success++;
                summary.newArticles += added[0];
            } catch (Exception e) {
                summary.failed++;
                if (summary.firstError == null) {
                    summary.firstError = feed.title + "：" + NetworkErrorMapper.toUserMessage(e);
                }
                feedDao.markFetchFailed(feed.id);
            }
        }
        return summary;
    }

    private List<Article> toEntities(long feedId, List<RssParser.ParsedItem> items) {
        Map<String, Article> unique = new LinkedHashMap<>();
        for (RssParser.ParsedItem item : items) {
            String link = item.link.trim();
            if (link.isEmpty() || unique.containsKey(link)) {
                continue; // 同一次抓取内部先去重
            }
            Article article = new Article();
            article.feedId = feedId;
            article.title = item.title.isEmpty() ? "（无标题）" : item.title;
            article.link = link;
            article.author = item.author;
            article.summary = HtmlUtils.ellipsize(item.summary == null ? "" : item.summary, 200);
            article.contentHtml = item.contentHtml;
            article.publishedAt = item.publishedAt;
            article.fetchedAt = System.currentTimeMillis();
            unique.put(link, article);
        }
        return new ArrayList<>(unique.values());
    }

    private static String describeError(Exception e) {
        return NetworkErrorMapper.toUserMessage(e);
    }

    /** 一轮刷新的统计结果 */
    public static final class RefreshSummary {
        public int success;
        public int failed;
        public int skipped;
        public int newArticles;
        /** 第一个失败的源的可读错误，用于错误态展示 */
        public String firstError;

        /** 有源参与、但一个都没成功 —— 界面应展示错误态而不是「成功 0 个」 */
        public boolean allFailed() {
            return success == 0 && failed > 0;
        }

        public String toMessage() {
            StringBuilder sb = new StringBuilder();
            if (success == 0 && failed == 0 && skipped == 0) {
                return "还没有可刷新的订阅源";
            }
            sb.append("刷新完成：成功 ").append(success).append(" 个");
            if (newArticles > 0) {
                sb.append("，新增 ").append(newArticles).append(" 篇");
            }
            if (failed > 0) {
                sb.append("，失败 ").append(failed).append(" 个");
            }
            if (skipped > 0) {
                sb.append("，跳过 ").append(skipped).append(" 个");
            }
            return sb.toString();
        }

        /** 错误态标题：优先用具体源的原因 */
        public String toErrorMessage() {
            if (firstError != null) {
                return firstError;
            }
            return "刷新失败：共 " + failed + " 个订阅源无法访问";
        }
    }
}
