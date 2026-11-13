package com.mobile.rssreader.ui.articles;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.mobile.rssreader.R;
import com.mobile.rssreader.data.entity.Article;
import com.mobile.rssreader.data.entity.FeedWithUnread;
import com.mobile.rssreader.databinding.FragmentArticlesBinding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章列表页（第 2 个界面）：推荐 / 未读 / 收藏 三档筛选。
 * 推荐档走自定义评分排序，另外两档直接走 Room 查询。
 */
public class ArticlesFragment extends Fragment implements ArticleAdapter.Listener {

    private FragmentArticlesBinding binding;
    private ArticlesViewModel viewModel;
    private ArticleAdapter adapter;
    private final Map<Long, String> feedTitles = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentArticlesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ArticlesViewModel.class);

        adapter = new ArticleAdapter(this);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);

        setupTabs();
        observe();
    }

    private void setupTabs() {
        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.tab_recommended));
        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.tab_unread));
        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.tab_starred));
        binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.setFilter(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                binding.recycler.scrollToPosition(0);
            }
        });
    }

    private void observe() {
        viewModel.getFeeds().observe(getViewLifecycleOwner(), feeds -> {
            feedTitles.clear();
            if (feeds != null) {
                for (FeedWithUnread item : feeds) {
                    feedTitles.put(item.feed.id, item.feed.title);
                }
            }
            adapter.setFeedTitles(feedTitles);
        });

        viewModel.getArticles().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(List<Article> list) {
        if (binding == null) {
            return;
        }
        if (list == null || list.isEmpty()) {
            adapter.submitList(null);
            binding.recycler.setVisibility(View.GONE);
            binding.stateView.showEmpty(emptyTextForCurrentTab());
            return;
        }
        binding.stateView.hide();
        binding.recycler.setVisibility(View.VISIBLE);
        adapter.submitList(list);
    }

    private String emptyTextForCurrentTab() {
        Integer filter = viewModel.getFilter().getValue();
        if (filter != null && filter == ArticlesViewModel.FILTER_UNREAD) {
            return getString(R.string.articles_empty_unread);
        }
        if (filter != null && filter == ArticlesViewModel.FILTER_STARRED) {
            return getString(R.string.articles_empty_starred);
        }
        return getString(R.string.articles_empty_all);
    }

    @Override
    public void onArticleClick(Article article) {
        // 详情页在 S06 实现；先标记已读，保证阅读状态真实生效
        viewModel.markRead(article);
        Toast.makeText(requireContext(), article.title, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStarClick(Article article) {
        viewModel.toggleStar(article);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
