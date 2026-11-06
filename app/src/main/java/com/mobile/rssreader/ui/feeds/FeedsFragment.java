package com.mobile.rssreader.ui.feeds;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mobile.rssreader.databinding.FragmentFeedsBinding;

/**
 * 订阅源列表页（第 1 个界面）。
 * 目前只搭骨架，列表与数据绑定在 feature/feeds 分支继续实现。
 */
public class FeedsFragment extends Fragment {

    private FragmentFeedsBinding binding;

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
        binding.stateView.showEmpty("还没有订阅源，点击右下角 + 添加一个");
        binding.stateView.setOnRetryClickListener(() -> {
            // 重试逻辑在数据层接入后补充
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
