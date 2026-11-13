package com.mobile.rssreader.data.net;

import android.util.Xml;

import com.mobile.rssreader.util.DateUtils;
import com.mobile.rssreader.util.HtmlUtils;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * RSS 2.0 / Atom 双格式解析器，基于 Android 内置的 {@link XmlPullParser}
 * （流式解析，不占内存、不引第三方 XML 库）。
 * <p>
 * 关键设计：
 * <ul>
 *   <li>关闭命名空间处理，改用「标签名 + 前缀」直接匹配，兼容 {@code content:encoded}、{@code dc:date} 这类前缀</li>
 *   <li>Atom 的 {@code <link href=...>} 与 RSS 的 {@code <link>文本</link>} 用「有没有 href 属性」区分</li>
 *   <li>正文优先 content / content:encoded，退化到 description / summary</li>
 * </ul>
 */
public final class RssParser {

    /** 单条 feed 的解析结果 */
    public static final class ParsedFeed {
        public String title = "";
        public String siteUrl = "";
        public String description = "";
        public final List<ParsedItem> items = new ArrayList<>();
    }

    /** 单篇文章的解析结果 */
    public static final class ParsedItem {
        public String title = "";
        public String link = "";
        public String author;
        public String summary;
        public String contentHtml;
        public long publishedAt;
    }

    private RssParser() {
    }

    public static ParsedFeed parse(InputStream inputStream)
            throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        return parse(parser, inputStream);
    }

    /**
     * 解析核心：由调用方提供 {@link XmlPullParser} 实现。
     * 这样 JVM 单元测试可以注入 kxml2 的解析器（Android 的 {@code android.util.Xml}
     * 在普通 JVM 单测里不可用），生产代码仍走系统实现。
     */
    static ParsedFeed parse(XmlPullParser parser, InputStream inputStream)
            throws org.xmlpull.v1.XmlPullParserException, java.io.IOException {
        // 第二参数传 null：由解析器按 BOM / <?xml encoding?> 自行判定编码（UTF-8 / GBK 都能吃）
        parser.setInput(inputStream, null);

        ParsedFeed feed = new ParsedFeed();
        ParsedItem currentItem = null;
        String currentTag = null;
        StringBuilder buffer = new StringBuilder();

        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            switch (event) {
                case XmlPullParser.START_TAG: {
                    String name = parser.getName();
                    currentTag = name;
                    buffer.setLength(0);

                    if ("item".equals(name) || "entry".equals(name)) {
                        currentItem = new ParsedItem();
                    } else if ("link".equals(name)) {
                        // Atom 形态：<link rel="alternate" href="http://..."/>
                        String href = parser.getAttributeValue(null, "href");
                        if (href != null) {
                            String rel = parser.getAttributeValue(null, "rel");
                            boolean isAlternate = rel == null || "alternate".equals(rel);
                            if (isAlternate && currentItem != null && currentItem.link.isEmpty()) {
                                currentItem.link = href.trim();
                            } else if (isAlternate && currentItem == null && feed.siteUrl.isEmpty()) {
                                feed.siteUrl = href.trim();
                            }
                            currentTag = null; // 自闭合，不需要读文本
                        }
                    }
                    break;
                }
                case XmlPullParser.TEXT:
                case XmlPullParser.CDSECT:
                case XmlPullParser.ENTITY_REF: {
                    if (currentTag != null) {
                        buffer.append(parser.getText());
                    }
                    break;
                }
                case XmlPullParser.END_TAG: {
                    String name = parser.getName();
                    if (currentTag != null && currentTag.equals(name)) {
                        String value = buffer.toString().trim();
                        if (currentItem == null) {
                            handleFeedTag(feed, name, value);
                        } else {
                            handleItemTag(currentItem, name, value);
                        }
                        currentTag = null;
                        buffer.setLength(0);
                    }
                    if ("item".equals(name) || "entry".equals(name)) {
                        finishItem(feed, currentItem);
                        currentItem = null;
                    }
                    break;
                }
                default:
                    break;
            }
            event = parser.next();
        }
        return feed;
    }

    private static void handleFeedTag(ParsedFeed feed, String tag, String value) {
        switch (tag) {
            case "title":
                if (feed.title.isEmpty()) {
                    feed.title = HtmlUtils.toPlainText(value);
                }
                break;
            case "link":
                if (feed.siteUrl.isEmpty()) {
                    feed.siteUrl = value;
                }
                break;
            case "description":
            case "subtitle":
                if (feed.description.isEmpty()) {
                    feed.description = HtmlUtils.ellipsize(HtmlUtils.toPlainText(value), 120);
                }
                break;
            default:
                break;
        }
    }

    private static void handleItemTag(ParsedItem item, String tag, String value) {
        switch (tag) {
            case "title":
                if (item.title.isEmpty()) {
                    item.title = HtmlUtils.toPlainText(value);
                }
                break;
            case "link":
                // 仅 RSS 形态会走到这里（Atom 在 START_TAG 已处理 href）
                if (item.link.isEmpty()) {
                    item.link = value;
                }
                break;
            case "guid":
            case "id":
                if (item.link.isEmpty() && value.startsWith("http")) {
                    item.link = value;
                }
                break;
            case "description":
            case "summary":
                if (item.summary == null) {
                    item.summary = HtmlUtils.toPlainText(value);
                }
                if (item.contentHtml == null) {
                    item.contentHtml = value;
                }
                break;
            case "content":
            case "content:encoded":
                item.contentHtml = value;
                break;
            case "author":
            case "dc:creator":
            case "creator":
                if (item.author == null && !value.isEmpty()) {
                    item.author = HtmlUtils.toPlainText(value);
                }
                break;
            case "pubDate":
            case "published":
            case "updated":
            case "dc:date":
            case "date":
                if (item.publishedAt == 0L) {
                    item.publishedAt = DateUtils.parse(value);
                }
                break;
            default:
                break;
        }
    }

    private static void finishItem(ParsedFeed feed, ParsedItem item) {
        if (item == null) {
            return;
        }
        if (item.title.isEmpty() && item.link.isEmpty()) {
            return; // 空条目直接丢弃
        }
        if (item.link.isEmpty()) {
            // 没有链接的条目无法去重也无法打开，跳过
            return;
        }
        if (item.summary == null && item.contentHtml != null) {
            item.summary = HtmlUtils.toPlainText(item.contentHtml);
        }
        if (item.summary == null) {
            item.summary = "";
        }
        if (item.contentHtml == null) {
            item.contentHtml = item.summary;
        }
        if (item.publishedAt == 0L) {
            item.publishedAt = System.currentTimeMillis();
        }
        feed.items.add(item);
    }
}
