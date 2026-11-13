package com.mobile.rssreader.ui.articles;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.rssreader.R;
import com.mobile.rssreader.data.entity.Article;
import com.mobile.rssreader.databinding.ItemArticleBinding;
import com.mobile.rssreader.util.DateUtils;
import com.mobile.rssreader.util.HtmlUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 文章列表适配器：已读置灰、未读加粗、星标可点。
 * 订阅源名称通过 {@link #setFeedTitles(Map)} 由页面注入，避免在 SQL 里再拼一张表。
 */
public class ArticleAdapter extends ListAdapter<Article, ArticleAdapter.ArticleViewHolder> {

    public interface Listener {
        void onArticleClick(Article article);

        void onStarClick(Article article);
    }

    private static final DiffUtil.ItemCallback<Article> DIFF = new DiffUtil.ItemCallback<Article>() {
        @Override
        public boolean areItemsTheSame(@NonNull Article oldItem, @NonNull Article newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Article oldItem, @NonNull Article newItem) {
            return oldItem.read == newItem.read
                    && oldItem.starred == newItem.starred
                    && TextUtils.equals(oldItem.title, newItem.title)
                    && TextUtils.equals(oldItem.summary, newItem.summary)
                    && oldItem.publishedAt == newItem.publishedAt;
        }
    };

    private final Listener listener;
    private final Map<Long, String> feedTitles = new HashMap<>();

    public ArticleAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void setFeedTitles(Map<Long, String> titles) {
        feedTitles.clear();
        if (titles != null) {
            feedTitles.putAll(titles);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemArticleBinding binding = ItemArticleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ArticleViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        holder.bind(getItem(position), feedTitles, listener);
    }

    static class ArticleViewHolder extends RecyclerView.ViewHolder {

        private final ItemArticleBinding binding;

        ArticleViewHolder(ItemArticleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Article item, Map<Long, String> feedTitles, Listener listener) {
            binding.tvTitle.setText(item.title);
            // 已读置灰、未读用主色强调（列表一眼可辨阅读状态）
            int titleColor = item.read
                    ? binding.getRoot().getContext().getColor(R.color.text_secondary)
                    : binding.getRoot().getContext().getColor(R.color.text_primary);
            binding.tvTitle.setTextColor(titleColor);

            String feed = feedTitles.get(item.feedId);
            if (feed == null || feed.isEmpty()) {
                feed = item.author == null ? "未知来源" : item.author;
            }
            binding.tvMeta.setText(binding.getRoot().getContext().getString(
                    R.string.article_meta_format, feed, DateUtils.toRelative(item.publishedAt)));

            String summary = HtmlUtils.ellipsize(item.summary == null ? "" : item.summary, 120);
            if (summary.isEmpty()) {
                binding.tvSummary.setVisibility(View.GONE);
            } else {
                binding.tvSummary.setVisibility(View.VISIBLE);
                binding.tvSummary.setText(summary);
            }

            binding.btnStar.setImageResource(
                    item.starred ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);

            binding.getRoot().setOnClickListener(v -> listener.onArticleClick(item));
            binding.btnStar.setOnClickListener(v -> listener.onStarClick(item));
        }
    }
}
