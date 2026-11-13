package com.mobile.rssreader.ui.detail;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.mobile.rssreader.R;
import com.mobile.rssreader.data.entity.Article;
import com.mobile.rssreader.ui.widget.WatermarkOverlay;
import com.mobile.rssreader.databinding.ActivityArticleDetailBinding;
import com.mobile.rssreader.util.HtmlUtils;

/**
 * 文章详情页（第 3 个界面）。
 * <p>
 * 用 WebView 渲染正文：RSS 正文本身就是 HTML，交给 WebView 比自己解析标签更稳，
 * 也能正确处理图片、列表、代码块。样式用注入的 CSS 控制，含深色模式适配。
 */
public class ArticleDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ARTICLE_ID = "extra_article_id";

    private ActivityArticleDetailBinding binding;
    private ArticleDetailViewModel viewModel;
    private Article current;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArticleDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WatermarkOverlay.attach(this);
        applyWindowInsets();

        viewModel = new ViewModelProvider(this).get(ArticleDetailViewModel.class);

        long id = getIntent().getLongExtra(EXTRA_ARTICLE_ID, -1L);
        if (id <= 0) {
            finish();
            return;
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.inflateMenu(R.menu.menu_detail);
        binding.toolbar.setOnMenuItemClickListener(this::onMenuClick);

        WebSettings settings = binding.webView.getSettings();
        // 正文是本地注入的 HTML，不需要 JS；关掉更安全，也少一类被源注入的风险
        settings.setJavaScriptEnabled(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setDomStorageEnabled(false);
        binding.webView.setBackgroundColor(getColor(R.color.surface_light));
        // 正文里的链接交给系统浏览器，避免在阅读视图里跑偏
        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                openInBrowser(request.getUrl());
                return true;
            }
        });

        viewModel.getArticle().observe(this, this::render);
        viewModel.load(id);
    }

    private void applyWindowInsets() {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private void render(Article article) {
        if (article == null) {
            binding.progress.setVisibility(android.view.View.GONE);
            binding.webView.setVisibility(android.view.View.GONE);
            binding.stateView.showError("文章不存在或已被删除");
            return;
        }
        boolean firstTime = current == null;
        current = article;
        updateStarIcon();
        binding.toolbar.setTitle("");

        if (firstTime) {
            viewModel.markRead(article); // 进入即已读
            loadHtml(article);
        }
        binding.progress.setVisibility(android.view.View.GONE);
    }

    private void loadHtml(Article article) {
        String body = article.contentHtml;
        if (TextUtils.isEmpty(body)) {
            body = article.summary;
        }
        if (TextUtils.isEmpty(body)) {
            binding.webView.setVisibility(android.view.View.GONE);
            binding.stateView.showEmpty(getString(R.string.detail_no_content));
            return;
        }
        binding.webView.setVisibility(android.view.View.VISIBLE);
        binding.webView.loadDataWithBaseURL(null, wrapHtml(article, body), "text/html", "utf-8", null);
    }

    /**
     * 把正文包进一个带响应式 CSS 的骨架：
     * 图片自适应宽度、代码块横向滚动、深色模式跟随系统。
     */
    private String wrapHtml(Article article, String body) {
        String title = HtmlUtils.unescape(article.title);
        String meta = HtmlUtils.unescape(article.author == null ? "" : article.author);
        String css = "<style>"
                + ":root{color-scheme:light dark}"
                + "body{margin:0;padding:18px 16px 40px;font:17px/1.75 -apple-system,'Noto Sans SC','PingFang SC',sans-serif;"
                + "color:#1A1C1E;background:#FBFBFD;-webkit-text-size-adjust:100%;word-wrap:break-word;overflow-wrap:break-word}"
                + "h1{font-size:22px;line-height:1.4;margin:0 0 8px}"
                + ".meta{color:#5A6068;font-size:13px;margin:0 0 20px}"
                + "img,video,iframe{max-width:100%;height:auto;border-radius:8px}"
                + "a{color:#3C6E9F}"
                + "pre{overflow-x:auto;background:rgba(0,0,0,.05);padding:12px;border-radius:8px;font-size:13px;line-height:1.5}"
                + "code{font-family:ui-monospace,Menlo,Consolas,monospace}"
                + "blockquote{margin:12px 0;padding:8px 14px;border-left:3px solid #C9CED4;color:#5A6068}"
                + "table{width:100%;border-collapse:collapse;font-size:14px}"
                + "td,th{border:1px solid #E3E5E8;padding:6px}"
                + "@media (prefers-color-scheme:dark){"
                + "body{color:#E3E5E8;background:#121417}"
                + ".meta{color:#9AA0A6}a{color:#8AB4D8}"
                + "pre{background:rgba(255,255,255,.08)}"
                + "blockquote{border-left-color:#3A3F45;color:#9AA0A6}"
                + "td,th{border-color:#2A2F35}}"
                + "</style>";
        return "<!DOCTYPE html><html><head>"
                + "<meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1\">"
                + css
                + "</head><body><h1>" + title + "</h1>"
                + (meta.isEmpty() ? "" : "<p class=\"meta\">" + meta + "</p>")
                + body
                + "</body></html>";
    }

    private void updateStarIcon() {
        if (current == null) {
            return;
        }
        Menu menu = binding.toolbar.getMenu();
        MenuItem item = menu.findItem(R.id.action_star);
        if (item != null) {
            item.setIcon(current.starred ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
            item.setTitle(current.starred ? R.string.action_starred : R.string.action_star);
        }
    }

    private boolean onMenuClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_star) {
            viewModel.toggleStar(current);
            return true;
        }
        if (id == R.id.action_share) {
            share();
            return true;
        }
        if (id == R.id.action_open_browser) {
            if (current != null) {
                openInBrowser(Uri.parse(current.link));
            }
            return true;
        }
        return false;
    }

    private void share() {
        if (current == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, current.title);
        intent.putExtra(Intent.EXTRA_TEXT, current.title + "\n" + current.link);
        startActivity(Intent.createChooser(intent, getString(R.string.detail_share_chooser)));
    }

    private void openInBrowser(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.detail_open_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
