package com.mobile.rssreader.ui.feeds;

import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.rssreader.R;
import com.mobile.rssreader.data.entity.Feed;
import com.mobile.rssreader.data.entity.FeedWithUnread;
import com.mobile.rssreader.databinding.ItemFeedBinding;
import com.mobile.rssreader.logic.FeedHealth;
import com.mobile.rssreader.util.DateUtils;

/**
 * 订阅列表适配器。
 * 用 ListAdapter + DiffUtil 做局部刷新，避免 notifyDataSetChanged 造成的闪烁与滚动跳动。
 */
public class FeedAdapter extends ListAdapter<FeedWithUnread, FeedAdapter.FeedViewHolder> {

    public interface Listener {
        void onFeedClick(Feed feed);

        void onFeedLongClick(Feed feed);
    }

    private static final DiffUtil.ItemCallback<FeedWithUnread> DIFF =
            new DiffUtil.ItemCallback<FeedWithUnread>() {
                @Override
                public boolean areItemsTheSame(@NonNull FeedWithUnread oldItem,
                                               @NonNull FeedWithUnread newItem) {
                    return oldItem.feed.id == newItem.feed.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull FeedWithUnread oldItem,
                                                  @NonNull FeedWithUnread newItem) {
                    return oldItem.unreadCount == newItem.unreadCount
                            && oldItem.feed.failCount == newItem.feed.failCount
                            && oldItem.feed.lastFetchedAt == newItem.feed.lastFetchedAt
                            && TextUtils.equals(oldItem.feed.title, newItem.feed.title);
                }
            };

    private final Listener listener;

    public FeedAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFeedBinding binding = ItemFeedBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new FeedViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class FeedViewHolder extends RecyclerView.ViewHolder {

        private final ItemFeedBinding binding;

        FeedViewHolder(ItemFeedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FeedWithUnread item, Listener listener) {
            Feed feed = item.feed;
            binding.tvTitle.setText(feed.title.isEmpty() ? feed.url : feed.title);

            String host = extractHost(feed.url);
            String time = feed.lastFetchedAt > 0
                    ? DateUtils.toRelative(feed.lastFetchedAt) + "更新"
                    : binding.getRoot().getContext().getString(R.string.feed_never_fetched);
            String health = FeedHealth.describe(feed, System.currentTimeMillis());
            String subtitle = binding.getRoot().getContext()
                    .getString(R.string.feed_subtitle_format, host, time);
            if (feed.failCount > 0) {
                subtitle = subtitle + " · " + health;
            }
            binding.tvSubtitle.setText(subtitle);

            if (item.unreadCount > 0) {
                binding.tvUnread.setVisibility(android.view.View.VISIBLE);
                binding.tvUnread.setText(String.valueOf(item.unreadCount));
            } else {
                binding.tvUnread.setVisibility(android.view.View.GONE);
            }

            binding.getRoot().setOnClickListener(v -> listener.onFeedClick(feed));
            binding.getRoot().setOnLongClickListener(v -> {
                listener.onFeedLongClick(feed);
                return true;
            });
        }

        private static String extractHost(String url) {
            try {
                String host = Uri.parse(url).getHost();
                return host == null ? url : host;
            } catch (Exception e) {
                return url;
            }
        }
    }
}
