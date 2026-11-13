package com.mobile.rssreader.ui.articles;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.mobile.rssreader.data.entity.Article;
import com.mobile.rssreader.data.entity.FeedWithUnread;
import com.mobile.rssreader.data.repo.ArticleRepository;
import com.mobile.rssreader.data.repo.FeedRepository;

import java.util.List;

/**
 * 文章列表的 ViewModel：用 switchMap 把「当前筛选」映射到不同的数据源，
 * 切换 Tab 时不需要重建页面，也不会泄漏旧查询的观察者。
 */
public class ArticlesViewModel extends AndroidViewModel {

    public static final int FILTER_RECOMMENDED = 0;
    public static final int FILTER_UNREAD = 1;
    public static final int FILTER_STARRED = 2;

    private final ArticleRepository articleRepository;
    private final FeedRepository feedRepository;

    private final MutableLiveData<Integer> filter = new MutableLiveData<>(FILTER_RECOMMENDED);

    private final LiveData<List<Article>> articles;

    public ArticlesViewModel(@NonNull Application application) {
        super(application);
        articleRepository = ArticleRepository.get(application);
        feedRepository = FeedRepository.get(application);

        articles = Transformations.switchMap(filter, current -> {
            if (current == null) {
                return articleRepository.observeAll();
            }
            switch (current) {
                case FILTER_UNREAD:
                    return articleRepository.observeUnread();
                case FILTER_STARRED:
                    return articleRepository.observeStarred();
                case FILTER_RECOMMENDED:
                default:
                    return articleRepository.observeRecommended();
            }
        });
    }

    public LiveData<List<Article>> getArticles() {
        return articles;
    }

    public LiveData<List<FeedWithUnread>> getFeeds() {
        return feedRepository.observeFeeds();
    }

    public LiveData<Integer> getFilter() {
        return filter;
    }

    public void setFilter(int value) {
        if (filter.getValue() == null || filter.getValue() != value) {
            filter.setValue(value);
        }
    }

    public void toggleStar(Article article) {
        articleRepository.setStarred(article.id, !article.starred);
    }

    /** 打开详情时标记已读（详情页也会标一次，这里保证「已进入即已读」） */
    public void markRead(Article article) {
        if (!article.read) {
            articleRepository.markRead(article.id);
        }
    }
}
