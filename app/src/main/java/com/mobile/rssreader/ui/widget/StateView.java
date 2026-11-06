package com.mobile.rssreader.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mobile.rssreader.R;

/**
 * 三态容器：加载 / 空 / 错误。
 * 抽成独立控件后，任何列表页都只需两行代码就能补齐三态 UI，
 * 也避免每个 Fragment 各写一套显隐逻辑。
 */
public class StateView extends FrameLayout {

    private View loadingView;
    private View emptyView;
    private View errorView;
    private TextView loadingText;
    private TextView emptyText;
    private TextView errorText;
    private View retryButton;
    private Runnable onRetry;

    public StateView(@NonNull Context context) {
        this(context, null);
    }

    public StateView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_state, this, true);
        loadingView = findViewById(R.id.state_loading);
        emptyView = findViewById(R.id.state_empty);
        errorView = findViewById(R.id.state_error);
        loadingText = findViewById(R.id.state_loading_text);
        emptyText = findViewById(R.id.state_empty_text);
        errorText = findViewById(R.id.state_error_text);
        retryButton = findViewById(R.id.state_retry);
        retryButton.setOnClickListener(v -> {
            if (onRetry != null) {
                onRetry.run();
            }
        });
        hide();
    }

    public StateView setOnRetryClickListener(Runnable action) {
        this.onRetry = action;
        return this;
    }

    public void showLoading() {
        showLoading(getContext().getString(R.string.state_loading));
    }

    public void showLoading(String message) {
        setVisible(loadingView);
        loadingText.setText(message);
    }

    public void showEmpty(String message) {
        setVisible(emptyView);
        emptyText.setText(message);
    }

    public void showError(String message) {
        setVisible(errorView);
        errorText.setText(message == null || message.isEmpty()
                ? getContext().getString(R.string.state_error)
                : message);
    }

    public void hide() {
        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        setVisibility(View.GONE);
    }

    private void setVisible(View target) {
        setVisibility(View.VISIBLE);
        loadingView.setVisibility(target == loadingView ? View.VISIBLE : View.GONE);
        emptyView.setVisibility(target == emptyView ? View.VISIBLE : View.GONE);
        errorView.setVisibility(target == errorView ? View.VISIBLE : View.GONE);
    }
}
