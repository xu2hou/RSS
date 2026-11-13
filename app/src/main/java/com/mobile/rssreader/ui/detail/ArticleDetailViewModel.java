package com.mobile.rssreader.ui.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.mobile.rssreader.data.entity.Article;
import com.mobile.rssreader.data.repo.ArticleRepository;

/**
 * 详情页 ViewModel：按 id 观察文章，并提供「标记已读 / 切换收藏」两个动作。
 * switchMap 保证换文章时自动切换数据源，不会留下旧观察者。
 */
public class ArticleDetailViewModel extends AndroidViewModel {

    private final ArticleRepository repository;
    private final MutableLiveData<Long> articleId = new MutableLiveData<>();
    private final LiveData<Article> article;

    public ArticleDetailViewModel(@NonNull Application application) {
        super(application);
        repository = ArticleRepository.get(application);
        article = Transformations.switchMap(articleId, repository::observeById);
    }

    public LiveData<Article> getArticle() {
        return article;
    }

    public void load(long id) {
        Long current = articleId.getValue();
        if (current == null || current != id) {
            articleId.setValue(id);
        }
    }

    public void markRead(Article item) {
        if (item != null && !item.read) {
            repository.markRead(item.id);
        }
    }

    public void toggleStar(Article item) {
        if (item != null) {
            repository.setStarred(item.id, !item.starred);
        }
    }
}
