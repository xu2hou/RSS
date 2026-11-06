package com.mobile.rssreader.ui.articles;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mobile.rssreader.databinding.FragmentArticlesBinding;

/**
 * 文章列表页（第 2 个界面），承接「推荐 / 未读 / 收藏」三种筛选。
 */
public class ArticlesFragment extends Fragment {

    private FragmentArticlesBinding binding;

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
        binding.stateView.showEmpty("暂无文章，先在「订阅」里添加一个源");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
