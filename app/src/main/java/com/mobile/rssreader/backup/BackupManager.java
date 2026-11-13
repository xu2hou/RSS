package com.mobile.rssreader.backup;

import android.content.Context;
import android.os.Build;

import com.mobile.rssreader.BuildConfig;
import com.mobile.rssreader.data.AppDatabase;
import com.mobile.rssreader.data.entity.Article;
import com.mobile.rssreader.data.entity.Feed;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 伴生模块核心：备份与恢复。
 * <p>
 * 与主 App 的数据打通方式：直接复用主 App 的 {@link AppDatabase}（同一进程、同一个 Room 实例），
 * 而不是去解析或复制 .db 文件 —— 后者在 WAL 模式下极易拿到不一致的快照。
 */
public final class BackupManager {

    /** 自动备份最多保留的份数 */
    public static final int MAX_BACKUPS = 7;
    private static final String DIR_NAME = "backups";
    private static final String PREFIX = "yueliu-backup-";
    private static final String SUFFIX = ".json";

    private final Context context;
    private final AppDatabase database;

    public BackupManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.get(this.context);
    }

    /** 备份目录（应用私有，随卸载清除；要长期保存可用「导出到文件」） */
    public File backupDir() {
        File dir = new File(context.getFilesDir(), DIR_NAME);
        if (!dir.exists() && !dir.mkdirs()) {
            return context.getFilesDir();
        }
        return dir;
    }

    /** 从数据库抓一份完整快照 */
    public BackupPayload snapshot() {
        BackupPayload payload = new BackupPayload();
        payload.exportedAt = System.currentTimeMillis();
        payload.appVersion = BuildConfig.VERSION_NAME;

        List<Feed> feeds = database.feedDao().getAllSync();
        for (Feed feed : feeds) {
            BackupPayload.FeedRecord record = new BackupPayload.FeedRecord();
            record.id = feed.id;
            record.title = feed.title;
            record.url = feed.url;
            record.siteUrl = feed.siteUrl;
            record.description = feed.description;
            record.createdAt = feed.createdAt;
            payload.feeds.add(record);
        }

        List<Article> articles = database.articleDao().getAllSync();
        for (Article article : articles) {
            BackupPayload.ArticleRecord record = new BackupPayload.ArticleRecord();
            record.feedId = article.feedId;
            record.title = article.title;
            record.link = article.link;
            record.author = article.author;
            record.summary = article.summary;
            record.contentHtml = article.contentHtml;
            record.publishedAt = article.publishedAt;
            record.read = article.read;
            record.starred = article.starred;
            payload.articles.add(record);
        }
        return payload;
    }

    /** 备份到应用私有目录，返回生成的文件 */
    public File backupToLocal() throws BackupException {
        BackupPayload payload = snapshot();
        String name = PREFIX
                + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date())
                + SUFFIX;
        File target = new File(backupDir(), name);
        try {
            String json = payload.toJson();
            try (OutputStream out = new FileOutputStream(target)) {
                out.write(json.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        } catch (JSONException | IOException e) {
            throw new BackupException("写入备份文件失败：" + e.getMessage(), e);
        }
        pruneOldBackups();
        return target;
    }

    /** 导出到调用方给的流（SAF 选中的位置） */
    public int exportTo(OutputStream out) throws BackupException {
        try {
            BackupPayload payload = snapshot();
            byte[] bytes = payload.toJson().getBytes(StandardCharsets.UTF_8);
            out.write(bytes);
            out.flush();
            return payload.articles.size();
        } catch (JSONException | IOException e) {
            throw new BackupException("导出失败：" + e.getMessage(), e);
        }
    }

    /** 读取并解析备份文件内容（用于导入前预览校验） */
    public BackupPayload readPayload(InputStream in) throws BackupException {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            String json = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            return BackupPayload.fromJson(json);
        } catch (IOException e) {
            throw new BackupException("读取文件失败：" + e.getMessage(), e);
        }
    }

    /**
     * 合并式恢复：
     * <ul>
     *   <li>订阅源按 url 去重，已存在则复用其 id，避免产生重复订阅</li>
     *   <li>文章按 link 去重；已存在时按 {@code overrideStates} 决定是否覆盖阅读/收藏状态</li>
     * </ul>
     * 整个过程在一个事务里完成，中途失败不会留下半截数据。
     */
    public RestoreResult restore(InputStream in, boolean overrideStates) throws BackupException {
        BackupPayload payload = readPayload(in);
        final RestoreResult result = new RestoreResult();
        try {
            database.runInTransaction(() -> {
                // 备份里的旧 feedId → 本机实际 feedId
                java.util.Map<Long, Long> idMap = new java.util.HashMap<>();
                for (BackupPayload.FeedRecord record : payload.feeds) {
                    Feed existing = database.feedDao().findByUrl(record.url);
                    if (existing != null) {
                        idMap.put(record.id, existing.id);
                        result.feedsSkipped++;
                        continue;
                    }
                    Feed feed = new Feed();
                    feed.title = record.title == null || record.title.isEmpty()
                            ? record.url : record.title;
                    feed.url = record.url;
                    feed.siteUrl = record.siteUrl;
                    feed.description = record.description;
                    feed.createdAt = record.createdAt > 0 ? record.createdAt : System.currentTimeMillis();
                    long newId = database.feedDao().insert(feed);
                    if (newId > 0) {
                        idMap.put(record.id, newId);
                        result.feedsAdded++;
                    }
                }

                for (BackupPayload.ArticleRecord record : payload.articles) {
                    Long mapped = idMap.get(record.feedId);
                    if (mapped == null) {
                        // 文章的源既不在备份里、本机也没有，跳过而不是丢弃整次恢复
                        result.articlesSkipped++;
                        continue;
                    }
                    Article existing = database.articleDao().findByLink(record.link);
                    if (existing == null) {
                        Article article = new Article();
                        article.feedId = mapped;
                        article.title = record.title == null || record.title.isEmpty()
                                ? "（无标题）" : record.title;
                        article.link = record.link;
                        article.author = record.author;
                        article.summary = record.summary;
                        article.contentHtml = record.contentHtml;
                        article.publishedAt = record.publishedAt > 0
                                ? record.publishedAt : System.currentTimeMillis();
                        article.read = record.read;
                        article.starred = record.starred;
                        database.articleDao().insertAll(java.util.Collections.singletonList(article));
                        result.articlesAdded++;
                    } else if (overrideStates) {
                        database.articleDao().updateStates(existing.id, record.read, record.starred);
                        result.articlesUpdated++;
                    } else {
                        result.articlesSkipped++;
                    }
                }
            });
        } catch (Exception e) {
            throw new BackupException("恢复失败，已回滚：" + e.getMessage(), e);
        }
        return result;
    }

    /** 从本机备份文件恢复 */
    public RestoreResult restoreFromFile(File file, boolean overrideStates) throws BackupException {
        try (InputStream in = new FileInputStream(file)) {
            return restore(in, overrideStates);
        } catch (IOException e) {
            throw new BackupException("打开备份文件失败：" + e.getMessage(), e);
        }
    }

    /** 备份记录（按时间倒序） */
    public List<File> listBackups() {
        File dir = backupDir();
        File[] files = dir.listFiles((d, name) -> name.startsWith(PREFIX) && name.endsWith(SUFFIX));
        List<File> list = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                list.add(f);
            }
        }
        list.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return list;
    }

    /** 只保留最近 {@link #MAX_BACKUPS} 份，避免无限占用空间 */
    public int pruneOldBackups() {
        List<File> files = listBackups();
        int removed = 0;
        for (int i = MAX_BACKUPS; i < files.size(); i++) {
            if (files.get(i).delete()) {
                removed++;
            }
        }
        return removed;
    }

    public int countArticlesIn(File file) throws BackupException {
        try (InputStream in = new FileInputStream(file)) {
            return readPayload(in).articles.size();
        } catch (IOException e) {
            throw new BackupException("读取备份文件失败：" + e.getMessage(), e);
        }
    }

    /** 恢复结果统计 */
    public static final class RestoreResult {
        public int feedsAdded;
        public int feedsSkipped;
        public int articlesAdded;
        public int articlesUpdated;
        public int articlesSkipped;

        public String toMessage() {
            return "恢复完成：新增订阅源 " + feedsAdded
                    + " 个（跳过重复 " + feedsSkipped + "），新增文章 " + articlesAdded
                    + " 篇，更新阅读状态 " + articlesUpdated + " 篇，跳过 " + articlesSkipped + " 篇";
        }
    }

    /** 供「关于」或调试用：记录设备信息，便于排查兼容问题 */
    public static String deviceSummary() {
        return Build.MANUFACTURER + " " + Build.MODEL + " / API " + Build.VERSION.SDK_INT;
    }
}
