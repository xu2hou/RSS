package com.mobile.rssreader.ui.feeds;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mobile.rssreader.data.entity.FeedWithUnread;
import com.mobile.rssreader.data.repo.FeedRepository;
import com.mobile.rssreader.util.Event;

import java.util.List;

/**
 * 订阅列表的 ViewModel：只暴露 LiveData 与动作方法，
 * 不持有任何 View 引用，屏幕旋转后列表与刷新状态都会自动保留。
 */
public class FeedsViewModel extends AndroidViewModel {

    private final FeedRepository repository;
    private final LiveData<List<FeedWithUnread>> feeds;
    private final MutableLiveData<Boolean> refreshing = new MutableLiveData<>(false);
    private final MutableLiveData<Event<String>> message = new MutableLiveData<>();

    public FeedsViewModel(@NonNull Application application) {
        super(application);
        repository = FeedRepository.get(application);
        feeds = repository.observeFeeds();
    }

    public LiveData<List<FeedWithUnread>> getFeeds() {
        return feeds;
    }

    public LiveData<Boolean> getRefreshing() {
        return refreshing;
    }

    public LiveData<Event<String>> getMessage() {
        return message;
    }

    /** 下拉刷新：抓取全部订阅源，结果用一次性事件提示 */
    public void refreshAll() {
        refreshing.setValue(true);
        repository.refreshAll(new com.mobile.rssreader.util.Callback<>() {
            @Override
            public void onSuccess(FeedRepository.RefreshSummary data) {
                refreshing.setValue(false);
                message.setValue(new Event<>(data.toMessage()));
            }

            @Override
            public void onError(String error) {
                refreshing.setValue(false);
                message.setValue(new Event<>(error));
            }
        });
    }

    public void deleteFeed(long feedId) {
        repository.deleteFeed(feedId, () -> message.setValue(new Event<>("已取消订阅")));
    }
}
