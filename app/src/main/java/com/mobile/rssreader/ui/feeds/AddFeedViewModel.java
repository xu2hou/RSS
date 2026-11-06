package com.mobile.rssreader.ui.feeds;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mobile.rssreader.data.entity.Feed;
import com.mobile.rssreader.data.repo.FeedRepository;
import com.mobile.rssreader.util.Callback;
import com.mobile.rssreader.util.Event;

/**
 * 添加订阅的 ViewModel：负责「校验 → 抓取 → 入库」的状态流转。
 * 页面只关心 loading 与结果事件，不碰线程。
 */
public class AddFeedViewModel extends AndroidViewModel {

    private final FeedRepository repository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Event<Outcome>> outcome = new MutableLiveData<>();

    public AddFeedViewModel(@NonNull Application application) {
        super(application);
        repository = FeedRepository.get(application);
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Event<Outcome>> getOutcome() {
        return outcome;
    }

    public void addFeed(String url) {
        if (url == null || url.trim().isEmpty()) {
            outcome.setValue(new Event<>(Outcome.fail("请先填写订阅地址")));
            return;
        }
        loading.setValue(true);
        repository.addFeed(url, new Callback<Feed>() {
            @Override
            public void onSuccess(Feed data) {
                loading.setValue(false);
                outcome.setValue(new Event<>(Outcome.ok(data.title)));
            }

            @Override
            public void onError(String message) {
                loading.setValue(false);
                outcome.setValue(new Event<>(Outcome.fail(message)));
            }
        });
    }

    /** 添加结果 */
    public static final class Outcome {
        public final boolean success;
        public final String message;

        private Outcome(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        static Outcome ok(String title) {
            return new Outcome(true, title);
        }

        static Outcome fail(String reason) {
            return new Outcome(false, reason);
        }
    }
}
