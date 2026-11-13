package com.mobile.rssreader.backup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 备份载荷：与数据库解耦的可序列化快照。
 * <p>
 * 用 Android 内置的 org.json 而不是引第三方序列化库：
 * 备份格式要能被别的工具读懂、也要能长期兼容，纯 JSON 最稳。
 * <p>
 * {@link #schemaVersion} 是兼容性开关：将来表结构变了，导入旧文件时据此做迁移，
 * 而不是直接崩在字段缺失上。
 */
public class BackupPayload {

    public static final int CURRENT_SCHEMA = 1;

    public int schemaVersion = CURRENT_SCHEMA;
    public long exportedAt;
    public String appVersion = "";
    public final List<FeedRecord> feeds = new ArrayList<>();
    public final List<ArticleRecord> articles = new ArrayList<>();

    /** 订阅源记录 */
    public static class FeedRecord {
        public long id;
        public String title = "";
        public String url = "";
        public String siteUrl;
        public String description;
        public long createdAt;
    }

    /** 文章记录（含阅读状态，这是「恢复」真正有价值的部分） */
    public static class ArticleRecord {
        public long feedId;
        public String title = "";
        public String link = "";
        public String author;
        public String summary;
        public String contentHtml;
        public long publishedAt;
        public boolean read;
        public boolean starred;
    }

    public String toJson() throws JSONException {
        JSONObject root = new JSONObject();
        root.put("schemaVersion", schemaVersion);
        root.put("exportedAt", exportedAt);
        root.put("appVersion", appVersion);

        JSONArray feedArray = new JSONArray();
        for (FeedRecord f : feeds) {
            JSONObject o = new JSONObject();
            o.put("id", f.id);
            o.put("title", f.title);
            o.put("url", f.url);
            o.put("siteUrl", f.siteUrl == null ? "" : f.siteUrl);
            o.put("description", f.description == null ? "" : f.description);
            o.put("createdAt", f.createdAt);
            feedArray.put(o);
        }
        root.put("feeds", feedArray);

        JSONArray articleArray = new JSONArray();
        for (ArticleRecord a : articles) {
            JSONObject o = new JSONObject();
            o.put("feedId", a.feedId);
            o.put("title", a.title);
            o.put("link", a.link);
            o.put("author", a.author == null ? "" : a.author);
            o.put("summary", a.summary == null ? "" : a.summary);
            o.put("contentHtml", a.contentHtml == null ? "" : a.contentHtml);
            o.put("publishedAt", a.publishedAt);
            o.put("read", a.read);
            o.put("starred", a.starred);
            articleArray.put(o);
        }
        root.put("articles", articleArray);
        return root.toString();
    }

    public static BackupPayload fromJson(String json) throws BackupException {
        if (json == null || json.trim().isEmpty()) {
            throw new BackupException("备份文件是空的");
        }
        JSONObject root;
        try {
            root = new JSONObject(json);
        } catch (JSONException e) {
            throw new BackupException("文件不是合法的备份 JSON：" + e.getMessage());
        }
        int version = root.optInt("schemaVersion", -1);
        if (version < 0) {
            throw new BackupException("缺少 schemaVersion 字段，可能不是本应用的备份文件");
        }
        if (version > CURRENT_SCHEMA) {
            throw new BackupException("备份文件版本过新（v" + version + "），请升级应用后再导入");
        }

        BackupPayload payload = new BackupPayload();
        payload.schemaVersion = version;
        payload.exportedAt = root.optLong("exportedAt", 0);
        payload.appVersion = root.optString("appVersion", "");

        JSONArray feedArray = root.optJSONArray("feeds");
        if (feedArray != null) {
            for (int i = 0; i < feedArray.length(); i++) {
                JSONObject o = feedArray.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                FeedRecord f = new FeedRecord();
                f.id = o.optLong("id", 0);
                f.title = o.optString("title", "");
                f.url = o.optString("url", "");
                f.siteUrl = o.optString("siteUrl", "");
                f.description = o.optString("description", "");
                f.createdAt = o.optLong("createdAt", 0);
                if (!f.url.isEmpty()) {
                    payload.feeds.add(f);
                }
            }
        }

        JSONArray articleArray = root.optJSONArray("articles");
        if (articleArray != null) {
            for (int i = 0; i < articleArray.length(); i++) {
                JSONObject o = articleArray.optJSONObject(i);
                if (o == null) {
                    continue;
                }
                ArticleRecord a = new ArticleRecord();
                a.feedId = o.optLong("feedId", 0);
                a.title = o.optString("title", "");
                a.link = o.optString("link", "");
                a.author = o.optString("author", "");
                a.summary = o.optString("summary", "");
                a.contentHtml = o.optString("contentHtml", "");
                a.publishedAt = o.optLong("publishedAt", 0);
                a.read = o.optBoolean("read", false);
                a.starred = o.optBoolean("starred", false);
                if (!a.link.isEmpty()) {
                    payload.articles.add(a);
                }
            }
        }
        return payload;
    }
}
