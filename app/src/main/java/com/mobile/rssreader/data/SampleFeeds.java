package com.mobile.rssreader.data;

/**
 * 推荐订阅源：均为本机实测可访问、且覆盖 RSS 2.0 与 Atom 两种格式，
 * 用于降低首次使用门槛，也方便演示时一键添加。
 */
public final class SampleFeeds {

    public static final String[][] ITEMS = {
            {"少数派", "https://sspai.com/feed"},
            {"Solidot", "https://www.solidot.org/index.rss"},
            {"IT之家", "https://www.ithome.com/rss/"},
            {"阮一峰的网络日志", "https://www.ruanyifeng.com/blog/atom.xml"},
    };

    private SampleFeeds() {
    }
}
