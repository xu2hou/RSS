package com.mobile.rssreader.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class BackupPayloadTest {

    private BackupPayload sample() {
        BackupPayload payload = new BackupPayload();
        payload.exportedAt = 1_800_000_000_000L;
        payload.appVersion = "1.0.0";

        BackupPayload.FeedRecord feed = new BackupPayload.FeedRecord();
        feed.id = 7;
        feed.title = "少数派";
        feed.url = "https://sspai.com/feed";
        feed.siteUrl = "https://sspai.com";
        feed.createdAt = 1_799_000_000_000L;
        payload.feeds.add(feed);

        BackupPayload.ArticleRecord a1 = new BackupPayload.ArticleRecord();
        a1.feedId = 7;
        a1.title = "派早报";
        a1.link = "https://sspai.com/post/1";
        a1.summary = "摘要";
        a1.contentHtml = "<p>正文</p>";
        a1.publishedAt = 1_799_500_000_000L;
        a1.read = true;
        a1.starred = true;
        payload.articles.add(a1);

        BackupPayload.ArticleRecord a2 = new BackupPayload.ArticleRecord();
        a2.feedId = 7;
        a2.title = "第二个";
        a2.link = "https://sspai.com/post/2";
        a2.publishedAt = 1_799_600_000_000L;
        payload.articles.add(a2);
        return payload;
    }

    @Test
    public void jsonRoundTripKeepsCountsAndFields() throws Exception {
        BackupPayload original = sample();
        BackupPayload restored = BackupPayload.fromJson(original.toJson());

        assertEquals(1, restored.feeds.size());
        assertEquals(2, restored.articles.size());
        assertEquals(original.exportedAt, restored.exportedAt);
        assertEquals("1.0.0", restored.appVersion);
        assertEquals(BackupPayload.CURRENT_SCHEMA, restored.schemaVersion);

        assertEquals("少数派", restored.feeds.get(0).title);
        assertEquals("https://sspai.com/feed", restored.feeds.get(0).url);

        BackupPayload.ArticleRecord first = restored.articles.get(0);
        assertEquals("https://sspai.com/post/1", first.link);
        assertEquals(7L, first.feedId);
        assertTrue("收藏状态必须保住，否则恢复没有意义", first.starred);
        assertTrue("阅读状态必须保住", first.read);
        assertTrue(first.contentHtml.contains("正文"));

        assertFalse(restored.articles.get(1).read);
        assertFalse(restored.articles.get(1).starred);
    }

    @Test
    public void rejectsNewerSchema() {
        String json = "{\"schemaVersion\":99,\"feeds\":[],\"articles\":[]}";
        try {
            BackupPayload.fromJson(json);
            fail("版本过新的备份应当被拒绝");
        } catch (BackupException e) {
            assertTrue("错误信息应说明版本问题: " + e.getMessage(), e.getMessage().contains("版本过新"));
        }
    }

    @Test
    public void rejectsNonBackupJson() {
        try {
            BackupPayload.fromJson("<html>not json</html>");
            fail("非 JSON 内容应当被拒绝");
        } catch (BackupException e) {
            assertTrue(e.getMessage().contains("JSON"));
        }
    }

    @Test
    public void rejectsMissingSchemaVersion() {
        try {
            BackupPayload.fromJson("{\"feeds\":[],\"articles\":[]}");
            fail("缺少 schemaVersion 应当被拒绝");
        } catch (BackupException e) {
            assertTrue(e.getMessage().contains("schemaVersion"));
        }
    }

    @Test
    public void skipsRecordsWithoutKeyFields() throws Exception {
        String json = "{\"schemaVersion\":1,\"feeds\":[{\"id\":1,\"title\":\"无url\"}],"
                + "\"articles\":[{\"feedId\":1,\"title\":\"无link\"}]}";
        BackupPayload payload = BackupPayload.fromJson(json);
        assertEquals("没有 url 的源应被丢弃", 0, payload.feeds.size());
        assertEquals("没有 link 的文章应被丢弃", 0, payload.articles.size());
    }
}
