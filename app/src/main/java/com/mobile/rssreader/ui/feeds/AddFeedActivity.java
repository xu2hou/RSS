package com.mobile.rssreader.ui.feeds;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.mobile.rssreader.R;
import com.mobile.rssreader.data.SampleFeeds;
import com.mobile.rssreader.databinding.ActivityAddFeedBinding;

/**
 * 添加订阅页（第 4 个界面）。
 * 输入地址 → 抓取解析 → 入库；失败时把后端的具体原因回显到输入框下方，
 * 而不是只弹 Toast —— 错误信息要能被用户读到并据此修改地址。
 */
public class AddFeedActivity extends AppCompatActivity {

    private ActivityAddFeedBinding binding;
    private AddFeedViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddFeedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        viewModel = new ViewModelProvider(this).get(AddFeedViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        applyWindowInsets();
        binding.btnAdd.setOnClickListener(v -> submit());
        buildSuggestions();
        observe();
    }

    /** 同 MainActivity：targetSdk 35 强制 edge-to-edge，需要手动避让系统栏 */
    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private void submit() {
        hideError();
        CharSequence raw = binding.etUrl.getText();
        String url = raw == null ? "" : raw.toString().trim();
        if (url.isEmpty()) {
            showError(getString(R.string.add_feed_url_empty));
            return;
        }
        viewModel.addFeed(url);
    }

    private void observe() {
        viewModel.getLoading().observe(this, loading -> {
            boolean busy = loading != null && loading;
            binding.rowLoading.setVisibility(busy ? View.VISIBLE : View.GONE);
            binding.btnAdd.setEnabled(!busy);
            binding.etUrl.setEnabled(!busy);
        });

        viewModel.getOutcome().observe(this, event -> {
            if (event == null) {
                return;
            }
            AddFeedViewModel.Outcome outcome = event.getContentIfNotHandled();
            if (outcome == null) {
                return;
            }
            if (outcome.success) {
                Toast.makeText(this, getString(R.string.add_feed_success, outcome.message),
                        Toast.LENGTH_SHORT).show();
                finish();
            } else {
                showError(outcome.message);
            }
        });
    }

    private void buildSuggestions() {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (String[] item : SampleFeeds.ITEMS) {
            final String url = item[1];
            MaterialButton button = (MaterialButton) inflater.inflate(
                    R.layout.item_suggestion_button, binding.containerSuggestions, false);
            button.setText(item[0]);
            button.setOnClickListener(v -> {
                binding.etUrl.setText(url);
                binding.etUrl.setSelection(url.length());
                hideError();
            });
            binding.containerSuggestions.addView(button);
        }
    }

    private void showError(String message) {
        binding.tvError.setText(message);
        binding.tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        binding.tvError.setVisibility(View.GONE);
    }
}
