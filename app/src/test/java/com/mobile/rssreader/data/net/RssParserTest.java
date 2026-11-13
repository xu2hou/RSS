package com.mobile.rssreader.data.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;

/**
 * 解析器回归测试：用三份离线 XML 样本覆盖 RSS 2.0 / Atom / 非订阅内容。
 * 注入 kxml2 的解析器，避免依赖 Android 运行时。
 */
public class RssParserTest {

    private XmlPullParser newParser() {
        return new KXmlParser();
    }

    private InputStream open(String name) {
        InputStream in = getClass().getClassLoader().getResourceAsStream(name);
        assertNotNull("测试资源缺失: " + name, in);
        return in;
    }

    @Test
    public void rss2_parsesChannelAndItems() throws Exception {
        RssParser.ParsedFeed feed = RssParser.parse(newParser(), open("rss2_sample.xml"));

        assertEquals("少数派", feed.title);
        assertEquals(2, feed.items.size());

        RssParser.ParsedItem first = feed.items.get(0);
        assertEquals("派早报：国务院办公厅发布 2027 年部分节假日放假安排等", first.title);
        assertEquals("https://sspai.com/post/118587", first.link);
        assertTrue(first.summary.contains("OpenAI"));
        // RFC 822 时间应被解析成毫秒时间戳
        assertTrue(first.publishedAt > 0);
    }

    @Test
    public void rss2_readsDateCorrectly() throws Exception {
        RssParser.ParsedFeed feed = RssParser.parse(newParser(), open("rss2_sample.xml"));
        long expected = 1793924679000L; // 2026-11-06 08:24:39 +0800
        long actual = feed.items.get(0).publishedAt;
        // 允许时区换算造成的秒级误差
        assertTrue("时间解析偏差过大: " + actual, Math.abs(actual - expected) < 1000L);
    }

    @Test
    public void atom_parsesEntryLinkFromHrefAndCdataContent() throws Exception {
        RssParser.ParsedFeed feed = RssParser.parse(newParser(), open("atom_sample.xml"));

        assertEquals("阮一峰的网络日志", feed.title);
        assertEquals(1, feed.items.size());

        RssParser.ParsedItem entry = feed.items.get(0);
        // Atom 的链接在 href 属性里，不是标签文本
        assertEquals("http://www.ruanyifeng.com/blog/2026/11/weekly-issue-418.html", entry.link);
        assertNotNull(entry.contentHtml);
        assertTrue(entry.contentHtml.contains("AI 与就业"));
        assertTrue(entry.publishedAt > 0);
    }

    @Test
    public void nonFeed_returnsNoItems() throws Exception {
        // 解析器本身比较"钝"：只要没遇到 item/entry 就当没有条目，
        // 至于「这是不是 RSS」由 RssFetcher 判断（items 与 title 都为空时抛业务异常）。
        // 所以这里只断言条目为空，不断言 title —— html 的 <title> 会被当作频道标题收下。
        RssParser.ParsedFeed feed = RssParser.parse(newParser(), open("not_a_feed.xml"));
        assertTrue(feed.items.isEmpty());
    }
}
