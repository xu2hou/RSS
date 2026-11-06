package com.mobile.rssreader.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.mobile.rssreader.R;
import com.mobile.rssreader.databinding.ActivityMainBinding;
import com.mobile.rssreader.ui.articles.ArticlesFragment;
import com.mobile.rssreader.ui.feeds.FeedsFragment;
import com.mobile.rssreader.ui.settings.SettingsFragment;

/**
 * 单 Activity + 三 Tab 的骨架。
 * 导航用 Fragment 切换，详情页另起 Activity（列表 → 详情 的跳转关系）。
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });

        if (savedInstanceState == null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_feeds);
        }
    }

    private void showTab(int itemId) {
        Fragment target;
        String tag;
        if (itemId == R.id.nav_articles) {
            target = new ArticlesFragment();
            tag = "articles";
        } else if (itemId == R.id.nav_settings) {
            target = new SettingsFragment();
            tag = "settings";
        } else {
            target = new FeedsFragment();
            tag = "feeds";
        }
        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, target, tag)
                .commit();
    }

    @NonNull
    public String currentTabTag() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        return current == null ? "" : String.valueOf(current.getTag());
    }
}
