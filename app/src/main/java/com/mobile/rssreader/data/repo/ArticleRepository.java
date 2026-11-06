package com.mobile.rssreader.data.repo;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.mobile.rssreader.data.AppDatabase;
import com.mobile.rssreader.data.dao.ArticleDao;
import com.mobile.rssreader.data.entity.Article;
import com.mobile.rssreader.data.entity.FeedWithUnread;
import com.mobile.rssreader.logic.ArticleRanker;
import com.mobile.rssreader.logic.FeedHealth;
import com.mobile.rssreader.util.AppExecutors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章仓库：读取、已读/星标状态、推荐排序。
 */
public final class ArticleRepository {

    private static volatile ArticleRepository instance;

    private final ArticleDao articleDao;
    private final FeedRepository feedRepository;

    private ArticleRepository(Context context) {
        AppDatabase database = AppDatabase.get(context.getApplicationContext());
        this.articleDao = database.articleDao();
        this.feedRepository = FeedRepository.get(context);
    }

    public static ArticleRepository get(Context context) {
        if (instance == null) {
            synchronized (ArticleRepository.class) {
                if (instance == null) {
                    instance = new ArticleRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public LiveData<List<Article>> observeAll() {
        return articleDao.observeAll();
    }

    public LiveData<List<Article>> observeByFeed(long feedId) {
        return articleDao.observeByFeed(feedId);
    }

    public LiveData<List<Article>> observeUnread() {
        return articleDao.observeUnread();
    }

    public LiveData<List<Article>> observeStarred() {
        return articleDao.observeStarred();
    }

    public LiveData<Article> observeById(long id) {
        return articleDao.observeById(id);
    }

    /**
     * 「为你推荐」：把「全部文章」与「各源健康度」两个数据源合并后排序。
     * <p>
     * 排序是纯内存计算（O(n log n)，n 为本地文章数，通常几百条），不涉及 I/O，
     * 因此放在 LiveData 观察回调里不会阻塞主线程；数据库读写在 Room 侧异步完成。
     */
    public LiveData<List<Article>> observeRecommended() {
        final MediatorLiveData<List<Article>> result = new MediatorLiveData<>();
        final Map<Long, Double> health = new HashMap<>();
        final List<Article> cache = new ArrayList<>(256);

        LiveData<List<Article>> articles = articleDao.observeAll();
        LiveData<List<FeedWithUnread>> feeds = feedRepository.observeFeeds();

        result.addSource(articles, list -> {
            cache.clear();
            if (list != null) {
                cache.addAll(list);
            }
            result.setValue(rank(cache, health));
        });
        result.addSource(feeds, feedList -> {
            health.clear();
            if (feedList != null) {
                for (FeedWithUnread item : feedList) {
                    health.put(item.feed.id, FeedHealth.healthScore(item.feed));
                }
            }
            if (!cache.isEmpty()) {
                result.setValue(rank(cache, health));
            }
        });
        return result;
    }

    private List<Article> rank(List<Article> articles, Map<Long, Double> health) {
        List<Article> safe = new ArrayList<>(articles);
        return ArticleRanker.rank(safe, feedId -> {
            Double h = health.get(feedId);
            return h == null ? 1.0 : h;
        }, 20);
    }

    public void markRead(long articleId) {
        AppExecutors.get().diskIO().execute(() -> articleDao.markRead(articleId));
    }

    public void setStarred(long articleId, boolean starred) {
        AppExecutors.get().diskIO().execute(() -> articleDao.setStarred(articleId, starred));
    }

    public void deleteArticle(long articleId) {
        AppExecutors.get().diskIO().execute(() -> articleDao.deleteById(articleId));
    }

    /** 取全部文章（备份导出/统计用，需在后台线程调用） */
    public List<Article> getAllSync() {
        return articleDao.getAllSync();
    }
}
