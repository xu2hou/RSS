package com.mobile.rssreader.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 文章。一条 RSS item / Atom entry。
 * <p>
 * 唯一索引用 {@code link} 做去重：同一链接重复抓取时不会产生重复行。
 */
@Entity(
        tableName = "articles",
        foreignKeys = @ForeignKey(
                entity = Feed.class,
                parentColumns = "id",
                childColumns = "feedId",
                onDelete = ForeignKey.CASCADE),
        indices = {
                @Index(value = {"link"}, unique = true),
                @Index(value = {"feedId"}),
                @Index(value = {"publishedAt"})
        })
public class Article {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long feedId;

    @NonNull
    public String title = "";

    /** 文章链接，唯一索引 */
    @NonNull
    public String link = "";

    public String author;

    /** 纯文本摘要，列表页展示 */
    public String summary;

    /** 正文 HTML，详情页用 WebView 渲染 */
    public String contentHtml;

    /** 发布时间（毫秒） */
    public long publishedAt;

    public boolean read;

    public boolean starred;

    /** 抓取入库时间，用于「最新收录」排序 */
    public long fetchedAt = System.currentTimeMillis();
}
