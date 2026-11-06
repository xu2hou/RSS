package com.mobile.rssreader.ui.feeds;

import android.content.Intent;
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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mobile.rssreader.R;
import com.mobile.rssreader.data.entity.Feed;
import com.mobile.rssreader.data.entity.FeedWithUnread;
import com.mobile.rssreader.databinding.FragmentFeedsBinding;

import java.util.List;

/**
 * 订阅源列表页（第 1 个界面）。
 * 数据流：Room(FeedDao) → FeedRepository → FeedsViewModel → LiveData → Adapter。
 */
public class FeedsFragment extends Fragment implements FeedAdapter.Listener {

    private FragmentFeedsBinding binding;
    private FeedsViewModel viewModel;
    private FeedAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFeedsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FeedsViewModel.class);

        adapter = new FeedAdapter(this);
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.refreshAll());
        binding.fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddFeedActivity.class);
            startActivity(intent);
        });
        binding.stateView.setOnRetryClickListener(() -> viewModel.refreshAll());

        observe();
    }

    private void observe() {
        viewModel.getFeeds().observe(getViewLifecycleOwner(), this::render);

        viewModel.getRefreshing().observe(getViewLifecycleOwner(), refreshing -> {
            if (binding == null || refreshing == null) {
                return;
            }
            binding.swipeRefresh.setRefreshing(refreshing);
        });

        viewModel.getMessage().observe(getViewLifecycleOwner(), event -> {
            if (event == null) {
                return;
            }
            String text = event.getContentIfNotHandled();
            if (text != null && binding != null) {
                Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void render(List<FeedWithUnread> list) {
        if (binding == null) {
            return;
        }
        if (list == null || list.isEmpty()) {
            adapter.submitList(null);
            binding.stateView.showEmpty(getString(R.string.feeds_empty));
            binding.recycler.setVisibility(View.GONE);
            return;
        }
        binding.stateView.hide();
        binding.recycler.setVisibility(View.VISIBLE);
        adapter.submitList(list);
    }

    @Override
    public void onFeedClick(Feed feed) {
        // 打开该源的文章列表：S05 实现
        Toast.makeText(requireContext(), feed.title + "（文章列表下一步实现）", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFeedLongClick(Feed feed) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.feed_delete_title)
                .setMessage(getString(R.string.feed_delete_message, feed.title))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        viewModel.deleteFeed(feed.id))
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
